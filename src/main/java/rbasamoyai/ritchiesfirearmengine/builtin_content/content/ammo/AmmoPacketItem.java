package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class AmmoPacketItem extends Item {

    private final boolean glint;
    private final int useDuration;
    private final int reloadCooldown;
    @Nullable private final SoundEvent useSound;
    private final boolean spawnParticlesOnUse;
    private final ImmutableMap<AmmoPredicate, Integer> defaultPrimaryAmmoCapacities; // Datapackable
    private final ImmutableMap<AmmoPredicate, Integer> defaultSecondaryAmmoCapacities; // Datapackable

    public AmmoPacketItem(Properties properties, boolean glint, int useDuration, int reloadCooldown,
                          @Nullable SoundEvent useSound, boolean spawnParticlesOnUse,
                          ImmutableMap<AmmoPredicate, Integer> defaultPrimaryAmmoCapacities,
                          ImmutableMap<AmmoPredicate, Integer> defaultSecondaryAmmoCapacities) {
        super(properties.component(BuiltInRFEPlugin.RFEDataComponents.ROUNDS, RFEItemContainerContents.EMPTY)
                .component(BuiltInRFEPlugin.RFEDataComponents.PRIMERS, RFEItemContainerContents.EMPTY));
        this.glint = glint;
        this.useDuration = useDuration;
        this.reloadCooldown = reloadCooldown;
        this.useSound = useSound;
        this.spawnParticlesOnUse = spawnParticlesOnUse;
        this.defaultPrimaryAmmoCapacities = defaultPrimaryAmmoCapacities;
        this.defaultSecondaryAmmoCapacities = defaultSecondaryAmmoCapacities;
        AmmoPacketItemPropertiesHandler.registerDefaults(this, defaultPrimaryAmmoCapacities, defaultSecondaryAmmoCapacities);
    }

    public ImmutableMap<AmmoPredicate, Integer> getPrimaryAmmoCapacities() {
        ImmutableMap<AmmoPredicate, Integer> ammoCapacities = AmmoPacketItemPropertiesHandler.getPrimaryAmmoCapacities(this);
        return ammoCapacities == null ? this.defaultPrimaryAmmoCapacities : ammoCapacities;
    }

    public ImmutableMap<AmmoPredicate, Integer> getSecondaryAmmoCapacities() {
        ImmutableMap<AmmoPredicate, Integer> ammoCapacities = AmmoPacketItemPropertiesHandler.getSecondaryAmmoCapacities(this);
        return ammoCapacities == null ? this.defaultSecondaryAmmoCapacities : ammoCapacities;
    }

    @Override public boolean isFoil(ItemStack itemStack) { return this.glint || super.isFoil(itemStack); }

    @Override public int getUseDuration(ItemStack itemStack, LivingEntity entity) { return this.useDuration; }

    @Override public UseAnim getUseAnimation(ItemStack itemStack) { return UseAnim.DRINK; }

    @Nullable public SoundEvent getPacketUseSound(ItemStack itemStack) { return this.useSound; }
    public boolean spawnParticlesOnUse(ItemStack itemStack) { return this.spawnParticlesOnUse; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.getStoredPrimaryAmmo(itemStack).isEmpty() && this.getStoredSecondaryAmmo(itemStack).isEmpty())
            return super.use(level, player, hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
        List<ItemStack> ammo = this.getStoredPrimaryAmmo(itemStack);
        for (ItemStack ammoStack : ammo)
            RFEItemUtils.addItemToEntity(ammoStack, entity);
        List<ItemStack> secondaryAmmo = this.getStoredSecondaryAmmo(itemStack);
        for (ItemStack ammoStack : secondaryAmmo)
            RFEItemUtils.addItemToEntity(ammoStack, entity);
        if (itemStack.getCount() > 1) {
            RFEItemUtils.addItemToEntity(new ItemStack(this), entity);
            itemStack.shrink(1);
            return itemStack;
        } else {
            return new ItemStack(this);
        }
    }

    public List<ItemStack> getStoredPrimaryAmmo(ItemStack itemStack) {
        return FirearmDataUtils.getRounds(itemStack, BuiltInRFEPlugin.RFEDataComponents.ROUNDS);
    }

    public void writeStoredPrimaryAmmo(ItemStack itemStack, List<ItemStack> ammo) {
        FirearmDataUtils.saveRounds(itemStack, BuiltInRFEPlugin.RFEDataComponents.ROUNDS, ammo);
    }

    public int countPrimaryAmmo(ItemStack itemStack) {
        return RFEItemUtils.countItems(this.getStoredPrimaryAmmo(itemStack));
    }

    public List<ItemStack> getStoredSecondaryAmmo(ItemStack itemStack) {
        return FirearmDataUtils.getRounds(itemStack, BuiltInRFEPlugin.RFEDataComponents.PRIMERS);
    }

    public void writeStoredSecondaryAmmo(ItemStack itemStack, List<ItemStack> ammo) {
        FirearmDataUtils.saveRounds(itemStack, BuiltInRFEPlugin.RFEDataComponents.PRIMERS, ammo);
    }

    public int countSecondaryAmmo(ItemStack itemStack) {
        return RFEItemUtils.countItems(this.getStoredSecondaryAmmo(itemStack));
    }

    public void tryReloadingOutsideOfMenu(ItemStack itemStack, LivingEntity entity) {
        if (this.isOnCooldown(entity))
            return;
        boolean split = itemStack.getCount() > 1;
        List<ItemStack> primaryAmmo = this.getStoredPrimaryAmmo(itemStack);
        List<ItemStack> secondaryAmmo = this.getStoredSecondaryAmmo(itemStack);
        ImmutableMap<AmmoPredicate, Integer> primaryCapacities = this.getPrimaryAmmoCapacities();
        ImmutableMap<AmmoPredicate, Integer> secondaryCapacities = this.getSecondaryAmmoCapacities();
        RFEItemUtils.consumeItemsFromEntity(entity, s -> {
            return s != itemStack && !FirearmDataUtils.isUsedPrimer(s)
                    && (this.tryReloadForItem(primaryAmmo, s, primaryCapacities) || this.tryReloadForItem(secondaryAmmo, s, secondaryCapacities));
        }, s -> {
            ItemStack writeTo = split ? itemStack.split(1) : itemStack;
            this.writeStoredPrimaryAmmo(writeTo, primaryAmmo);
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

    protected boolean tryReloadForItem(List<ItemStack> ammo, ItemStack availableStack, ImmutableMap<AmmoPredicate, Integer> ammoCapacities) {
        boolean infinite = availableStack.is(RFEItemTags.INFINITE_AMMO.tag);
        if (ammo.isEmpty()) {
            for (Map.Entry<AmmoPredicate, Integer> entry : ammoCapacities.entrySet()) {
                if (!entry.getKey().test(availableStack))
                    continue;
                ItemStack toAdd = availableStack;
                if (infinite)
                    toAdd = availableStack.copyWithCount(entry.getValue());
                int consumed = FirearmDataUtils.addAmmo(ammo, toAdd, false, false, entry.getValue());
                if (!infinite)
                    availableStack.shrink(consumed);
                return true;
            }
        } else {
            ItemStack firstAmmo = ammo.get(0);
            int ammoCount = RFEItemUtils.countItems(ammo);
            for (Map.Entry<AmmoPredicate, Integer> entry : ammoCapacities.entrySet()) {
                if (!entry.getKey().test(firstAmmo)) // Match first ammo
                    continue;
                if (!entry.getKey().test(availableStack))
                    return false;
                int addable = Math.max(0, entry.getValue() - ammoCount);
                if (addable < 1)
                    return false;
                ItemStack toAdd = availableStack;
                if (infinite)
                    toAdd = availableStack.copyWithCount(addable);
                int consumed = FirearmDataUtils.addAmmo(ammo, toAdd, false, false, addable);
                if (!infinite)
                    availableStack.shrink(consumed);
                return true;
            }
        }
        return false;
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

    public int getReloadCooldown(ItemStack itemStack) { return this.reloadCooldown; }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, context, tooltip, flag);
        List<ItemStack> storedAmmo = this.getStoredPrimaryAmmo(itemStack);
        storedAmmo.addAll(this.getStoredSecondaryAmmo(itemStack));
        Map<Item, Integer> storedIndex = new Object2IntLinkedOpenHashMap<>();
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

            int useDuration = GsonHelper.getAsInt(obj, "use_time");
            if (useDuration < 0)
                throw new IllegalStateException("'use_time' must be at least 0");
            int reloadCooldown = GsonHelper.getAsInt(obj, "reload_cooldown", 5);
            if (reloadCooldown < 0)
                throw new IllegalStateException("'reload_cooldown' must be at least 0");
            SoundEvent useSound = null;
            if (GsonHelper.isStringValue(obj, "use_sound")) {
                String str = GsonHelper.getAsString(obj, "use_sound");
                useSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            }
            boolean spawnParticlesOnUse = GsonHelper.getAsBoolean(obj, "spawn_particles_on_use", false);

            JsonArray primaryAmmoCapArr = GsonHelper.getAsJsonArray(obj, "primary_ammo");
            ImmutableMap.Builder<AmmoPredicate, Integer> primaryAmmoCapacities = ImmutableMap.builder();
            for (JsonElement el : primaryAmmoCapArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Primary ammo packet capacity must be a json object");
                JsonObject capObj = el.getAsJsonObject();
                AmmoPredicate pred = AmmoPredicate.fromString(GsonHelper.getAsString(capObj, "ammo"));
                int capacity = GsonHelper.getAsInt(capObj, "capacity");
                if (capacity < 1)
                    throw new IllegalStateException("'capacity' must be at least 1");
                primaryAmmoCapacities.put(pred, capacity);
            }

            JsonArray secondaryAmmoCapArr = GsonHelper.getAsJsonArray(obj, "secondary_ammo");
            ImmutableMap.Builder<AmmoPredicate, Integer> secondaryAmmoCapacities = ImmutableMap.builder();
            for (JsonElement el : secondaryAmmoCapArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Secondary ammo packet capacity must be a json object");
                JsonObject capObj = el.getAsJsonObject();
                AmmoPredicate pred = AmmoPredicate.fromString(GsonHelper.getAsString(capObj, "ammo"));
                int capacity = GsonHelper.getAsInt(capObj, "capacity");
                if (capacity < 1)
                    throw new IllegalStateException("'capacity' must be at least 1");
                secondaryAmmoCapacities.put(pred, capacity);
            }

            return new AmmoPacketItem(new Properties().stacksTo(stacksTo).rarity(rarity), glint, useDuration, reloadCooldown,
                    useSound, spawnParticlesOnUse, primaryAmmoCapacities.build(), secondaryAmmoCapacities.build());
        }
    }

}
