package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.RFETooltip;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFiringInput;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeHandlingProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhaseAccessFilter;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudItemInfoProviders;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadManager;
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
public abstract class RFEFirearmItem extends Item implements IFirearmItem, IHasRFEItemAttachments {

    protected final Map<String, RFEFirearmMode> baseFirearmModes;
    protected final List<String> modeOrder;
    protected final String defaultMode;
    protected final Set<ResourceLocation> globalAttachments;

    protected RFEFirearmItem(Properties properties, Map<String, RFEFirearmMode> baseFirearmModes, List<String> modeOrder,
                             String defaultMode, Set<ResourceLocation> globalAttachments) {
        super(properties.stacksTo(1));
        this.baseFirearmModes = baseFirearmModes;
        this.modeOrder = modeOrder;
        this.defaultMode = defaultMode;
        this.globalAttachments = globalAttachments;
        this.registerHUDProviders();
    }

    protected void registerHUDProviders() {
        RFEHudItemInfoProviders.registerHudInfoProvider(this, new RFEHudItemInfoProviders.RFEHudInfoProvider() {
            @Nullable
            @Override
            public List<ItemStack> getPrimaryAmmo(ItemStack itemStack) {
                return RFEFirearmItem.this.getAmmoItemsForHUD(itemStack);
            }

            @Nullable
            @Override
            public List<ItemStack> getSecondaryAmmo(ItemStack itemStack) {
                return RFEFirearmItem.this.getSecondaryAmmoItemsForHUD(itemStack);
            }

            @Override
            public Optional<Integer> countPrimaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
                return RFEFirearmItem.this.getInventoryAmmoCountForHUD(itemStack, inventory, countLooseRounds);
            }

            @Override
            public Optional<Integer> countSecondaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
                return RFEFirearmItem.this.getInventorySecondaryAmmoCountForHUD(itemStack, inventory, countLooseRounds);
            }

            @Override
            public float getHeatAmount(ItemStack itemStack) {
                return RFEFirearmItem.this.getHeatAmountForHUD(itemStack);
            }

            @Override
            public float getHeatCapacity(ItemStack itemStack) {
                return RFEFirearmItem.this.getHeatCapacityForHUD(itemStack);
            }
        });
    }

    public static Predicate<ItemStack> getGuiValidAmmoPredicate(ItemStack itemStack, RFEFirearmItem firearmItem, boolean shiftDown, boolean ctrlDown) {
        if (!ctrlDown && shiftDown) { // Only display current mode consumable ammo
            RFEFirearmModeAmmoProperties modeProperties = firearmItem.getCurrentMode(itemStack).getAmmoProperties(itemStack);
            return modeProperties::isValidItem;
        } else if (ctrlDown && shiftDown) { // Show all consumable ammo
            RFEFirearmProperties<RFEFirearmModeAmmoProperties> ammo = RFEFirearmAmmoHandler.getAmmoProperties(itemStack);
            return s -> {
                if (ammo.defaultProperties().isValidItem(s))
                    return true;
                for (RFEFirearmModeAmmoProperties modeProperties : ammo.propertiesByMode().values()) {
                    if (modeProperties.isValidItem(s))
                        return true;
                }
                return false;
            };
        }
        return s -> false;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        List<ItemAttributeModifiers.Entry> modifiers = new ArrayList<>(2);
        RFEFirearmMode mode = this.getCurrentMode(stack);
        RFEFirearmModeHandlingProperties properties = mode.getHandlingProperties(stack);
        float speedModifierValue = properties.movementSpeedModifier();
        if (speedModifierValue != 0f) {
            AttributeModifier speedModifier = new AttributeModifier(RitchiesFirearmEngine.resource("handling_movement_speed"),
                    speedModifierValue, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            modifiers.add(new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED.getDelegate(), speedModifier, EquipmentSlotGroup.MAINHAND));
            modifiers.add(new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED.getDelegate(), speedModifier, EquipmentSlotGroup.OFFHAND));
        }
        return new ItemAttributeModifiers(modifiers, true);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(itemStack, level, entity, slotId, isSelected);
        if (!isSelected) {
            FirearmDataUtils.setHoldingAttackKey(itemStack, false);
            if (entity instanceof LivingEntity living) {
                RFESpreadManager.stopTrackingSpread(living, itemStack);
                RFERecoilManager.stopTrackingRecoil(living, itemStack);
            }
        }
        if (entity instanceof LivingEntity living)
            this.getCurrentMode(itemStack).onTick(itemStack, living, isSelected);
        // TODO clientside effects?
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return true; // TODO other swinging
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public Map<String, RFEFirearmMode> getFirearmModes(ItemStack stack, @Nullable LivingEntity entity) {
        return new LinkedHashMap<>(this.baseFirearmModes);
    }

    public RFEFirearmMode getCurrentMode(ItemStack stack) {
        String stateRef = stack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.FIREARM_MODE, this.defaultMode);
        return this.baseFirearmModes.getOrDefault(stateRef, this.getDefaultMode());
    }

    public RFEFirearmMode getDefaultMode() {
        return this.baseFirearmModes.get(this.defaultMode);
    }

    @Override
    public boolean onPressAttackKey(ItemStack stack, LivingEntity entity) {
        FirearmDataUtils.setHoldingAttackKey(stack, true);
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        if (firearmMode.canFireProjectile(stack, entity)) {
            if (!firearmMode.isBurstFiring(stack, entity))
                firearmMode.fireFirearm(stack, entity, RFEFirearmMode.FiringType.CLICK);
        } else if (firearmMode.canChargeInternal(stack, entity)) {
            firearmMode.onCharge(stack, entity);
        } else if (firearmMode.canCancelReloadOrUnloadByClick(stack, entity)) {
            firearmMode.setForceCancelAction(stack, entity, true);
        }
        // TODO alternative API for entity interaction
        return true;
    }

    public void onEntityTryAttackOption(ItemStack stack, LivingEntity entity) {
        FirearmDataUtils.setHoldingAttackKey(stack, true);
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        if (firearmMode.canFireProjectile(stack, entity)) {
            if (!firearmMode.isBurstFiring(stack, entity)) {
                firearmMode.fireFirearm(stack, entity, RFEFirearmMode.FiringType.CLICK);
            }
            if (this.getCurrentAction(stack) == Action.FIRING)
                return;
        }
        if (firearmMode.requiresAmmo() && !firearmMode.hasAmmo(stack) || firearmMode.requiresSecondaryAmmo() && !firearmMode.hasSecondaryAmmo(stack)) {
            firearmMode.tryRunningReloadAction(stack, entity, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE);
            if (this.getCurrentAction(stack) == Action.RELOAD)
                return;
        }
        if (firearmMode.canChargeInternal(stack, entity) && !firearmMode.isCharged(stack)) {
            firearmMode.onCharge(stack, entity);
            if (this.getCurrentAction(stack) == Action.CHARGING)
                return;
        }
        if (firearmMode.tryRunningReloadAction(stack, entity, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE)) {
            if (this.getCurrentAction(stack) == Action.RELOAD)
                return;
        }
    }

    @Override
    public void handleClientFireInputOnServer(ItemStack itemStack, LivingEntity entity, List<RFEFiringInput> firingInputs,
                                              boolean jam, @Nullable UUID recoilUUID, InteractionHand hand) {
        long currentTime = entity.level().getGameTime();
        long lastFiredTime = itemStack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.LAST_SHOT_TIME, -1L);
        if (currentTime != lastFiredTime) {
            itemStack.set(BuiltInRFEPlugin.RFEDataComponents.LAST_SHOT_TIME, currentTime);
        } else {
            return; // Do not accept more than 1 clientbound shot from an entity/player each tick
        }
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        mode.handleFiringInputOnServer(itemStack, entity, firingInputs, jam, recoilUUID, hand);
    }

    @Override
    public void handleServerAutomaticFireOnClient(ItemStack itemStack, LivingEntity entity, InteractionHand hand,
                                                  RFERecoilClientImpulse recoil, @Nullable UUID recoilUUID) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        mode.handleServerRecoil(itemStack, entity, hand, recoil, recoilUUID);
        mode.fireFirearm(itemStack, entity, RFEFirearmMode.FiringType.AUTOMATIC);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        RFETooltip.addAmmoHighlightingTooltip(stack, context, tooltipComponents, tooltipFlag);
    }

    public void onReload(ItemStack stack, LivingEntity entity) {
        RFEFirearmMode firearmMode = this.getCurrentMode(stack);
        firearmMode.tryRunningReloadAction(stack, entity, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE);
    }

    public void onUnload(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode firearmMode = this.getCurrentMode(itemStack);
        firearmMode.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE);
    }

    public void onSwitchMode(ItemStack itemStack, LivingEntity entity) {
        if (this.modeOrder.size() == 1)
            return;
        if (this.getCurrentAction(itemStack) != null)
            return;
        String stateRef = itemStack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.FIREARM_MODE, this.defaultMode);
        int index = this.modeOrder.indexOf(stateRef);
        if (index == -1) {
            this.warnInvalidModeAndReset(itemStack);
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
        String stateRef = itemStack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.FIREARM_MODE, this.defaultMode);
        int index = this.modeOrder.indexOf(stateRef);
        if (index == -1) {
            this.warnInvalidModeAndReset(itemStack);
            return;
        }
        ++index;
        if (index >= this.modeOrder.size())
            index = 0;
        String nextStateRef = this.modeOrder.get(index);
        itemStack.set(BuiltInRFEPlugin.RFEDataComponents.FIREARM_MODE, nextStateRef);
    }

    protected void warnInvalidModeAndReset(ItemStack itemStack) {
        RitchiesFirearmEngine.LOGGER.warn("Firearm {} has invalid mode setup", BuiltInRegistries.ITEM.getKey(this));
        itemStack.set(BuiltInRFEPlugin.RFEDataComponents.FIREARM_MODE, this.defaultMode);
    }

    @Override
    public boolean isHoldingAttackKey(ItemStack itemStack, LivingEntity player) {
        return FirearmDataUtils.isHoldingAttackKey(itemStack);
    }

    @Override
    public void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setHoldingAttackKey(itemStack, false);
        RFEFirearmMode firearmMode = this.getCurrentMode(itemStack);
        firearmMode.onReleaseAttackKey(itemStack, entity);
    }

    public float countEntityAmmo(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        List<ItemStack> entityAmmo = RFEItemUtils.getItemsFromEntity(entity, ammoPred, 0, false);
        int count = 0;
        for (ItemStack ammo : entityAmmo) {
            if (ammo.is(RFEItemTags.INFINITE_AMMO.tag))
                return Float.POSITIVE_INFINITY;
            count += ammo.getCount();
        }
        if (count > 0)
            return count;
        if (!canEntityInfiniteReload(entity))
            return 0;
        ItemStack infiniteAmmo = ammoProperties.unlimitedPrimaryReloadItem();
        return ammoPred.test(infiniteAmmo) ? Float.POSITIVE_INFINITY : 0;
    }

    public int ammoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.getLoadedAmmoCount(itemStack, entity, true);
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

    public float countEntitySecondaryAmmo(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.secondaryAmmo());
        List<ItemStack> entityAmmo = RFEItemUtils.getItemsFromEntity(entity, ammoPred, 0, false);
        int count = 0;
        for (ItemStack ammo : entityAmmo) {
            if (FirearmDataUtils.isUsedPrimer(ammo))
                continue;
            if (ammo.is(RFEItemTags.INFINITE_AMMO.tag))
                return Float.POSITIVE_INFINITY;
            count += ammo.getCount();
        }
        if (count > 0)
            return count;
        if (!canEntityInfiniteReload(entity))
            return 0;
        ItemStack infiniteAmmo = ammoProperties.unlimitedSecondaryReloadItem();
        return ammoPred.test(infiniteAmmo) ? Float.POSITIVE_INFINITY : 0;
    }

    public int freeSecondaryAmmoSpace(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.countFreeSecondaryAmmoSpaces(itemStack);
    }

    public int usedSecondaryAmmoCount(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.countUsedSecondaryAmmo(itemStack);
    }

    public int secondaryAmmoCount(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.countSecondaryAmmo(itemStack);
    }

    public boolean hasChamberedSecondary(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.hasChamberedSecondary(itemStack);
    }

    public boolean hasChamberedUsedSecondary(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.hasChamberedUsedSecondary(itemStack);
    }

    public int primableAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.primableAmmoCount(itemStack, entity);
    }

    public int primedAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.primedAmmoCount(itemStack, entity);
    }

    public boolean laysFlatOnGround(ItemStack stack) {
        return true;
    }

    public float getAttackStrengthScaleForRendering(ItemStack itemStack, Player player, float original) {
        if (this.getCurrentAction(itemStack) == Action.DRAW)
            return this.getCurrentMode(itemStack).getDrawFraction(itemStack, player);
        // TODO melee?
        return 1;
    }

    @Nullable public Action getCurrentAction(ItemStack itemStack) { return FirearmDataUtils.getAction(itemStack); }

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        boolean startUsing = false;
        if (this.tryAiming(itemStack, player))
            startUsing = true;
        // TODO other interactions
        return startUsing ? ItemUtils.startUsingInstantly(level, player, usedHand) : super.use(level, player, usedHand);
    }

    public boolean tryAiming(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        if (mode.canAim(itemStack, entity)) {
            mode.startAiming(itemStack, entity);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return super.onItemUseFirst(stack, context);
        ItemStack itemStack = context.getItemInHand();
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        boolean startUsing = false;
        if (mode.canAim(itemStack, player)) {
            mode.startAiming(itemStack, player);
            startUsing = true;
        }
        // TODO other interactions
        return startUsing ? ItemUtils.startUsingInstantly(context.getLevel(), player, context.getHand()).getResult() : super.onItemUseFirst(stack, context);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

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

    public List<ItemStack> getAmmoItemsForHUD(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.requiresAmmo() ? mode.getLoadedAmmo(itemStack) : null;
    }

    public List<ItemStack> getSecondaryAmmoItemsForHUD(ItemStack itemStack) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.requiresSecondaryAmmo() ? mode.getLoadedSecondaryAmmo(itemStack) : null;
    }

    public float getItemLength(ItemStack itemStack, @Nullable LivingEntity entity) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        return mode.getItemLength(itemStack, entity);
    }

    public Optional<Integer> getInventoryAmmoCountForHUD(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        Predicate<ItemStack> primaryAmmoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        Predicate<ItemStack> secondaryPred = RFEUtils.orAllPredicates(ammoProperties.secondaryAmmo());
        Predicate<ItemStack> magazineAndSpeedloaderPred = RFEUtils.orAllPredicates(ammoProperties.magazines())
                .or(RFEUtils.orAllPredicates(ammoProperties.speedloaders()));
        int count = 0;
        for (ItemStack invStack : inventory) {
            if (countLooseRounds && primaryAmmoPred.test(invStack)) {
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag))
                    return Optional.of(-1);
                count += invStack.getCount();
            } else if (magazineAndSpeedloaderPred.test(invStack) && invStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> magAmmo = magazineItem.getStoredAmmo(invStack);
                int magCount = 0;
                if (magAmmo.isEmpty())
                    continue;
                boolean valid = true;
                if (mode.requiresSecondaryAmmo() && magazineItem.getSecondaryMagazineCapacity(invStack) > 0) {
                    List<ItemStack> secondaryAmmo = magazineItem.getStoredSecondaryAmmo(invStack);
                    for (ItemStack secondaryStack : secondaryAmmo) {
                        if (!secondaryPred.test(secondaryStack)) {
                            valid = false;
                            break;
                        }
                    }
                }
                if (valid) {
                    for (ItemStack magStack : magAmmo) {
                        if (!primaryAmmoPred.test(magStack)) {
                            magCount = 0;
                            valid = false;
                            break;
                        }
                        magCount += magStack.getCount();
                    }
                }
                if (valid && invStack.is(RFEItemTags.INFINITE_AMMO.tag))
                    return Optional.of(-1);
                count += magCount;
            }
        }
        return Optional.of(count);
    }

    public Optional<Integer> getInventorySecondaryAmmoCountForHUD(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) {
        RFEFirearmMode mode = this.getCurrentMode(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = mode.getAmmoProperties(itemStack);
        Predicate<ItemStack> secondaryPred = RFEUtils.orAllPredicates(ammoProperties.secondaryAmmo());
        Predicate<ItemStack> primaryAmmoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        Predicate<ItemStack> magazinePred = RFEUtils.orAllPredicates(ammoProperties.magazines());
        int count = 0;
        for (ItemStack invStack : inventory) {
            if (FirearmDataUtils.isUsedPrimer(invStack))
                continue;
            if (countLooseRounds && secondaryPred.test(invStack)) {
                if (invStack.is(RFEItemTags.INFINITE_AMMO.tag))
                    return Optional.of(-1);
                count += invStack.getCount();
            } else if (magazinePred.test(invStack) && invStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> magAmmo = magazineItem.getStoredSecondaryAmmo(invStack);
                int magCount = 0;
                if (magAmmo.isEmpty())
                    continue;
                boolean valid = true;
                if (mode.requiresAmmo() && magazineItem.getMagazineCapacity(invStack) > 0) {
                    List<ItemStack> primaryAmmo = magazineItem.getStoredAmmo(invStack);
                    for (ItemStack primaryStack : primaryAmmo) {
                        if (!primaryAmmoPred.test(primaryStack)) {
                            valid = false;
                            break;
                        }
                    }
                }
                if (valid) {
                    for (ItemStack magStack : magAmmo) {
                        if (!secondaryPred.test(magStack)) {
                            magCount = 0;
                            valid = false;
                            break;
                        }
                        magCount += magStack.getCount();
                    }
                }
                if (valid && invStack.is(RFEItemTags.INFINITE_AMMO.tag))
                    return Optional.of(-1);
                count += magCount;
            }
        }
        return Optional.of(count);
    }

    public float getHeatAmountForHUD(ItemStack itemStack) {
        return this.getCurrentMode(itemStack).getHeatAmount(itemStack);
    }

    public float getHeatCapacityForHUD(ItemStack itemStack) {
        return this.getCurrentMode(itemStack).getHeatCapacity(itemStack);
    }

    @Override
    public Map<ResourceLocation, ItemStack> getAttachments(ItemStack stack) {
        Map<ResourceLocation, ItemStack> attachmentsRet = new Object2ObjectOpenHashMap<>();
        for (RFEFirearmMode mode : this.getFirearmModes(stack, null).values())
            mode.addAttachments(stack, attachmentsRet);
        return attachmentsRet;
    }

    public int getShotCount(ItemStack itemStack) {
        return this.getCurrentMode(itemStack).getShotCount(itemStack);
    }

    public static boolean canEntityInfiniteReload(LivingEntity entity) {
        if (entity instanceof Player player)
            return player.isCreative();
        return true; // TODO config
    }

    public int getAIShootingRange() {
        return 16; // TODO config and possibly per entity. This is mostly for BehaviorUtils/BehaviorUtilsMixin
    }

    public enum Action implements StringRepresentable {
        RELOAD(false),
        UNLOAD(false),
        FIRING(true),
        CHARGING(true),
        DRAW(false),
        SWITCH_MODE(true),
        COOLDOWN(false);

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Action.class);

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
