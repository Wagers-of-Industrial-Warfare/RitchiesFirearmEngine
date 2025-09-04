package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MagazineItem extends Item {

    private final boolean glint;
    private final int capacity;
    private final ImmutableList<AmmoPredicate> defaultAmmoPredicates; // Datapackable
    private final ImmutableList<AmmoPredicate> defaultSpeedloaderPredicates; // Datapackable
    private final int reloadCooldown;
    private final boolean trackEmptySlots;

    public MagazineItem(Properties pProperties, boolean glint, int capacity, ImmutableList<AmmoPredicate> defaultAmmoPredicates,
                        ImmutableList<AmmoPredicate> defaultSpeedloaderPredicates, int reloadCooldown, boolean trackEmptySlots) {
        super(pProperties);
        this.glint = glint;
        this.capacity = capacity;
        this.defaultAmmoPredicates = defaultAmmoPredicates;
        this.defaultSpeedloaderPredicates = defaultSpeedloaderPredicates;
        this.reloadCooldown = reloadCooldown;
        this.trackEmptySlots = trackEmptySlots;
        MagazineItemPropertiesHandler.registerDefaults(this, defaultAmmoPredicates, defaultSpeedloaderPredicates);
    }

    @Override public boolean isFoil(ItemStack stack) { return this.glint || super.isFoil(stack); }

    // TODO not forge
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.countAmmo(stack) > 0 ? 1 : super.getMaxStackSize(stack);
    }

    public int getMagazineCapacity(ItemStack itemStack) { return this.capacity; }

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

    public ImmutableList<AmmoPredicate> getSpeedloaderPredicate(ItemStack itemStack) {
        ImmutableList<AmmoPredicate> predicates = MagazineItemPropertiesHandler.getValidSpeedloaderPredicates(this);
        return predicates == null ? this.defaultSpeedloaderPredicates : predicates;
    }

    public boolean matchesSpeedloaderItem(ItemStack magazine, ItemStack speedloader) {
        for (AmmoPredicate pred : this.getSpeedloaderPredicate(magazine)) {
            if (pred.test(speedloader))
                return true;
        }
        return false;
    }

    public int getReloadCooldown(ItemStack itemStack) { return this.reloadCooldown; }

    public List<ItemStack> getStoredAmmo(ItemStack itemStack) {
        return FirearmDataUtils.getRounds(itemStack, "Rounds");
    }

    public void writeStoredAmmo(ItemStack itemStack, List<ItemStack> ammo) {
        FirearmDataUtils.saveRounds(itemStack, "Rounds", ammo);
    }

    public int countAmmo(ItemStack itemStack) {
        return RFEItemUtils.countItems(this.getStoredAmmo(itemStack));
    }

    public void tryReloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        List<ItemStack> ammo = this.getStoredAmmo(itemStack);
        int capacity = this.getMagazineCapacity(itemStack);
        if (this.trackEmptySlots) {
            int diff = capacity - RFEItemUtils.countItemsIncludingSlots(ammo);
            for (int i = 0; i < diff; ++i)
                ammo.add(ItemStack.EMPTY);
        }
        if (RFEItemUtils.countItems(ammo) >= capacity)
            return;
        boolean split = itemStack.getCount() > 1;
        RFEItemUtils.consumeItemsFromEntity(entity, s -> {
            return s != itemStack && this.tryReloadForItem(itemStack, ammo, s);
        }, s -> {
            ItemStack writeTo = split ? itemStack.split(1) : itemStack;
            this.writeStoredAmmo(writeTo, ammo);
            this.applyCooldown(writeTo, entity);
            if (split) {
                if (s.isEmpty())
                    return writeTo; // Take place of emptied ammo
                RFEItemUtils.addItemToEntity(writeTo, entity);
            }
            return s.isEmpty() ? ItemStack.EMPTY : s;
        });
    }

    protected boolean tryReloadForItem(ItemStack magazineStack, List<ItemStack> ammo, ItemStack availableStack) {
        if (this.matchesAmmoItem(magazineStack, availableStack)) {
            int consumed = FirearmDataUtils.addAmmo(ammo, availableStack, false, this.trackEmptySlots, 1);
            if (!availableStack.is(RFEItemTags.INFINITE_AMMO.tag))
                availableStack.shrink(consumed);
            return true;
        }
        if (availableStack.getItem() instanceof MagazineItem secondary && this.matchesSpeedloaderItem(magazineStack, availableStack)) {
            List<ItemStack> speedloaderAmmo = secondary.getStoredAmmo(availableStack);
            boolean allValid = !speedloaderAmmo.isEmpty();
            for (ItemStack ammoStack : speedloaderAmmo) {
                if (this.matchesAmmoItem(magazineStack, ammoStack))
                    continue;
                allValid = false;
                break;
            }
            if (allValid) {
                int added = FirearmDataUtils.addMultipleAmmo(ammo, speedloaderAmmo, false, true, this.trackEmptySlots, this.getMagazineCapacity(magazineStack));
                if (!availableStack.is(RFEItemTags.INFINITE_AMMO.tag)) {
                    FirearmDataUtils.stripMultipleAmmo(speedloaderAmmo, added, true, false, this.trackEmptySlots, true);
                    secondary.writeStoredAmmo(availableStack, speedloaderAmmo);
                }
                return true;
            }
        }
        return false;
    }

    public void tryUnloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        List<ItemStack> ammo = this.getStoredAmmo(itemStack);
        if (RFEItemUtils.countItems(ammo) == 0)
            return;
        ItemStack stripped = FirearmDataUtils.stripAmmo(ammo, false, false, this.trackEmptySlots);
        this.writeStoredAmmo(itemStack, ammo);
        this.applyCooldown(itemStack, entity);

        if (stripped.isEmpty())
            return;
        RFEItemUtils.addItemToEntity(stripped, entity);
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
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, tooltip, flag);
        List<ItemStack> storedAmmo = this.getStoredAmmo(itemStack);
        Map<Item, Integer> storedIndex = new LinkedHashMap<>();
        for (ItemStack ammoStack : storedAmmo) {
            if (!ammoStack.isEmpty())
                storedIndex.merge(ammoStack.getItem(), ammoStack.getCount(), Integer::sum);
        }
        for (Map.Entry<Item, Integer> entry : storedIndex.entrySet()) {
            MutableComponent itemTitle = Component.translatable(entry.getKey().getDescriptionId()).copy();
            itemTitle.append(" x").append(Component.literal(String.valueOf(entry.getValue())));
            tooltip.add(itemTitle);
        }
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject obj) {
            int stacksTo = GsonHelper.getAsInt(obj, "stacks_to", 64);
            if (stacksTo < 1)
                throw new IllegalStateException("'stacks_to' must be at least 1");
            Rarity rarity = RFEUtils.getRarityFromString(GsonHelper.getAsString(obj, "rarity", "common"));
            boolean glint = GsonHelper.getAsBoolean(obj, "glint", false);
            int capacity = GsonHelper.getAsInt(obj, "capacity");
            if (capacity < 1)
                throw new IllegalStateException("'capacity' must be at least 1");
            boolean trackEmptySlots = GsonHelper.getAsBoolean(obj, "track_empty_slots", false);
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
            return new MagazineItem(new Properties().stacksTo(stacksTo).rarity(rarity), glint, capacity, ammoPredicates.build(),
                    speedloaderPredicates.build(), reloadCooldown, trackEmptySlots);
        }
    }

}
