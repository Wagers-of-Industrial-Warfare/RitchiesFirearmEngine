package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmCondition;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.network.ClientboundRunFiringLogicPacket;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundRunFiringLogicPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Basic firearm mode class.
 */
public class RFEFirearmMode {

    protected final String modeId;
    protected final String modeDisplayId;

    protected final RFEFirearmModeHandlingProperties defaultDataPackProperties;

    protected final String modeTagId;

    // Drawing
    protected final int drawTime;
    @Nullable protected final SoundEvent drawSound;

    // Mode change
    protected final int modeChangeTime;
    @Nullable protected final SoundEvent modeChangeSound;

    // Aiming
    protected final int aimTime;
    protected final int unaimTime;
    @Nullable protected final SoundEvent aimSound;
    @Nullable protected final SoundEvent unaimSound;

    // Ammo
    protected final boolean ammoRequired;
    protected final int internalCapacity;
    protected final int nominalCapacity;
    protected final boolean plusOneCapacity;
    protected final boolean requiresSecondaryAmmo;
    protected final boolean trackEmptySlots;

    // Firing
    protected final FireMode fireMode;
    protected final float firingCooldown;
    protected final boolean ammoConsumedLast;
    protected final boolean ignoreEmptySlotsWhenFiring;
    protected final int shotsFired;
    protected final int burstRoundCount;
    protected final boolean slamfire;
    @Nullable protected final SoundEvent firingSound;
    // Wind-up
    protected final int windUpTime;
    @Nullable protected final SoundEvent windUpSound;
    // Wind-down
    protected final int windDownTime;
    @Nullable protected final SoundEvent windDownSound;

    // Reloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> reloadPhases;
    protected final Map<ResourceLocation, CompareValueSource> reloadingCompareValues;

    // Unloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> unloadPhases;
    protected final Map<ResourceLocation, CompareValueSource> unloadingCompareValues;

    // Charging
    protected final List<ChargeAction> chargeActions;
    protected final Map<ResourceLocation, CompareValueSource> chargingCompareValues;

    // Overheating
    protected final boolean canOverheat;
    protected final int cooldownTime;
    @Nullable protected final SoundEvent cooldownSound;

    public RFEFirearmMode(RFEFirearmModeBuilder builder, String modeId) {
        this.modeId = modeId;
        this.modeDisplayId = builder.modeDisplayId;

        this.defaultDataPackProperties = RFEFirearmModeHandlingProperties.fromItemDefinition(builder);

        this.modeTagId = builder.modeTagId;

        this.drawTime = builder.drawTime;
        this.drawSound = builder.drawSound;

        this.modeChangeTime = builder.modeChangeTime;
        this.modeChangeSound = builder.modeChangeSound;

        this.aimTime = builder.aimTime;
        this.unaimTime = builder.unaimTime;
        this.aimSound = builder.aimSound;
        this.unaimSound = builder.unaimSound;

        this.ammoRequired = builder.ammoRequired;
        this.internalCapacity = builder.internalCapacity;
        this.nominalCapacity = builder.nominalCapacity;
        this.plusOneCapacity = builder.plusOneCapacity;
        this.requiresSecondaryAmmo = builder.requiresSecondaryAmmo;
        this.trackEmptySlots = builder.trackEmptySlots;

        this.fireMode = builder.fireMode;
        this.firingCooldown = builder.firingCooldown;
        this.ammoConsumedLast = builder.ammoConsumedLast;
        this.ignoreEmptySlotsWhenFiring = builder.ignoreEmptySlotsWhenFiring;
        this.shotsFired = builder.shotsFired;
        this.burstRoundCount = builder.burstRoundCount;
        this.slamfire = builder.slamfire;
        this.firingSound = builder.firingSound;
        this.windUpTime = builder.windUpTime;
        this.windUpSound = builder.windUpSound;
        this.windDownTime = builder.windDownTime;
        this.windDownSound = builder.windDownSound;

        this.reloadPhases = builder.finalReloadPhases;
        this.unloadPhases = builder.finalUnloadPhases;
        this.chargeActions = builder.chargeActions;

        this.reloadingCompareValues = builder.reloadingCompareValues;
        this.unloadingCompareValues = builder.unloadingCompareValues;
        this.chargingCompareValues = builder.chargingCompareValues;

        this.canOverheat = builder.canOverheat;
        this.cooldownTime = builder.cooldownTime;
        this.cooldownSound = builder.cooldownSound;
    }

    public String getModeId() { return this.modeId; }
    public String getDisplayId() { return this.modeDisplayId; }

    public FireMode getFireMode() { return this.fireMode; }

    public RFEFirearmModeHandlingProperties getHandlingProperties(ItemStack itemStack) {
        ImmutableMap<String, RFEFirearmModeHandlingProperties> handlingPropertiesByMode = RFEFirearmHandlingPropertiesHandler.getHandlingProperties(itemStack);
        return handlingPropertiesByMode.getOrDefault(this.modeId, this.defaultDataPackProperties);
    }

    public RFEFirearmModeAmmoProperties getAmmoProperties(ItemStack itemStack) {
        return RFEFirearmAmmoHandler.getAmmoProperties(itemStack).getProperties(this.modeId);
    }

    public CompoundTag getOrCreateModeTag(ItemStack itemStack) {
        CompoundTag topTag = itemStack.getOrCreateTag();
        if (!topTag.contains(this.modeTagId, Tag.TAG_COMPOUND))
            topTag.put(this.modeTagId, new CompoundTag());
        return topTag.getCompound(this.modeTagId);
    }

    public int getNominalCapacity(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0)
            return this.nominalCapacity;
        ItemStack magazine = ItemStack.of(this.getOrCreateModeTag(itemStack).getCompound("DetachedMagazine"));
        return magazine.getItem() instanceof MagazineItem magazineItem ? magazineItem.getMagazineCapacity(magazine) : 0;
    }

    public List<ItemStack> getLoadedAmmo(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0) {
            return FirearmDataUtils.getRounds(modeTag, "InternalRounds");
        } else {
            List<ItemStack> list = new ArrayList<>();
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty())
                    list.add(loadedRound);
            }
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                list.addAll(magazineItem.getStoredAmmo(magazine));
            return list;
        }
    }

    public int getLoadedAmmoCount(ItemStack itemStack, LivingEntity entity, boolean countPlusOne) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0)
            return RFEItemUtils.countItems(FirearmDataUtils.getRounds(modeTag, "InternalRounds"));
        int count = 0;
        if (this.plusOneCapacity && countPlusOne && !this.getLoadedRound(itemStack).isEmpty())
            ++count;
        CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
        ItemStack magazine = ItemStack.of(magazineTag);
        if (magazine.getItem() instanceof MagazineItem magazineItem)
            count += RFEItemUtils.countItems(magazineItem.getStoredAmmo(magazine));
        return count;
    }

    public List<ItemStack> getNextRoundsInItem(ItemStack itemStack, LivingEntity entity, int count, boolean strip) {
        List<ItemStack> totalStripped = new LinkedList<>();
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0) {
            List<ItemStack> list = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            List<ItemStack> internalStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                    !strip, this.trackEmptySlots, this.ignoreEmptySlotsWhenFiring);
            count -= RFEItemUtils.countItems(internalStripped);
            totalStripped.addAll(internalStripped);
            if (strip)
                FirearmDataUtils.saveRounds(modeTag, "InternalRounds", list);
        } else {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    --count;
                    totalStripped.add(loadedRound.split(1));
                    if (strip)
                        this.setLoadedRound(itemStack, ItemStack.EMPTY);
                }
            }
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> list = magazineItem.getStoredAmmo(magazine);
                List<ItemStack> magStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                        !strip, this.trackEmptySlots, this.ignoreEmptySlotsWhenFiring);
                count -= RFEItemUtils.countItems(magStripped);
                totalStripped.addAll(magStripped);
                if (strip) {
                    magazineItem.writeStoredAmmo(magazine, list);
                    modeTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
                }
            }
        }
        return totalStripped;
    }

    public ItemStack getLoadedRound(ItemStack itemStack) {
        return ItemStack.of(this.getOrCreateModeTag(itemStack).getCompound("LoadedRound"));
    }

    public void setLoadedRound(ItemStack itemStack, ItemStack roundStack) {
        if (roundStack.isEmpty()) {
            this.getOrCreateModeTag(itemStack).remove("LoadedRound");
        } else {
            this.getOrCreateModeTag(itemStack).put("LoadedRound", roundStack.save(new CompoundTag()));
        }
    }

    public boolean isBusyWithStagedAction(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        return modeTag.contains("UnloadPhase") || modeTag.contains("ReloadPhase");
    }

    public boolean canFireProjectile(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.fireMode == FireMode.SAFETY || !FirearmDataUtils.isCharged(modeTag) || FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        if (this.isJammed(itemStack))
            return false;
        if (!this.ammoRequired)
            return true;
        // TODO check for secondary ammo
        if (this.plusOneCapacity && this.getLoadedRound(itemStack).isEmpty())
            return false;
        return !this.getNextRoundsInItem(itemStack, entity, this.shotsFired, false).isEmpty();
    }

    public void fireFirearm(ItemStack itemStack, LivingEntity entity, FiringType firing) {
        InteractionHand hand = entity.getMainHandItem() == itemStack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean client = entity.level().isClientSide;

        // TODO windup

        if (entity instanceof Player && firing != FiringType.NON_PLAYER_AND_EFFECTS) {
            if (client && firing == FiringType.CLICK || !client && firing == FiringType.AUTOMATIC)
                this.handlePlayerAmmoAndShootingOnClient(itemStack, entity);
            return;
        } // TODO other entities

        if (!client)
            this.tryStartBurstFire(itemStack, entity);

        RFEFirearmModeHandlingProperties firearmProperties = this.getHandlingProperties(itemStack);
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);

        if (this.canOverheat) {
            FirearmDataUtils.addHeat(modeTag, firearmProperties.heatAddedOnFiring());
            FirearmDataUtils.setCoolingDelay(modeTag, firearmProperties.coolingDelayTime());
            if (FirearmDataUtils.getHeat(modeTag) > firearmProperties.heatCapacity())
                FirearmDataUtils.setOverheated(modeTag, true);
        }

        this.setCharged(itemStack, entity, false);
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.FIRING);
        if (this.firingCooldown > 0) {
            float extraActionTime = Math.max(modeTag.getFloat("ExtraFiringTime"), 0);
            float time = this.firingCooldown + extraActionTime;
            int actionTime = Mth.floor(time);
            float remainder = time - actionTime;
            FirearmDataUtils.setActionTime(itemStack, actionTime);
            modeTag.putFloat("ExtraFiringTime", remainder);
        }
        this.playFiringEffects(itemStack, entity);
    }

    protected void handlePlayerAmmoAndShootingOnClient(ItemStack itemStack, LivingEntity entity) {
        if (!FirearmDataUtils.isHoldingAttackKey(itemStack) && this.fireMode == FireMode.FULL_AUTO)
            return;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);

        // TODO recoil
        List<RFEProjectileType> toFire = new ArrayList<>();
        if (this.ammoRequired) {
            List<ItemStack> strippedAmmo = this.getNextRoundsInItem(itemStack, entity, this.shotsFired, false);
            // TODO consume secondary ammo if required
            for (ItemStack ammoStack : strippedAmmo) {
                for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : ammoProperties.primaryAmmo().entrySet()) {
                    if (entry.getKey().test(ammoStack)) {
                        int sz = ammoStack.getCount();
                        RFEProjectileType projectileType = entry.getValue();
                        for (int i = 0; i < sz; ++i)
                            toFire.add(projectileType);
                        break;
                    }
                }
            }
        } else {
            RFEProjectileType unlimitedProjectile = ammoProperties.unlimitedProjectile();
            if (unlimitedProjectile != null) {
                // TODO one-time warning if no projectile?
                for (int i = 0; i < this.shotsFired; ++i)
                    toFire.add(unlimitedProjectile);
            }
        }

        // TODO something better probably
        InteractionHand hand = entity.getMainHandItem() == itemStack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        Vec3 pos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());

        List<RFEFiringInput> firingInputs = new ArrayList<>();

        for (RFEProjectileType type : toFire) {
            // TODO shooter positioning
            Vec3 aimDirection = entity.getViewVector(1f);
            ResourceLocation typeId = RFEProjectileTypeHandler.getProjectileTypeId(type);
            if (typeId != null) {
                // TODO warn once if missing?
                firingInputs.add(new RFEFiringInput(typeId, aimDirection, pos));
            }
        }

        RFERecoilInstance recoilInstance = RFERecoilManager.getRecoilInstance(entity, itemStack);
        if (recoilInstance == null) {
            recoilInstance = RFERecoilProviderPackHandler.getRecoilProviders(itemStack).getProperties(this.modeId)
                    .createRecoilInstance(itemStack, entity, entity.getRandom());
            RFERecoilManager.trackRecoil(recoilInstance, entity, itemStack, hand);
        }
        RFERecoilClientImpulse impulse = recoilInstance.updateRecoil(itemStack, entity);

        boolean jam = this.fireMode.isSelfLoading() && this.shouldJam(itemStack, entity);
        this.setJammed(itemStack, entity, jam);

        UUID recoilUUID = RFERecoilManager.getRecoilId(itemStack);
        if (entity.level().isClientSide) {
            RFENetwork.sendToServer(new ServerboundRunFiringLogicPacket(firingInputs, jam, hand, recoilUUID));
        } else if (entity instanceof ServerPlayer splayer) {
            RFENetwork.sendToPlayer(new ClientboundRunFiringLogicPacket(hand, impulse), splayer);
            this.handleFiringInputOnServer(itemStack, entity, firingInputs, jam, recoilUUID, hand);
        }
    }

    public void handleServerRecoil(ItemStack itemStack, LivingEntity entity, InteractionHand hand, RFERecoilClientImpulse recoil) {
        RFERecoilInstance recoilInstance = RFERecoilManager.getRecoilInstance(entity, itemStack);
        if (recoilInstance == null) {
            recoilInstance = RFERecoilProviderPackHandler.getRecoilProviders(itemStack).getProperties(this.modeId)
                    .createRecoilInstance(itemStack, entity, entity.getRandom());
            RFERecoilManager.trackRecoil(recoilInstance, entity, itemStack, hand);
        }
        recoilInstance.updateRecoilWithImpulse(itemStack, entity, recoil);
    }

    public void handleFiringInputOnServer(ItemStack itemStack, LivingEntity entity, List<RFEFiringInput> firingInputs,
                                          boolean jam, @Nullable UUID recoilUUID, InteractionHand hand) {
        RFERecoilManager.setRecoilId(itemStack, recoilUUID);
        if (this.ammoRequired)
            this.getNextRoundsInItem(itemStack, entity, firingInputs.size(), true);

        RFESpreadInstance spreadInstance = RFESpreadManager.getSpreadInstance(entity, itemStack);
        if (spreadInstance == null) {
            spreadInstance = RFESpreadProviderPackHandler.getSpreadProviders(itemStack).getProperties(this.modeId)
                    .createSpreadInstance(itemStack, entity, entity.getRandom());
            RFESpreadManager.trackSpread(spreadInstance, entity, itemStack, hand);
        }

        for (RFEFiringInput input : firingInputs) {
            RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(input.projectile());
            if (type == null) {
                // TODO warn once?
                continue;
            }
            RFEProjectileInstance projectile = type.createInstance();
            projectile.setOwner(entity);
            projectile.setPosition(input.pos());

            Vec3 aimDirection = input.aim();
            projectile.shoot(aimDirection.x, aimDirection.y, aimDirection.z, itemStack, entity, spreadInstance);
            spreadInstance.updateSpread(itemStack, entity);
            RFEProjectileManager.queueAddedProjectile(projectile, entity.level());
        }

        this.fireFirearm(itemStack, entity, FiringType.NON_PLAYER_AND_EFFECTS);
        this.setJammed(itemStack, entity, jam);
    }

    public void playFiringEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.firingSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.firingSound, SoundSource.NEUTRAL, 1, 1);
    }

    public void onTickFiring(ItemStack itemStack, LivingEntity entity) {
        // TODO winding up and winding down
        CompoundTag tag = itemStack.getOrCreateTag();
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);

        if (this.canOverheat && FirearmDataUtils.isOverheated(this.getOrCreateModeTag(itemStack))) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
            FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            return;
        }
        if (this.fireMode == FireMode.SINGLE_ACTION
                && this.automaticSingleActionCycle(itemStack, entity, holdingKey)
                && this.canChargeInternal(itemStack, entity)) {
            this.onCharge(itemStack, entity);
            return;
        }
        if (this.fireMode.isSelfLoading()) {
            boolean isPlayer = entity instanceof Player;
            if (isPlayer && this.isJammed(itemStack) || !isPlayer && this.shouldJam(itemStack, entity)) {
                this.setJammed(itemStack, entity, true);
                return;
            } else {
                this.finishCharge(itemStack, entity);
            }
        }
        if (entity.level().isClientSide)
            return; // Handle automatic fire on the server only
        if (this.fireMode == FireMode.FULL_AUTO && this.canFireProjectile(itemStack, entity)) {
            if (!(itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKey) || holdAttackKey.isHoldingAttackKey(itemStack, entity))
                this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
            return;
        }
        if (this.fireMode == FireMode.BURST && this.canContinueBurstFire(itemStack, entity)) {
            this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
            this.decrementBurstFire(itemStack, entity);
        }
    }

    public boolean automaticSingleActionCycle(ItemStack itemStack, LivingEntity entity, boolean hold) {
        return this.getHandlingProperties(itemStack).chargingBehavior().canChargeAfterFiring(hold);
    }

    public boolean canContinueBurstFire(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        int burstFireCount = modeTag.getInt("BurstFireCount");
        if (burstFireCount <= 0 || !this.canFireProjectile(itemStack, entity)) {
            modeTag.remove("BurstFireCount");
            return false;
        } else {
            return true;
        }
    }

    public void tryStartBurstFire(ItemStack itemStack, LivingEntity entity) {
        if (!entity.level().isClientSide && this.fireMode == FireMode.BURST && this.burstRoundCount > 1 && !this.isBurstFiring(itemStack, entity))
            this.getOrCreateModeTag(itemStack).putInt("BurstFireCount", this.burstRoundCount - 1);
    }

    public void decrementBurstFire(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        int dec = modeTag.getInt("BurstFireCount") - 1;
        modeTag.putInt("BurstFireCount", dec);
    }

    public boolean isBurstFiring(ItemStack itemStack, LivingEntity entity) {
        if (this.burstRoundCount <= 1 || this.fireMode != FireMode.BURST)
            return false;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        return modeTag.getInt("BurstFireCount") > 0;
    }

    public void clearBurstFiring(ItemStack itemStack) {
        this.getOrCreateModeTag(itemStack).remove("BurstFireCount");
    }

    // TODO secondary ammo
    public boolean tryRunningReloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType,
                                          boolean onInput, boolean blockMagazineReload, boolean firstReload) {
        if (!this.ammoRequired)
            return false;
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (onInput && this.isBusyWithStagedAction(itemStack))
            return false;
        if (!this.reloadPhases.containsKey(phaseType))
            return false;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.reloadingCompareValues, itemStack, entity);
        for (ListIterator<ReloadPhase> lister = this.reloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (phaseType == ReloadPhase.PhaseType.RELOAD && phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES && blockMagazineReload)
                continue;
            if (phaseType == ReloadPhase.PhaseType.RELOAD && !firstReload && phase.firstReloadOnly())
                continue;
            if (!phase.test(compareContext))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.RELOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            modeTag.putString("ReloadPhase", phase.phaseType().getSerializedName());
            modeTag.putInt("ReloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    public void onTickReload(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (!this.ammoRequired) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }

        ReloadPhase.PhaseType phaseType = ReloadPhase.PhaseType.byId(modeTag.getString("ReloadPhase"));
        if (phaseType == null) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        List<ReloadPhase> phaseList = this.reloadPhases.get(phaseType);
        int phaseIndex = modeTag.contains("ReloadPhaseIndex", Tag.TAG_INT) ? modeTag.getInt("ReloadPhaseIndex") : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        ReloadPhase phase = phaseList.get(phaseIndex);

        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        phase.playEffects(itemStack, entity, phase.time() - actionTime);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.RELOAD)
            this.executeReloadPhase(itemStack, entity, phase, actionTime);

        if (actionTime > 0)
            return;
        if (phase.chargeFirearm())
            this.finishCharge(itemStack, entity);
        boolean blockMagazineReloads = phase.blockMagazineReloads() && phaseType == ReloadPhase.PhaseType.RELOAD;
        if (phaseType == ReloadPhase.PhaseType.PREPARE && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD, false, false, true)) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phase.endReload() || this.forceCancelReload(itemStack, entity)) {
            this.setForceCancelReload(itemStack, entity, false);
            if (!this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false, false, false))
                FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD, false, blockMagazineReloads, false)
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false, false, false)) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelReload(itemStack, modeTag);
    }

    // TODO secondary ammo
    public void executeReloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);

        int actualTime = phase.time() - actionTime;
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.unloadMagazineTime() == actualTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            if (phase.reloadMagazineTime() == actualTime) {
                Predicate<ItemStack> magPred = RFEUtils.orAllPredicates(ammoProperties.magazines());
                Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
                ItemStack foundMagazine = RFEItemUtils.findFullestMagazine(entity, magPred, ammoPred, true);
                if (!foundMagazine.isEmpty())
                    this.setMagazine(itemStack, entity, foundMagazine);
            }
            return;
        }
        int reloadCount = phase.reloadsAtTime(actualTime);
        if (reloadCount < 1)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        boolean addedLast = phase.ammoAddedLast();
        boolean replaceChamberedRound = phase.replaceChamberedRound() && !addedLast;
        List<ItemStack> ammoList;
        int capacity;
        ItemStack chambered = ItemStack.EMPTY;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            if (this.trackEmptySlots) {
                int diff = this.internalCapacity - RFEItemUtils.countItemsIncludingSlots(ammoList);
                for (int i = 0; i < diff; ++i)
                    ammoList.add(ItemStack.EMPTY);
            }
            capacity = this.internalCapacity;
            if (!phase.ammoAddedLast() && !replaceChamberedRound)
                chambered = FirearmDataUtils.stripAmmo(ammoList, false, false, this.trackEmptySlots);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
            capacity = magazineItem.getMagazineCapacity(magazine);
            if (this.plusOneCapacity && replaceChamberedRound) {
                ++capacity;
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    FirearmDataUtils.addAmmo(ammoList, loadedRound, false, this.trackEmptySlots, 0);
                    this.setLoadedRound(itemStack, ItemStack.EMPTY);
                }
            } else if (!this.plusOneCapacity && !replaceChamberedRound) {
                chambered = FirearmDataUtils.stripAmmo(ammoList, false, true, this.trackEmptySlots);
            }
        }
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        if (phase.reloadType() == ReloadPhase.ReloadType.ROUNDS) {
            int addable = Mth.clamp(capacity - RFEItemUtils.countItems(ammoList), 0, reloadCount);
            if (addable > 0) {
                List<ItemStack> foundAmmo = new LinkedList<>();
                RFEItemUtils.consumeItemsFromEntity(entity, ammoPred.and(s -> s != itemStack), s -> {
                    int takeAmount = Math.min(addable - RFEItemUtils.countItems(foundAmmo), s.getMaxStackSize());
                    ItemStack addition;
                    if (s.is(RFEItemTags.INFINITE_AMMO.tag)) {
                        addition = s.copyWithCount(takeAmount);
                    } else {
                        addition = s.split(takeAmount);
                    }
                    FirearmDataUtils.addAmmo(foundAmmo, addition, false, false);
                    return s.isEmpty() ? ItemStack.EMPTY : s;
                }, () -> foundAmmo.size() >= addable || !foundAmmo.isEmpty() && foundAmmo.get(foundAmmo.size() - 1).is(RFEItemTags.INFINITE_AMMO.tag));
                if (!foundAmmo.isEmpty()) {
                    for (ItemStack sourceStack : foundAmmo)
                        FirearmDataUtils.addAmmo(ammoList, sourceStack, addedLast, this.trackEmptySlots, 0);
                }
            }
        } else {
            Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(ammoProperties.speedloaders());
            int reloadCount1 = capacity - this.getLoadedAmmoCount(itemStack, entity, false);
            ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, true);
            if (bestSpeedloaderStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> strippedAmmo = magazineItem.getStoredAmmo(bestSpeedloaderStack);
                int consumed = FirearmDataUtils.addMultipleAmmo(ammoList, strippedAmmo, addedLast, false, this.trackEmptySlots, capacity);
                FirearmDataUtils.stripMultipleAmmo(strippedAmmo, consumed, false, false, false /* TODO track empty slots */, true);
                magazineItem.writeStoredAmmo(bestSpeedloaderStack, strippedAmmo);
                RFEItemUtils.addItemToEntity(bestSpeedloaderStack, entity);
            }
        }
        if (!chambered.isEmpty())
            FirearmDataUtils.addAmmo(ammoList, chambered, false, this.trackEmptySlots);
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(modeTag, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                magazineItem.writeStoredAmmo(magazine, ammoList);
            modeTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
        }
    }

    public boolean canCancelReloadByClick(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.getAction(itemStack) == RFEFirearmItem.Action.RELOAD
                && ReloadPhase.PhaseType.byId(this.getOrCreateModeTag(itemStack).getString("ReloadPhase")) == ReloadPhase.PhaseType.RELOAD;
    }

    public void setForceCancelReload(ItemStack itemStack, LivingEntity entity, boolean set) {
        if (set) {
            this.getOrCreateModeTag(itemStack).putBoolean("ForceEndReload", true);
        } else {
            this.getOrCreateModeTag(itemStack).remove("ForceEndReload");
        }
    }

    public boolean forceCancelReload(ItemStack itemStack, LivingEntity entity) {
        return this.getOrCreateModeTag(itemStack).contains("ForceEndReload");
    }

    public boolean tryRunningUnloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType, boolean onInput) {
        if (!this.ammoRequired)
            return false;
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (onInput && this.isBusyWithStagedAction(itemStack))
            return false;
        if (!this.unloadPhases.containsKey(phaseType))
            return false;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.unloadingCompareValues, itemStack, entity);
        for (ListIterator<ReloadPhase> lister = this.unloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (!phase.test(compareContext))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.UNLOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            modeTag.putString("UnloadPhase", phase.phaseType().getSerializedName());
            modeTag.putInt("UnloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    // TODO secondary ammo
    public void onTickUnload(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (!this.ammoRequired) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }

        ReloadPhase.PhaseType phaseType = ReloadPhase.PhaseType.byId(modeTag.getString("UnloadPhase"));
        if (phaseType == null) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        List<ReloadPhase> phaseList = this.unloadPhases.get(phaseType);
        int phaseIndex = modeTag.contains("UnloadPhaseIndex", Tag.TAG_INT) ? modeTag.getInt("UnloadPhaseIndex") : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        ReloadPhase phase = phaseList.get(phaseIndex);

        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        phase.playEffects(itemStack, entity, phase.time() - actionTime);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.RELOAD)
            this.executeUnloadPhase(itemStack, entity, phase, actionTime);

        if (actionTime > 0)
            return;
        if (phase.chargeFirearm())
            this.finishCharge(itemStack, entity);
        if (phaseType == ReloadPhase.PhaseType.PREPARE && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD, false)) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phase.endReload()) {
            if (!this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false))
                FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD, false)
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false)) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
    }

    // TODO secondary ammo
    public void executeUnloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        int actualTime = phase.time() - actionTime;
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.unloadMagazineTime() == actualTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            return;
        }
        int unloadCount = phase.reloadsAtTime(actualTime);
        if (unloadCount < 1)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        List<ItemStack> ammoList;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            if (this.trackEmptySlots) {
                int diff = this.internalCapacity - RFEItemUtils.countItemsIncludingSlots(ammoList);
                for (int i = 0; i < diff; ++i)
                    ammoList.add(ItemStack.EMPTY);
            }
        } else {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    RFEItemUtils.addItemToEntity(loadedRound, entity);
                    this.setLoadedRound(itemStack, ItemStack.EMPTY);
                    unloadCount -= 1;
                    if (unloadCount < 1)
                        return;
                }
            }
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
        }
        List<ItemStack> items = FirearmDataUtils.stripMultipleAmmo(ammoList, unloadCount, phase.ammoAddedLast(), false, this.trackEmptySlots, true);
        for (ItemStack item : items)
            RFEItemUtils.addItemToEntity(item, entity);
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(modeTag, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                magazineItem.writeStoredAmmo(magazine, ammoList);
            modeTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
        }
    }

    public ItemStack setMagazine(ItemStack itemStack, LivingEntity entity, ItemStack magazineStack) {
        if (this.internalCapacity > 0)
            return ItemStack.EMPTY;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        ItemStack previous = ItemStack.of(modeTag.getCompound("DetachedMagazine"));
        if (magazineStack.isEmpty()) {
            modeTag.remove("DetachedMagazine");
        } else {
            modeTag.put("DetachedMagazine", magazineStack.save(new CompoundTag()));
        }
        return previous;
    }

    public void setCharged(ItemStack itemStack, LivingEntity entity, boolean charged) {
        FirearmDataUtils.setCharged(this.getOrCreateModeTag(itemStack), charged);
    }

    public boolean canChargeInternal(ItemStack itemStack, LivingEntity entity) {
        return !this.isBusyWithStagedAction(itemStack) && FirearmDataUtils.getActionTime(itemStack) <= 0;
    }

    public void onCharge(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.chargingCompareValues, itemStack, entity);
        for (ChargeAction action : this.chargeActions) {
            if (action.tryExecute(itemStack, entity, compareContext))
                return;
        }
    }

    public void onTickCharging(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        if (!entity.level().isClientSide)
            this.finishCharge(itemStack, entity);
        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);
        if (!entity.level().isClientSide && this.fireMode == FireMode.SINGLE_ACTION && this.slamfire && holdingKey) {
            this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
        } else {
            FirearmDataUtils.setAction(itemStack, null);
        }
    }

    public void finishCharge(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        this.setCharged(itemStack, entity, true);
        this.setJammed(itemStack, entity, false);
        if (this.plusOneCapacity && this.getLoadedRound(itemStack).isEmpty()) {
            List<ItemStack> nextAmmoList = this.getNextRoundsInItem(itemStack, entity, 1, true);
            ItemStack nextAmmoStack = FirearmDataUtils.stripAmmo(nextAmmoList, this.ammoConsumedLast, false, this.trackEmptySlots);
            this.setLoadedRound(itemStack, nextAmmoStack);
        }
        if (this.canOverheat) {
            float heat = FirearmDataUtils.getHeat(modeTag);
            heat -= this.getHandlingProperties(itemStack).heatRemovedOnCharge();
            heat = Math.max(0, heat);
            FirearmDataUtils.setHeat(modeTag, heat);
        }
        // TODO cyclical charging; return item to bottom
        ItemStack chamberedRound = this.getChamberedRound(itemStack);
        if (!chamberedRound.isEmpty()) {
            boolean valid = false;
            RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
            for (AmmoPredicate pred : ammoProperties.primaryAmmo().keySet()) {
                if (pred.test(chamberedRound)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                this.setCharged(itemStack, entity, false);
                this.setJammed(itemStack, entity, true);
                if (this.plusOneCapacity) {
                    ItemStack loadedRound = this.getLoadedRound(itemStack);
                    if (!loadedRound.isEmpty()) {
                        this.setLoadedRound(itemStack, ItemStack.EMPTY);
                        RFEItemUtils.addItemToEntity(loadedRound, entity);
                    }
                } else if (this.internalCapacity > 0) {
                    List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
                    ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                    FirearmDataUtils.saveRounds(modeTag, "InternalRounds", ammoList);
                    if (!ejected.isEmpty())
                        RFEItemUtils.addItemToEntity(ejected, entity);
                } else {
                    CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
                    ItemStack magazine = ItemStack.of(magazineTag);
                    if (magazine.getItem() instanceof MagazineItem magazineItem) {
                        List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                        ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                        magazineItem.writeStoredAmmo(magazine, ammoList);
                        modeTag.put("DetachedMagazine", magazine.save(magazineTag));
                        if (!ejected.isEmpty())
                            RFEItemUtils.addItemToEntity(ejected, entity);
                    }
                }
            }
        }
    }

    public boolean shouldJam(ItemStack itemStack, LivingEntity entity) {
        return entity.getRandom().nextFloat() < this.getHandlingProperties(itemStack).jamChance();
    }

    public void setJammed(ItemStack itemStack, LivingEntity entity, boolean jammed) {
        FirearmDataUtils.setJammed(this.getOrCreateModeTag(itemStack), jammed);
    }

    public boolean isJammed(ItemStack itemStack) {
        return FirearmDataUtils.isJammed(this.getOrCreateModeTag(itemStack));
    }

    public void onTickDraw(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.drawTime)
            this.playDrawEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playDrawEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.drawSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.drawSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void onTickCooldown(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.cooldownTime)
            this.playCooldownEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playCooldownEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.cooldownSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.cooldownSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void startSwitchMode(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.SWITCH_MODE);
        FirearmDataUtils.setActionTime(itemStack, this.modeChangeTime);
    }

    public void playSwitchModeEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.modeChangeSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.modeChangeSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void onTickSwitchMode(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.modeChangeTime)
            this.playSwitchModeEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        if (itemStack.getItem() instanceof RFEFirearmItem firearmItem)
            firearmItem.completeSwitchMode(itemStack, entity);
        FirearmDataUtils.setAction(itemStack, null);
    }

    public boolean canAim(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        if (action != null && !action.canAim())
            return false;
        if (action == RFEFirearmItem.Action.SWITCH_MODE)
            ; // TODO do not allow switch mode if transition isn't "seamless" (e.g. safety toggle, mechanism switch)
        return true;
    }

    public void startAiming(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAiming(itemStack, true);
        int currentUnaimingTime = this.getAimingTime(itemStack, entity);
        float frac = this.unaimTime == 0 ? 0 : (float) currentUnaimingTime / (float) this.unaimTime;
        frac = 1f - frac;
        this.setAimingTime(itemStack, entity, Mth.ceil(this.aimTime * frac));
        if (this.aimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.aimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void stopAiming(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAiming(itemStack, false);
        int currentAimingTime = this.getAimingTime(itemStack, entity);;
        float frac = this.aimTime == 0 ? 0 : (float) currentAimingTime / (float) this.aimTime;
        frac = 1f - frac;
        this.setAimingTime(itemStack, entity, Mth.ceil(this.unaimTime * frac));
        entity.stopUsingItem();
        if (this.unaimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.unaimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public boolean isAiming(ItemStack itemStack, LivingEntity entity) {
        return entity instanceof Player ? entity.isUsingItem() : FirearmDataUtils.isAiming(itemStack);
    }

    public int getAimingTime(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.getAimingTime(itemStack);
    }

    public void setAimingTime(ItemStack itemStack, LivingEntity entity, int time) {
        FirearmDataUtils.setAimingTime(itemStack, time);
    }

    public int aimTime() { return this.aimTime; }

    public int unaimTime() { return this.unaimTime; }

    public void onTick(ItemStack itemStack, LivingEntity entity, boolean selected) {
        if (!selected) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.DRAW);
            FirearmDataUtils.setActionTime(itemStack, this.drawTime);
            this.clearBurstFiring(itemStack);
            this.setForceCancelReload(itemStack, entity, false);
            return;
        }
        CompoundTag tag = itemStack.getOrCreateTag();
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        RFEFirearmModeHandlingProperties properties = this.getHandlingProperties(itemStack);

        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);

        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);
        if (action != RFEFirearmItem.Action.FIRING)
            this.clearBurstFiring(itemStack);
        if (action != RFEFirearmItem.Action.RELOAD)
            this.setForceCancelReload(itemStack, entity, false);
        if (action != null) {
            switch (action) {
                case RELOAD -> this.onTickReload(itemStack, entity);
                case UNLOAD -> this.onTickUnload(itemStack, entity);
                case FIRING -> this.onTickFiring(itemStack, entity);
                case CHARGING -> this.onTickCharging(itemStack, entity);
                case DRAW -> this.onTickDraw(itemStack, entity);
                case SWITCH_MODE -> this.onTickSwitchMode(itemStack, entity);
                case COOLDOWN -> this.onTickCooldown(itemStack, entity);
            }
            RFEFirearmItem.Action endAction = FirearmDataUtils.getAction(itemStack);
            if (endAction == null)
                this.startIdleEffects(itemStack, entity);
        } else {
            if (FirearmDataUtils.isOverheated(modeTag)) {
                FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
                FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            } else if (this.fireMode == FireMode.SINGLE_ACTION
                    && this.automaticSingleActionCycle(itemStack, entity, holdingKey)
                    && this.canChargeInternal(itemStack, entity)) {
                this.onCharge(itemStack, entity);
            }
        }

        action = FirearmDataUtils.getAction(itemStack);
        if (action != RFEFirearmItem.Action.RELOAD) {
            modeTag.remove("ReloadPhase");
            modeTag.remove("ReloadPhaseIndex");
        }
        if (action != RFEFirearmItem.Action.UNLOAD) {
            modeTag.remove("UnloadPhase");
            modeTag.remove("UnloadPhaseIndex");
        }

        if (this.isAiming(itemStack, entity) && !this.canAim(itemStack, entity)) {
            this.stopAiming(itemStack, entity);
        }
        int aimingTime = this.getAimingTime(itemStack, entity);
        if (aimingTime > 0) {
            --aimingTime;
            this.setAimingTime(itemStack, entity, aimingTime);
        }

        if (!entity.level().isClientSide) {
            // TODO secondary ammo:
            //      TODO tick ammo slots
            //      TODO tick non-ammo slot

            if (this.canOverheat && !FirearmDataUtils.isOverheated(modeTag)) {
                int cooldownDelay = FirearmDataUtils.getCoolingDelay(modeTag);
                if (cooldownDelay > 0) {
                    --cooldownDelay;
                    FirearmDataUtils.setCoolingDelay(modeTag, cooldownDelay);
                } else {
                    float heat = FirearmDataUtils.getHeat(modeTag);
                    heat -= properties.heatRemovedPerTick();
                    heat = Math.max(0, heat);
                    FirearmDataUtils.setHeat(modeTag, heat);
                }
            }
            if (action != RFEFirearmItem.Action.FIRING)
                modeTag.remove("ExtraFiringTime");
        }
    }

    public void startIdleEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity) {
    }

    public int countFreeAmmoSpaces(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            int count = RFEItemUtils.countItems(ammoList);
            return this.nominalCapacity - count;
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                int capacity = magazineItem.getMagazineCapacity(magazine);
                List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                int count = RFEItemUtils.countItems(ammoList);
                return capacity - count;
            } else {
                return 0;
            }
        }
    }

    public int countExtraAmmoSpaces(ItemStack itemStack) {
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(this.getOrCreateModeTag(itemStack), "InternalRounds");
            int count = RFEItemUtils.countItems(ammoList);
            int extraSlots = this.internalCapacity - this.nominalCapacity;
            int extraAmmo = Math.max(0, count - this.nominalCapacity);
            return extraSlots - extraAmmo;
        } else {
            return 0; // Magazines have no extra slots
        }
    }

    public boolean hasAmmo(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            return !ammoList.isEmpty();
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                return !ammoList.isEmpty();
            } else {
                return false;
            }
        }
    }

    protected ItemStack getChamberedRound(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        ItemStack chamberedRound;
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        } else if (this.plusOneCapacity) {
            return this.getLoadedRound(itemStack);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return ItemStack.EMPTY;
            List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        }
        return chamberedRound;
    }

    public boolean hasChamberedRound(ItemStack itemStack) {
        if (!this.isCharged(itemStack))
            return false;
        if (!this.ammoRequired)
            return true;
        ItemStack chamberedRound = this.getChamberedRound(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        for (AmmoPredicate pred : ammoProperties.primaryAmmo().keySet()) {
            if (pred.test(chamberedRound))
                return true;
        }
        return false;
    }

    public boolean isCharged(ItemStack itemStack) {
        return FirearmDataUtils.isCharged(this.getOrCreateModeTag(itemStack));
    }

    public boolean hasMagazine(ItemStack itemStack) {
        if (this.internalCapacity > 0)
            return false;
        CompoundTag magazineTag = this.getOrCreateModeTag(itemStack).getCompound("DetachedMagazine");
        ItemStack magazine = ItemStack.of(magazineTag);
        return magazine.getItem() instanceof MagazineItem;
    }

    public boolean entityHasMagazine(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0)
            return false;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        ItemStack magazine = RFEItemUtils.findFullestMagazine(entity, RFEUtils.orAllPredicates(ammoProperties.magazines()),
                RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates()), false);
        return !magazine.isEmpty();
    }

    public int bestSpeedloaderAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(ammoProperties.speedloaders());
        int reloadCount1 = this.countFreeAmmoSpaces(itemStack);
        ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, false);
        return bestSpeedloaderStack.getItem() instanceof MagazineItem magazine ? magazine.countAmmo(bestSpeedloaderStack) : 0;
    }

    public boolean requiresAmmo() { return this.ammoRequired; }

    public enum FiringType {
        CLICK,
        AUTOMATIC,
        NON_PLAYER_AND_EFFECTS
    }
    
}
