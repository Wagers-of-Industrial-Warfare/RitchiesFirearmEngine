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
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AmmoPacketItem extends Item {

    private final boolean glint;
    private final int useDuration;
    private final int reloadCooldown;
    @Nullable private final SoundEvent useSound;
    private final boolean spawnParticlesOnUse;
    private final ImmutableMap<AmmoPredicate, Integer> defaultAmmoCapacities; // Datapackable

    public AmmoPacketItem(Properties properties, boolean glint, int useDuration, int reloadCooldown,
                          @Nullable SoundEvent useSound, boolean spawnParticlesOnUse,
                          ImmutableMap<AmmoPredicate, Integer> defaultAmmoCapacities) {
        super(properties);
        this.glint = glint;
        this.useDuration = useDuration;
        this.reloadCooldown = reloadCooldown;
        this.useSound = useSound;
        this.spawnParticlesOnUse = spawnParticlesOnUse;
        this.defaultAmmoCapacities = defaultAmmoCapacities;
        AmmoPacketItemPropertiesHandler.registerDefaults(this, defaultAmmoCapacities);
    }

    public ImmutableMap<AmmoPredicate, Integer> getAmmoCapacities() {
        ImmutableMap<AmmoPredicate, Integer> ammoCapacities = AmmoPacketItemPropertiesHandler.getAmmoCapacities(this);
        return ammoCapacities == null ? this.defaultAmmoCapacities : ammoCapacities;
    }

    @Override public boolean isFoil(ItemStack itemStack) { return this.glint || super.isFoil(itemStack); }

    @Override public int getUseDuration(ItemStack itemStack) { return this.useDuration; }

    @Override public UseAnim getUseAnimation(ItemStack itemStack) { return UseAnim.DRINK; }

    @Nullable public SoundEvent getPacketUseSound(ItemStack itemStack) { return this.useSound; }
    public boolean spawnParticlesOnUse(ItemStack itemStack) { return this.spawnParticlesOnUse; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.getStoredAmmo(itemStack).isEmpty())
            return super.use(level, player, hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
        List<ItemStack> ammo = this.getStoredAmmo(itemStack);
        for (ItemStack ammoStack : ammo)
            RFEItemUtils.addItemToEntity(ammoStack, entity);
        if (itemStack.getCount() > 1) {
            RFEItemUtils.addItemToEntity(new ItemStack(this), entity);
            itemStack.shrink(1);
            return itemStack;
        } else {
            return new ItemStack(this);
        }
    }

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
        if (!this.getStoredAmmo(itemStack).isEmpty())
            return;
        boolean split = itemStack.getCount() > 1;
        List<ItemStack> ammo = new ArrayList<>();
        RFEItemUtils.consumeItemsFromEntity(entity, s -> {
            return s != itemStack && this.tryReloadForItem(ammo, s);
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

    protected boolean tryReloadForItem(List<ItemStack> ammo, ItemStack availableStack) {
        ImmutableMap<AmmoPredicate, Integer> ammoCapacities = this.getAmmoCapacities();
        if (ammo.isEmpty()) {
            for (Map.Entry<AmmoPredicate, Integer> entry : ammoCapacities.entrySet()) {
                if (!entry.getKey().test(availableStack))
                    continue;
                int consumed = FirearmDataUtils.addAmmo(ammo, availableStack, false, entry.getValue());
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
                int consumed = FirearmDataUtils.addAmmo(ammo, availableStack, false, addable);
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
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, tooltip, flag);
        List<ItemStack> storedAmmo = this.getStoredAmmo(itemStack);
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

            JsonArray primaryAmmoCapArr = GsonHelper.getAsJsonArray(obj, "ammo");
            ImmutableMap.Builder<AmmoPredicate, Integer> primaryAmmoCapacities = ImmutableMap.builder();
            for (JsonElement el : primaryAmmoCapArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Ammo packet capacity must be a json object");
                JsonObject capObj = el.getAsJsonObject();
                AmmoPredicate pred = AmmoPredicate.fromString(GsonHelper.getAsString(capObj, "ammo"));
                int capacity = GsonHelper.getAsInt(capObj, "capacity");
                if (capacity < 1)
                    throw new IllegalStateException("'capacity' must be at least 1");
                primaryAmmoCapacities.put(pred, capacity);
            }

            return new AmmoPacketItem(new Properties().stacksTo(stacksTo).rarity(rarity), glint, useDuration, reloadCooldown,
                    useSound, spawnParticlesOnUse, primaryAmmoCapacities.build());
        }
    }

}
