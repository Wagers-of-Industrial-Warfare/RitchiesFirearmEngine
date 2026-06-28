package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudItemInfoProviders;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class MagazineItem extends Item {

    private final boolean glint;
    private final int primaryCapacity;
    private final int secondaryCapacity;
    private final ImmutableList<AmmoPredicate> defaultAmmoPredicates; // Datapackable
    private final ImmutableList<AmmoPredicate> defaultSpeedloaderPredicates; // Datapackable
    private final ImmutableList<AmmoPredicate> defaultSecondaryPredicates; // Datapackable
    private final int reloadCooldown;
    private final boolean trackEmptySlots;
    private final boolean trackEmptySecondarySlots;

    public MagazineItem(Properties pProperties, boolean glint, int primaryCapacity, int secondaryCapacity,
                        ImmutableList<AmmoPredicate> defaultAmmoPredicates,
                        ImmutableList<AmmoPredicate> defaultSpeedloaderPredicates,
                        ImmutableList<AmmoPredicate> defaultSecondaryPredicates,
                        int reloadCooldown, boolean trackEmptySlots, boolean trackEmptySecondarySlots) {
        super(pProperties.component(RFEDataComponents.ROUNDS, RFEItemContainerContents.EMPTY)
                .component(RFEDataComponents.PRIMERS, RFEItemContainerContents.EMPTY));
        this.glint = glint;
        this.primaryCapacity = primaryCapacity;
        this.secondaryCapacity = secondaryCapacity;
        this.defaultAmmoPredicates = defaultAmmoPredicates;
        this.defaultSpeedloaderPredicates = defaultSpeedloaderPredicates;
        this.defaultSecondaryPredicates = defaultSecondaryPredicates;
        this.reloadCooldown = reloadCooldown;
        this.trackEmptySlots = trackEmptySlots;
        this.trackEmptySecondarySlots = trackEmptySecondarySlots;
        MagazineItemPropertiesHandler.registerDefaults(this, defaultAmmoPredicates, defaultSpeedloaderPredicates, defaultSecondaryPredicates);
        this.registerHUDProviders();
    }

    protected void registerHUDProviders() {
        RFEHudItemInfoProviders.registerHudInfoProvider(this, new RFEHudItemInfoProviders.RFEHudInfoProvider() {
            @Nullable
            @Override
            public List<ItemStack> getPrimaryAmmo(ItemStack itemStack) {
                return MagazineItem.this.getAmmoItemsForHUD(itemStack);
            }

            @Nullable
            @Override
            public List<ItemStack> getSecondaryAmmo(ItemStack itemStack) {
                return MagazineItem.this.getSecondaryItemsForHUD(itemStack);
            }

            @Override
            public Optional<Integer> countPrimaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
                return MagazineItem.this.getInventoryAmmoCountForHUD(itemStack, inventory, countLooseRounds).getFirst();
            }

            @Override
            public Optional<Integer> countSecondaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
                return MagazineItem.this.getInventoryAmmoCountForHUD(itemStack, inventory, countLooseRounds).getSecond();
            }
        });
    }

    @Override public boolean isFoil(ItemStack stack) { return this.glint || super.isFoil(stack); }

    // TODO not forge
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.countAmmo(stack) > 0 ? stack.getOrDefault(RFEDataComponents.FILLED_MAX_STACK_SIZE, 1) : super.getMaxStackSize(stack);
    }

    public int getMagazineCapacity(ItemStack itemStack) { return this.primaryCapacity; }

    public int getSecondaryMagazineCapacity(ItemStack itemStack) { return this.secondaryCapacity; }

    public ImmutableList<AmmoPredicate> getAmmoPredicates(ItemStack itemStack) {
        ImmutableList<AmmoPredicate> predicates = MagazineItemPropertiesHandler.getValidAmmoPredicates(this);
        return predicates == null ? this.defaultAmmoPredicates : predicates;
    }

    public boolean matchesAmmoItem(ItemStack magazine, ItemStack ammo) {
        for (AmmoPredicate pred : this.getAmmoPredicates(magazine)) {
            if (pred.test(ammo))
                return true;
        }
        return false;
    }

    public ImmutableList<AmmoPredicate> getSpeedloaderPredicates(ItemStack itemStack) {
        ImmutableList<AmmoPredicate> predicates = MagazineItemPropertiesHandler.getValidSpeedloaderPredicates(this);
        return predicates == null ? this.defaultSpeedloaderPredicates : predicates;
    }

    public boolean matchesSpeedloaderItem(ItemStack magazine, ItemStack speedloader) {
        for (AmmoPredicate pred : this.getSpeedloaderPredicates(magazine)) {
            if (pred.test(speedloader))
                return true;
        }
        return false;
    }

    public ImmutableList<AmmoPredicate> getSecondaryPredicates(ItemStack itemStack) {
        ImmutableList<AmmoPredicate> predicates = MagazineItemPropertiesHandler.getValidSecondaryPredicates(this);
        return predicates == null ? this.defaultSecondaryPredicates : predicates;
    }

    public boolean matchesSecondaryItem(ItemStack magazine, ItemStack primer) {
        if (FirearmDataUtils.isUsedPrimer(primer))
            return false;
        for (AmmoPredicate pred : this.getSecondaryPredicates(magazine)) {
            if (pred.test(primer))
                return true;
        }
        return false;
    }

    public int getReloadCooldown(ItemStack itemStack) { return this.reloadCooldown; }

    public List<ItemStack> getStoredAmmo(ItemStack itemStack) {
        List<ItemStack> stored = FirearmDataUtils.getRounds(itemStack, RFEDataComponents.ROUNDS);
        if (this.trackEmptySlots) {
            int diff = this.getMagazineCapacity(itemStack) - RFEItemUtils.countItemsIncludingSlots(stored);
            for (int i = 0; i < diff; ++i)
                stored.add(ItemStack.EMPTY);
        }
        return stored;
    }

    public void writeStoredAmmo(ItemStack itemStack, List<ItemStack> ammo) {
        FirearmDataUtils.saveRounds(itemStack, RFEDataComponents.ROUNDS, ammo);
    }

    public int countAmmo(ItemStack itemStack) {
        return RFEItemUtils.countItems(this.getStoredAmmo(itemStack));
    }

    public List<ItemStack> getStoredSecondaryAmmo(ItemStack itemStack) {
        List<ItemStack> stored = FirearmDataUtils.getRounds(itemStack, RFEDataComponents.PRIMERS);
        if (this.trackEmptySecondarySlots) {
            int diff = this.getSecondaryMagazineCapacity(itemStack) - RFEItemUtils.countItemsIncludingSlots(stored);
            for (int i = 0; i < diff; ++i)
                stored.add(ItemStack.EMPTY);
        }
        return stored;
    }

    public void writeStoredSecondaryAmmo(ItemStack itemStack, List<ItemStack> primers) {
        FirearmDataUtils.saveRounds(itemStack, RFEDataComponents.PRIMERS, primers);
    }

    public int countSecondaryAmmo(ItemStack itemStack) {
        return RFEItemUtils.countItems(this.getStoredSecondaryAmmo(itemStack));
    }

    public void tryReloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        List<ItemStack> primaryAmmo = this.getStoredAmmo(itemStack);
        List<ItemStack> secondaryAmmo = this.getStoredSecondaryAmmo(itemStack);
        int primaryCapacity = this.getMagazineCapacity(itemStack);
        int secondaryCapacity = this.getSecondaryMagazineCapacity(itemStack);
        if (RFEItemUtils.countItems(primaryAmmo) >= primaryCapacity
                && RFEItemUtils.countItemsConditional(secondaryAmmo, Predicate.not(FirearmDataUtils::isUsedPrimer)) >= secondaryCapacity)
            return;
        boolean split = itemStack.getCount() > 1;
        RFEItemUtils.consumeItemsFromEntity(entity, s -> {
            return s != itemStack && this.tryReloadForItem(itemStack, primaryAmmo, secondaryAmmo, s);
        }, s -> {
            ItemStack writeTo = split ? itemStack.split(1) : itemStack;
            this.writeStoredAmmo(writeTo, primaryAmmo);
            this.writeStoredSecondaryAmmo(writeTo, secondaryAmmo);
            this.applyCooldown(writeTo, entity);
            if (split) {
                if (s.isEmpty())
                    return writeTo; // Take place of emptied ammo
                RFEItemUtils.addItemToEntity(writeTo, entity);
            }
            return s.isEmpty() ? ItemStack.EMPTY : s;
        });
    }

    protected boolean tryReloadForItem(ItemStack magazineStack, List<ItemStack> primaryAmmo, List<ItemStack> secondaryAmmo, ItemStack availableStack) {
        if (this.matchesAmmoItem(magazineStack, availableStack)) {
            int consumed = FirearmDataUtils.addAmmo(primaryAmmo, availableStack, false, this.trackEmptySlots, 1);
            if (!availableStack.is(RFEItemTags.INFINITE_AMMO.tag))
                availableStack.shrink(consumed);
            return true;
        }
        if (this.matchesSecondaryItem(magazineStack, availableStack)) {
            int consumed = FirearmDataUtils.addAmmo(secondaryAmmo, availableStack, false, this.trackEmptySecondarySlots, 1);
            if (!availableStack.is(RFEItemTags.INFINITE_AMMO.tag))
                availableStack.shrink(consumed);
            return true;
        }
        if (availableStack.getItem() instanceof MagazineItem speedloader && this.matchesSpeedloaderItem(magazineStack, availableStack)) {
            List<ItemStack> speedloaderAmmo = speedloader.getStoredAmmo(availableStack);
            List<ItemStack> speedloaderSecondary = speedloader.getStoredSecondaryAmmo(availableStack);
            if (speedloaderAmmo.isEmpty() && speedloaderSecondary.isEmpty())
                return false;
            for (ItemStack ammoStack : speedloaderAmmo) {
                if (this.matchesAmmoItem(magazineStack, ammoStack))
                    continue;
                return false;
            }
            for (ItemStack primerStack : speedloaderSecondary) {
                if (this.matchesSecondaryItem(magazineStack, primerStack))
                    continue;
                return false;
            }
            int primaryAdded = FirearmDataUtils.addMultipleAmmo(primaryAmmo, speedloaderAmmo, false, true, this.trackEmptySlots, this.getMagazineCapacity(magazineStack));
            int secondaryAdded = FirearmDataUtils.addMultipleAmmo(secondaryAmmo, speedloaderSecondary, false, true, this.trackEmptySecondarySlots, this.getSecondaryMagazineCapacity(magazineStack));
            if (!availableStack.is(RFEItemTags.INFINITE_AMMO.tag)) {
                FirearmDataUtils.stripMultipleAmmo(speedloaderAmmo, primaryAdded, true, false, this.trackEmptySlots, true);
                FirearmDataUtils.stripMultipleAmmo(speedloaderSecondary, secondaryAdded, true, false, this.trackEmptySecondarySlots, true);
                speedloader.writeStoredAmmo(availableStack, speedloaderAmmo);
                speedloader.writeStoredSecondaryAmmo(availableStack, speedloaderSecondary);
            }
            return true;
        }
        return false;
    }

    public void tryUnloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        List<ItemStack> ammo = this.getStoredAmmo(itemStack);
        if (RFEItemUtils.countItems(ammo) > 0) {
            ItemStack stripped = FirearmDataUtils.stripAmmo(ammo, false, false, false);
            if (!stripped.isEmpty())
                RFEItemUtils.addItemToEntity(stripped, entity);
            this.writeStoredAmmo(itemStack, ammo);
            this.applyCooldown(itemStack, entity);
            return;
        }
        List<ItemStack> secondary = this.getStoredSecondaryAmmo(itemStack);
        if (RFEItemUtils.countItems(secondary) > 0) {
            ItemStack stripped = FirearmDataUtils.stripAmmo(secondary, false, false, false);
            if (!stripped.isEmpty())
                RFEItemUtils.addItemToEntity(stripped, entity);
            this.writeStoredSecondaryAmmo(itemStack, secondary);
            this.applyCooldown(itemStack, entity);
        }
    }

    protected boolean isOnCooldown(LivingEntity entity) {
        if (entity instanceof Player player && player.getCooldowns().isOnCooldown(this))
            return true;
        return false;
    }

    protected void applyCooldown(ItemStack itemStack, LivingEntity entity) {
        if (entity instanceof Player player)
            player.getCooldowns().addCooldown(this, this.getReloadCooldown(itemStack));
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, context, tooltip, flag);
        List<ItemStack> storedAmmo = this.getStoredAmmo(itemStack);
        Map<Item, Integer> storedIndex = new LinkedHashMap<>();
        for (ItemStack ammoStack : storedAmmo) {
            if (!ammoStack.isEmpty())
                storedIndex.merge(ammoStack.getItem(), ammoStack.getCount(), Integer::sum);
        }
        int additional = 0;
        int MAX_TYPES = RFEConfig.CLIENT.maxMagazineItemTypesDisplayed.get();
        int types = 0;
        for (Map.Entry<Item, Integer> entry : storedIndex.entrySet()) {
            ++types;
            if (types > MAX_TYPES) {
                additional += entry.getValue();
            } else {
                MutableComponent itemTitle = Component.translatable(entry.getKey().getDescriptionId()).copy();
                itemTitle.append(" x").append(Component.literal(String.valueOf(entry.getValue())));
                tooltip.add(itemTitle);
            }
        }
        if (additional > 0)
            tooltip.add(Component.translatable("container.rfe_builtin.magazine.more", additional).withStyle(ChatFormatting.ITALIC));
    }

    public List<ItemStack> getAmmoItemsForHUD(ItemStack itemStack) {
        return this.getStoredAmmo(itemStack);
    }

    public Pair<Optional<Integer>, Optional<Integer>> getInventoryAmmoCountForHUD(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
        Predicate<ItemStack> ammoPredicate = RFEUtils.orAllPredicates(this.getAmmoPredicates(itemStack));
        Predicate<ItemStack> secondaryPredicate = RFEUtils.orAllPredicates(this.getSecondaryPredicates(itemStack));
        Predicate<ItemStack> speedloaderPredicate = RFEUtils.orAllPredicates(this.getSpeedloaderPredicates(itemStack));
        int primaryCount = 0;
        int secondaryCount = 0;
        stackIter:
        for (ItemStack invStack : inventory) {
            if (primaryCount != -1 && countLooseRounds && ammoPredicate.test(invStack)) {
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag)) {
                    primaryCount = -1;
                } else {
                    primaryCount += invStack.getCount();
                }
            } else if (secondaryCount != -1 && countLooseRounds && secondaryPredicate.test(invStack) && !FirearmDataUtils.isUsedPrimer(invStack)) {
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag)) {
                    secondaryCount = -1;
                } else {
                    secondaryCount += invStack.getCount();
                }
            } else if (speedloaderPredicate.test(invStack) && invStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> magAmmo = magazineItem.getStoredAmmo(invStack);
                List<ItemStack> magSecondaryAmmo = magazineItem.getStoredSecondaryAmmo(invStack);
                if (magAmmo.isEmpty() && magSecondaryAmmo.isEmpty())
                    continue;
                int magCount = 0;
                for (ItemStack magStack : magAmmo) {
                    if (magStack.isEmpty())
                        continue;
                    if (!ammoPredicate.test(magStack))
                        continue stackIter;
                    magCount += magStack.getCount();
                }
                int magSecondaryCount = 0;
                for (ItemStack magSecondaryStack : magSecondaryAmmo) {
                    if (magSecondaryStack.isEmpty())
                        continue;
                    if (!secondaryPredicate.test(magSecondaryStack))
                        continue stackIter;
                    magSecondaryCount += magSecondaryStack.getCount();
                }
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag)) {
                    if (magCount > 0)
                        primaryCount = -1;
                    if (magSecondaryCount > 0)
                        secondaryCount = -1;
                } else {
                    primaryCount += magCount;
                    secondaryCount += magSecondaryCount;
                }
            }
            if (primaryCount == -1 && secondaryCount == -1)
                break;
        }
        return Pair.of(Optional.of(primaryCount), Optional.of(secondaryCount));
    }

    private List<ItemStack> getSecondaryItemsForHUD(ItemStack itemStack) {
        return this.getStoredSecondaryAmmo(itemStack);
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
            boolean trackEmptySlots = GsonHelper.getAsBoolean(obj, "track_empty_slots", false);
            boolean trackEmptySecondarySlots = GsonHelper.getAsBoolean(obj, "track_empty_secondary_slots", false);
            int reloadCooldown = GsonHelper.getAsInt(obj, "reload_cooldown", 5);
            if (reloadCooldown < 0)
                throw new IllegalStateException("'reload_cooldown' must be at least 0");
            JsonArray ammoPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_ammo");
            ImmutableList.Builder<AmmoPredicate> ammoPredicates = ImmutableList.builder();
            for (JsonElement el : ammoPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    ammoPredicates.add(pred);
            }
            JsonArray speedloaderPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_speedloaders", new JsonArray());
            ImmutableList.Builder<AmmoPredicate> speedloaderPredicates = ImmutableList.builder();
            for (JsonElement el : speedloaderPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    speedloaderPredicates.add(pred);
            }
            JsonArray secondaryPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_secondaries", new JsonArray());
            ImmutableList.Builder<AmmoPredicate> secondaryPredicates = ImmutableList.builder();
            for (JsonElement el : secondaryPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    secondaryPredicates.add(pred);
            }
            return new MagazineItem(new Properties().stacksTo(stacksTo).rarity(rarity)
                        .component(RFEDataComponents.FILLED_MAX_STACK_SIZE, stacksToWhenFilled),
                    glint, primaryCapacity, secondaryCapacity, ammoPredicates.build(), speedloaderPredicates.build(), secondaryPredicates.build(),
                    reloadCooldown, trackEmptySlots, trackEmptySecondarySlots);
        }
    }

}
