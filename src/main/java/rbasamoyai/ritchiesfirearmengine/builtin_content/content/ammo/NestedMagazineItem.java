package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class NestedMagazineItem extends MagazineItem {

    public NestedMagazineItem(Properties properties, boolean glint, int primaryCapacity, int secondaryCapacity,
                              ImmutableList<AmmoPredicate> defaultMagazinePredicates, int reloadCooldown) {
        super(properties.component(RFEDataComponents.NESTED_MAGAZINES, RFEItemContainerContents.EMPTY),
                glint, primaryCapacity, secondaryCapacity, defaultMagazinePredicates, ImmutableList.of(), ImmutableList.of(),
                reloadCooldown, false, false);
    }

    // TODO multiple nested magazines?
    public ItemStack getNestedMagazine(ItemStack itemStack) {
        return itemStack.getOrDefault(RFEDataComponents.NESTED_MAGAZINES, RFEItemContainerContents.EMPTY).copyOne();
    }

    public void setNestedMagazine(ItemStack itemStack, ItemStack nested) {
        itemStack.set(RFEDataComponents.NESTED_MAGAZINES, RFEItemContainerContents.fromItems(List.of(nested)));
    }

    @Override
    public int getMagazineCapacity(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.getMagazineCapacity(nested) : 0;
    }

    @Override
    public int getSecondaryMagazineCapacity(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.getSecondaryMagazineCapacity(nested) : 0;
    }

    @Override
    public List<ItemStack> getStoredAmmo(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.getStoredAmmo(nested) : new ArrayList<>();
    }

    @Override
    public void writeStoredAmmo(ItemStack itemStack, List<ItemStack> ammo) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        if (!(nested.getItem() instanceof MagazineItem magazineItem))
            return;
        magazineItem.writeStoredAmmo(nested, ammo);
        this.setNestedMagazine(itemStack, nested);
    }

    @Override
    public int countAmmo(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.countAmmo(nested) : 0;
    }

    @Override
    public List<ItemStack> getStoredSecondaryAmmo(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.getStoredSecondaryAmmo(nested) : new ArrayList<>();
    }

    @Override
    public void writeStoredSecondaryAmmo(ItemStack itemStack, List<ItemStack> primers) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        if (!(nested.getItem() instanceof MagazineItem magazineItem))
            return;
        magazineItem.writeStoredSecondaryAmmo(nested, primers);
        this.setNestedMagazine(itemStack, nested);
    }

    @Override
    public int countSecondaryAmmo(ItemStack itemStack) {
        ItemStack nested = this.getNestedMagazine(itemStack);
        return nested.getItem() instanceof MagazineItem magazineItem ? magazineItem.countSecondaryAmmo(nested) : 0;
    }

    @Override
    public void tryReloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        int primaryCount = this.countAmmo(itemStack);
        int secondaryCount = RFEItemUtils.countItemsConditional(this.getStoredSecondaryAmmo(itemStack), Predicate.not(FirearmDataUtils::isUsedPrimer));
        if (primaryCount >= this.primaryCapacity && secondaryCount >= this.secondaryCapacity)
            return;
        boolean split = itemStack.getCount() > 1;
        ItemStack nested = this.getNestedMagazine(itemStack);

        final int effectiveOldCount;
        if (nested.getItem() instanceof MagazineItem currentItem) {
            if (currentItem.primaryCapacity < 1) {
                effectiveOldCount = currentItem.countSecondaryAmmo(nested);
            } else if (currentItem.secondaryCapacity < 1) {
                effectiveOldCount = currentItem.countAmmo(nested);
            } else {
                effectiveOldCount = Math.min(currentItem.countAmmo(nested), currentItem.countSecondaryAmmo(nested));
            }
        } else {
            effectiveOldCount = 0;
        }

        RFEItemUtils.consumeItemsFromEntity(entity, s -> {
            return s != itemStack && this.canReloadNestedMagazine(itemStack, effectiveOldCount, s);
        }, s -> {
            ItemStack writeTo = split ? itemStack.split(1) : itemStack;
            this.setNestedMagazine(writeTo, s.split(1));
            this.applyCooldown(writeTo, entity);
            if (split) {
                if (s.isEmpty())
                    return writeTo;
                RFEItemUtils.addItemToEntity(writeTo, entity);
            }
            return s.isEmpty() ? ItemStack.EMPTY : s;
        }, () -> {
            RFEItemUtils.addItemToEntity(nested, entity);
            return true;
        });
    }

    protected boolean canReloadNestedMagazine(ItemStack itemStack, int effectiveOldCount, ItemStack candidateMagazine) {
        if (candidateMagazine.isEmpty() || !(candidateMagazine.getItem() instanceof MagazineItem candidateItem)
            || !this.matchesAmmoItem(itemStack, candidateMagazine))
            return false;
        int newPrimaryCount = candidateItem.countAmmo(candidateMagazine);
        int newSecondaryCount = candidateItem.countSecondaryAmmo(candidateMagazine);
        if (newPrimaryCount > this.primaryCapacity || newSecondaryCount > this.secondaryCapacity)
            return false;
        // Reload if the effective number of complete shots in the new magazine is greater.
        int effectiveNewCount;
        if (this.primaryCapacity < 1) {
            effectiveNewCount = newSecondaryCount;
        } else if (this.secondaryCapacity < 1) {
            effectiveNewCount = newPrimaryCount;
        } else {
            effectiveNewCount = Math.min(newPrimaryCount, newSecondaryCount);
        }
        return effectiveNewCount > effectiveOldCount;
    }

    @Override
    protected boolean tryReloadForItem(ItemStack magazineStack, List<ItemStack> primaryAmmo, List<ItemStack> secondaryAmmo,
                                       ItemStack availableStack, int maxConsumable) {
        return false;
    }

    @Override
    public boolean tryReloadingInMenu(ItemStack magazineStack, List<ItemStack> ammoStacks, boolean simulate) {
        if (!this.getNestedMagazine(magazineStack).isEmpty())
            return false;
        for (ItemStack ammoStack : ammoStacks) {
            if (this.canReloadNestedMagazine(magazineStack, 0, ammoStack)) {
                this.setNestedMagazine(magazineStack, simulate ? ammoStack.copyWithCount(1) : ammoStack.split(1));
                return true;
            }
        }
        return false;
    }

    @Override
    public void tryUnloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        ItemStack nested = this.getNestedMagazine(itemStack);
        if (nested.isEmpty())
            return;
        RFEItemUtils.addItemToEntity(nested, entity);
        this.setNestedMagazine(itemStack, ItemStack.EMPTY);
        this.applyCooldown(itemStack, entity);
    }

    @Override
    public boolean testInnerData(ItemStack itemStack, Predicate<ItemStack> magazinePredicate, int recursionDepth) {
        if (recursionDepth < 0)
            return false;
        ItemStack nested = this.getNestedMagazine(itemStack);
        return magazinePredicate.test(nested) && nested.getItem() instanceof MagazineItem magazineItem
                && magazineItem.testInnerData(nested, magazinePredicate, recursionDepth - 1);
    }

    @Override
    public Pair<Optional<Integer>, Optional<Integer>> getInventoryAmmoCountForHUD(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
        Predicate<ItemStack> nestedMagazinePredicate = RFEUtils.orAllPredicates(this.getAmmoPredicates(itemStack));
        int primaryCount = 0;
        int secondaryCount = 0;
        for (ItemStack invStack : inventory) {
            if (itemStack == invStack)
                continue;
            if (nestedMagazinePredicate.test(invStack) && invStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> magAmmo = magazineItem.getStoredAmmo(invStack);
                List<ItemStack> magSecondaryAmmo = magazineItem.getStoredSecondaryAmmo(invStack);
                if (magAmmo.isEmpty() && magSecondaryAmmo.isEmpty()
                        || magazineItem.getMagazineCapacity(invStack) > this.primaryCapacity
                        || magazineItem.getSecondaryMagazineCapacity(invStack) > this.secondaryCapacity)
                    continue;
                int magCount = RFEItemUtils.countItems(magAmmo);
                int magSecondaryCount = RFEItemUtils.countItems(magSecondaryAmmo);
                if (invStack.is(RFETags.RFEItemTags.INFINITE_AMMO.tag)) {
                    if (magCount > 0)
                        primaryCount = -1;
                    if (magSecondaryCount > 0)
                        secondaryCount = -1;
                } else {
                    if (primaryCount > -1)
                        primaryCount += magCount;
                    if (secondaryCount > -1)
                        secondaryCount += magSecondaryCount;
                }
            }
            if (primaryCount == -1 && secondaryCount == -1)
                break;
        }
        return Pair.of(Optional.of(primaryCount), Optional.of(secondaryCount));
    }

    @Override
    protected void addAmmoLinesToTooltip(ItemStack itemStack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack nestedMagazine = this.getNestedMagazine(itemStack);
        if (nestedMagazine.isEmpty())
            return;
        tooltip.add(nestedMagazine.getDisplayName());
        List<Component> innerMagazineLines = new ArrayList<>();
        this.addAmmoLinesToTooltipFromItems(this.getStoredAmmo(itemStack), innerMagazineLines);
        this.addAmmoLinesToTooltipFromItems(this.getStoredSecondaryAmmo(itemStack), innerMagazineLines);
        for (Component component : innerMagazineLines)
            tooltip.add(Component.literal("  ").append(component));
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject obj) {
            int stacksTo = GsonHelper.getAsInt(obj, "stacks_to", 64);
            if (stacksTo < 1)
                throw new IllegalStateException("'stacks_to' must be at least 1");
            int stacksToWhenFilled = GsonHelper.getAsInt(obj, "stacks_to_when_filled", 1);
            if (stacksToWhenFilled < 1)
                throw new IllegalStateException("'stacks_to_when_filled' must be at least 1");
            Rarity rarity = RFEUtils.getRarityFromString(GsonHelper.getAsString(obj, "rarity", "common"));
            boolean glint = GsonHelper.getAsBoolean(obj, "glint", false);
            int primaryCapacity = GsonHelper.getAsInt(obj, "capacity", 0);
            int secondaryCapacity = GsonHelper.getAsInt(obj, "secondary_capacity", 0);
            if (primaryCapacity < 1 && secondaryCapacity < 1)
                throw new IllegalStateException("'capacity' or 'secondary_capacity' must be at least 1; both cannot be 0");
            if (primaryCapacity < 0)
                throw new IllegalStateException("'capacity' must be at least 0");
            if (secondaryCapacity < 0)
                throw new IllegalStateException("'secondary_capacity' must be at least 0");
            int reloadCooldown = GsonHelper.getAsInt(obj, "reload_cooldown", 20);
            if (reloadCooldown < 0)
                throw new IllegalStateException("'reload_cooldown' must be at least 0");
            JsonArray ammoPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_magazines");
            ImmutableList.Builder<AmmoPredicate> magazinePredicates = ImmutableList.builder();
            for (JsonElement el : ammoPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    magazinePredicates.add(pred);
            }
            return new NestedMagazineItem(new Properties().stacksTo(stacksTo).rarity(rarity)
                    .component(RFEDataComponents.FILLED_MAX_STACK_SIZE, stacksToWhenFilled),
                    glint, primaryCapacity, secondaryCapacity, magazinePredicates.build(), reloadCooldown);
        }
    }

}
