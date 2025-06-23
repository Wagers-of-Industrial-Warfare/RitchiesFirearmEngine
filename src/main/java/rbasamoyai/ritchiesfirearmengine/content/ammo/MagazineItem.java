package rbasamoyai.ritchiesfirearmengine.content.ammo;

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
import org.jetbrains.annotations.Nullable;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MagazineItem extends Item {

    private final boolean glint;
    private final int capacity; // Datapackable
    private final List<AmmoPredicate> defaultAmmoPredicates; // Datapackable
    private final List<AmmoPredicate> defaultSpeedloaderPredicates; // Datapackable
    private final int reloadCooldown; // Datapackable

    public MagazineItem(Properties pProperties, boolean glint, int capacity, List<AmmoPredicate> defaultAmmoPredicates,
                        List<AmmoPredicate> defaultSpeedloaderPredicates, int reloadCooldown) {
        super(pProperties);
        this.glint = glint;
        this.capacity = capacity;
        this.defaultAmmoPredicates = defaultAmmoPredicates;
        this.defaultSpeedloaderPredicates = defaultSpeedloaderPredicates;
        this.reloadCooldown = reloadCooldown;
    }

    @Override public boolean isFoil(ItemStack stack) { return this.glint || super.isFoil(stack); }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.countAmmo(stack) > 0 ? 1 : super.getMaxStackSize(stack);
    }

    public int getMagazineCapacity(ItemStack itemStack) { return this.capacity; }

    public List<AmmoPredicate> getAmmoPredicates(ItemStack itemStack) { return this.defaultAmmoPredicates; }

    public boolean matchesAmmoItem(ItemStack magazine, ItemStack ammo) {
        for (AmmoPredicate pred : this.getAmmoPredicates(magazine)) {
            if (pred.test(ammo))
                return true;
        }
        return false;
    }

    public List<AmmoPredicate> getSpeedloaderPredicate(ItemStack itemStack) { return this.defaultSpeedloaderPredicates; }

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
        if (RFEItemUtils.countItems(ammo) >= this.getMagazineCapacity(itemStack))
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
            int consumed = FirearmDataUtils.addAmmo(ammo, availableStack, false, 1);
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
                int added = FirearmDataUtils.addMultipleAmmo(ammo, speedloaderAmmo, false, true, this.getMagazineCapacity(magazineStack));
                FirearmDataUtils.stripMultipleAmmo(speedloaderAmmo, added, true, false);
                secondary.writeStoredAmmo(availableStack, speedloaderAmmo);
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
        ItemStack stripped = FirearmDataUtils.stripAmmo(ammo, false, false);
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
        for (ItemStack ammoStack : storedAmmo)
            storedIndex.merge(ammoStack.getItem(), ammoStack.getCount(), Integer::sum);
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
            int reloadCooldown = GsonHelper.getAsInt(obj, "reload_cooldown", 5);
            if (reloadCooldown < 0)
                throw new IllegalStateException("'reload_cooldown' must be at least 0");
            JsonArray ammoPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_ammo");
            List<AmmoPredicate> ammoPredicates = new LinkedList<>();
            for (JsonElement el : ammoPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    ammoPredicates.add(pred);
            }
            JsonArray speedloaderPredicateArr = GsonHelper.getAsJsonArray(obj, "valid_speedloaders", new JsonArray());
            List<AmmoPredicate> speedloaderPredicates = new LinkedList<>();
            for (JsonElement el : speedloaderPredicateArr) {
                AmmoPredicate pred = AmmoPredicate.fromString(el.getAsString());
                if (pred != null)
                    speedloaderPredicates.add(pred);
            }
            return new MagazineItem(new Properties().stacksTo(stacksTo).rarity(rarity), glint, capacity, ammoPredicates,
                    speedloaderPredicates, reloadCooldown);
        }
    }

}
