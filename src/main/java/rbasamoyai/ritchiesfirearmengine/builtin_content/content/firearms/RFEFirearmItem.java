package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFirearmModeAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Basic firearms class.
 */
public abstract class RFEFirearmItem extends Item implements IFirearmItem {

    protected final Map<String, RFEFirearmMode> baseFirearmModes;
    protected final List<String> modeOrder;
    protected final String defaultMode;

    protected RFEFirearmItem(Properties properties, Map<String, RFEFirearmMode> baseFirearmModes, List<String> modeOrder, String defaultMode) {
        super(properties.stacksTo(1));
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
            return true;
        FirearmDataUtils.setHoldingAttackKey(stack, true);
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        if (firearmMode.canFireProjectile(stack, entity)) {
            firearmMode.fireProjectile(stack, entity);
        } else if (firearmMode.canCharge(stack, entity)) {
            firearmMode.onCharge(stack, entity);
        }
        return true;
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

    public float countEntityAmmo(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        List<ItemStack> entityAmmo = RFEItemUtils.getItemsFromEntity(entity, RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates()), 0, false);
        int count = 0;
        for (ItemStack ammo : entityAmmo) {
            if (ammo.is(RFEItemTags.INFINITE_AMMO.tag))
                return Float.POSITIVE_INFINITY;
            count += ammo.getCount();
        }
        return count;
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
        return mode.entityHasMagazine(itemStack, entity);
    }

    public int bestSpeedloaderAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.bestSpeedloaderAmmoCount(itemStack, entity);
    }

    public boolean laysFlatOnGround(ItemStack stack) {
        return true;
    }

    public boolean disableAttackAnimation(ItemStack itemStack, Player player) {
        return true; // TODO melee?
    }

    @Override
    public float getFov(ItemStack itemStack, Player player, float currentFovModifier, float partialTicks) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        boolean isAiming = player.isUsingItem();
        float zoomIn = 0.5f; // TODO configurable by attachments, etc

        int denom = mode.isAiming(itemStack, player) ? mode.aimTime() : mode.unaimTime();
        float aimingTime = (float) denom - mode.getAimingTime(itemStack, player);
        float frac = denom > 0 ? aimingTime / (float) denom : 1;
        float frac1 = denom > 0 ? partialTicks / (float) denom : 0;
        float d = isAiming ? frac + frac1 : 1 - frac - frac1;
        d = Mth.clamp(d, 0f, 1f);
        float d1 = d * d * d;
        return Mth.lerp(d1, 1f, zoomIn) * currentFovModifier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        boolean startUsing = false;
        if (mode.canAim(itemStack, player)) {
            mode.startAiming(itemStack, player);
            startUsing = true;
        }
        // TODO other interactions
        return startUsing ? ItemUtils.startUsingInstantly(level, player, hand) : super.use(level, player, hand);
    }

    @Override public int getUseDuration(ItemStack itemStack) { return 72000; }

    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
        this.stopAiming(itemStack, entity);
        return super.finishUsingItem(itemStack, level, entity);
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int timeCharged) {
        this.stopAiming(itemStack, entity);
        super.releaseUsing(itemStack, level, entity, timeCharged);
    }

    public void stopAiming(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        mode.stopAiming(itemStack, entity);
    }

    @Override
    public boolean isAiming(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.isAiming(itemStack, entity);
    }

    @Nullable
    public static List<ItemStack> getAmmoItemsForHUD(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof RFEFirearmItem firearm))
            return null;
        RFEFirearmMode mode = firearm.getCurrentMode(itemStack);
        return mode.requiresAmmo() ? mode.getLoadedAmmo(itemStack) : null;
    }

    public static Optional<Integer> getInventoryAmmoCountForHUD(ItemStack itemStack, List<ItemStack> inventory) {
        if (!(itemStack.getItem() instanceof RFEFirearmItem firearm))
            return Optional.empty();
        RFEFirearmMode mode = firearm.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        Predicate<ItemStack> primaryAmmoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmo().keySet());
        Predicate<ItemStack> magazineAndSpeedloaderPred = RFEUtils.orAllPredicates(ammoProperties.magazines())
                .or(RFEUtils.orAllPredicates(ammoProperties.speedloaders()));
        int count = 0;
        for (ItemStack invStack : inventory) {
            if (primaryAmmoPred.test(invStack)) {
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag))
                    return Optional.of(-1);
                count += invStack.getCount();
            } else if (magazineAndSpeedloaderPred.test(invStack) && invStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> magAmmo = magazineItem.getStoredAmmo(invStack);
                int magCount = 0;
                for (ItemStack magStack : magAmmo) {
                    if (!primaryAmmoPred.test(magStack)) {
                        magCount = 0;
                        break;
                    }
                    magCount += magStack.getCount();
                }
                count += magCount;
            }
        }
        // TODO test magazines and stripper clips
        return Optional.of(count);
    }

    public enum Action implements StringRepresentable {
        RELOAD(false),
        UNLOAD(false),
        FIRING(true),
        CHARGING(true),
        DRAW(false),
        SWITCH_MODE(true),
        COOLDOWN(false);

        private static final Map<String, Action> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(Action::getSerializedName, Function.identity()));

        private final String id = this.name().toLowerCase(Locale.ROOT);

        private final boolean canAim;

        Action(boolean canAim) {
            this.canAim = canAim;
        }

        @Override public String getSerializedName() { return this.id; }

        @Nullable public static Action byId(String id) { return BY_ID.get(id); }

        public boolean canAim() { return this.canAim; }
    }

}
