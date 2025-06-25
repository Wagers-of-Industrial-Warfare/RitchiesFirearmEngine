package rbasamoyai.ritchiesfirearmengine.content.firearms;

import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.content.SimultaneousUseAndAttack;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.FirearmModeDataPackProperties;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Basic firearms class.
 */
public abstract class RFEFirearmItem extends Item implements SimultaneousUseAndAttack, HoldAttackKeyInteraction {

    protected final Map<String, RFEFirearmMode> baseFirearmModes;
    protected final List<String> modeOrder;
    protected final String defaultMode;

    protected RFEFirearmItem(Properties properties, Map<String, RFEFirearmMode> baseFirearmModes, List<String> modeOrder, String defaultMode) {
        super(properties);
        this.baseFirearmModes = baseFirearmModes;
        this.modeOrder = modeOrder;
        this.defaultMode = defaultMode;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(itemStack, level, entity, slotId, isSelected);
        if (level.isClientSide)
            return; // TODO maybe clientside effects?
        if (!isSelected)
            FirearmDataUtils.setHoldingAttackKey(itemStack, false);
        if (entity instanceof LivingEntity living)
            this.getCurrentMode(itemStack).onTick(itemStack, living, isSelected);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return this.commonOnEntitySwing(stack, entity);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public Map<String, RFEFirearmMode> getFirearmModes(ItemStack stack, LivingEntity entity) {
        return new LinkedHashMap<>(this.baseFirearmModes);
    }

    public RFEFirearmMode getCurrentMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        String stateRef = tag.contains("FirearmMode", Tag.TAG_STRING) ? tag.getString("FirearmMode") : this.defaultMode;
        return this.baseFirearmModes.getOrDefault(stateRef, this.getDefaultMode());
    }

    public RFEFirearmMode getDefaultMode() {
        return this.baseFirearmModes.get(this.defaultMode);
    }

    public boolean commonOnEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return false;
        FirearmDataUtils.setHoldingAttackKey(stack, true);
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        if (firearmMode.canFireProjectile(stack, entity)) {
            firearmMode.fireProjectile(stack, entity);
        } else if (firearmMode.canCharge(stack, entity)) {
            firearmMode.onCharge(stack, entity);
        }
        return false;
    }

    public void onReload(ItemStack stack, LivingEntity entity) {
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        firearmMode.tryRunningReloadAction(stack, entity, ReloadPhase.PhaseType.PREPARE);
    }

    public void onUnload(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode firearmMode = this.getCurrentMode(itemStack);
        firearmMode.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.PREPARE);
    }

    public void onSwitchMode(ItemStack itemStack, LivingEntity entity) {
        if (this.modeOrder.size() == 1)
            return;
        CompoundTag tag = itemStack.getOrCreateTag();
        String stateRef = tag.contains("FirearmMode", Tag.TAG_STRING) ? tag.getString("FirearmMode") : this.defaultMode;
        int index = this.modeOrder.indexOf(stateRef);
        if (index == -1) {
            this.warnInvalidModeAndReset(itemStack);
            tag.putString("FirearmMode", this.defaultMode);
            return;
        }
        ++index;
        if (index >= this.modeOrder.size())
            index = 0;
        String nextStateRef = this.modeOrder.get(index);
        RFEFirearmMode nextMode = this.baseFirearmModes.getOrDefault(nextStateRef, this.getDefaultMode());
        if (nextMode == null) {
            this.warnInvalidModeAndReset(itemStack);
            return;
        }
        nextMode.startSwitchMode(itemStack, entity);
    }

    public void completeSwitchMode(ItemStack itemStack, LivingEntity entity) {
        CompoundTag tag = itemStack.getOrCreateTag();
        String stateRef = tag.contains("FirearmMode", Tag.TAG_STRING) ? tag.getString("FirearmMode") : this.defaultMode;
        int index = this.modeOrder.indexOf(stateRef);
        if (index == -1) {
            this.warnInvalidModeAndReset(itemStack);
            return;
        }
        ++index;
        if (index >= this.modeOrder.size())
            index = 0;
        String nextStateRef = this.modeOrder.get(index);
        tag.putString("FirearmMode", nextStateRef);
    }

    protected void warnInvalidModeAndReset(ItemStack itemStack) {
        RitchiesFirearmEngine.LOGGER.warn("Firearm {} has invalid mode setup", BuiltInRegistries.ITEM.getKey(this));
        itemStack.getOrCreateTag().putString("FirearmMode", this.defaultMode);
    }

    @Override
    public boolean isHoldingAttackKey(ItemStack itemStack, Player player) {
        return FirearmDataUtils.isHoldingAttackKey(itemStack);
    }

    @Override
    public void onReleaseAttackKey(ItemStack itemStack, Player player) {
        FirearmDataUtils.setHoldingAttackKey(itemStack, false);
        RFEFirearmMode firearmMode = this.getCurrentMode(itemStack);
        firearmMode.onReleaseAttackKey(itemStack, player);
    }

    public int countAmmo(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        FirearmModeDataPackProperties properties = mode.getDataPackProperties();
        return RFEItemUtils.countItems(RFEItemUtils.getItemsFromEntity(entity, RFEUtils.orAllPredicates(properties.primaryAmmoPredicates()), 0, false));
    }

    public int freeAmmoSpace(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.countFreeAmmoSpaces(itemStack);
    }

    public int extraAmmoSpace(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.countExtraAmmoSpaces(itemStack);
    }

    public boolean hasAmmo(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.hasAmmo(itemStack);
    }

    public boolean hasChamberedRound(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.hasChamberedRound(itemStack);
    }

    public boolean isCharged(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.isCharged(itemStack);
    }

    public boolean isJammed(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.isJammed(itemStack);
    }

    public boolean hasMagazine(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.hasMagazine(itemStack);
    }

    public boolean entityHasMagazine(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.entityHasMagazine(entity);
    }

    public int bestSpeedloaderAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.bestSpeedloaderAmmoCount(itemStack, entity);
    }

    public enum Action implements StringRepresentable {
        RELOAD,
        UNLOAD,
        FIRING,
        CHARGING,
        DRAW,
        SWITCH_MODE,
        COOLDOWN;

        private static final Map<String, Action> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(Action::getSerializedName, Function.identity()));

        private final String id = this.name().toLowerCase(Locale.ROOT);

        @Override public String getSerializedName() { return this.id; }

        @Nullable public static Action byId(String id) { return BY_ID.get(id); }
    }

}
