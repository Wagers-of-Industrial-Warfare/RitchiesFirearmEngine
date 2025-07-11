package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
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

    protected final FirearmModeDataPackProperties defaultDataPackProperties;

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
    protected final int internalCapacity;
    protected final int nominalCapacity;
    protected final boolean plusOneCapacity;
    protected final boolean requiresSecondaryAmmo;

    // Firing
    protected final FireMode fireMode;
    protected final int firingCooldown;
    protected final boolean ammoConsumedLast;
    protected final int ammoConsumed;
    protected final int burstRoundCount;
    @Nullable protected final SoundEvent firingSound;
    // Wind-up
    protected final int windUpTime;
    @Nullable protected final SoundEvent windUpSound;
    // Wind-down
    protected final int windDownTime;
    @Nullable protected final SoundEvent windDownSound;

    // Reloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> reloadPhases;
    // TODO secondary reload phases

    // Unloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> unloadPhases;
    // TODO secondary reload phases

    // Charging
    protected final List<ChargeAction> chargeActions;

    // Overheating
    protected final boolean canOverheat;
    protected final int cooldownTime;
    @Nullable protected final SoundEvent cooldownSound;

    public RFEFirearmMode(RFEFirearmModeBuilder builder, String modeId) {
        this.modeId = modeId;

        this.defaultDataPackProperties = new FirearmModeDataPackProperties(builder);

        this.modeTagId = builder.modeTagId;

        this.drawTime = builder.drawTime;
        this.drawSound = builder.drawSound;

        this.modeChangeTime = builder.modeChangeTime;
        this.modeChangeSound = builder.modeChangeSound;

        this.aimTime = builder.aimTime;
        this.unaimTime = builder.unaimTime;
        this.aimSound = builder.aimSound;
        this.unaimSound = builder.unaimSound;

        this.internalCapacity = builder.internalCapacity;
        this.nominalCapacity = builder.nominalCapacity;
        this.plusOneCapacity = builder.plusOneCapacity;
        this.requiresSecondaryAmmo = builder.requiresSecondaryAmmo;

        this.fireMode = builder.fireMode;
        this.firingCooldown = builder.firingCooldown;
        this.ammoConsumedLast = builder.ammoConsumedLast;
        this.ammoConsumed = builder.ammoConsumed;
        this.burstRoundCount = builder.burstRoundCount;
        this.firingSound = builder.firingSound;
        this.windUpTime = builder.windUpTime;
        this.windUpSound = builder.windUpSound;
        this.windDownTime = builder.windDownTime;
        this.windDownSound = builder.windDownSound;

        this.reloadPhases = builder.finalReloadPhases;
        this.unloadPhases = builder.finalUnloadPhases;
        this.chargeActions = builder.chargeActions;

        this.canOverheat = builder.canOverheat;
        this.cooldownTime = builder.cooldownTime;
        this.cooldownSound = builder.cooldownSound;
    }

    public FirearmModeDataPackProperties getDataPackProperties() {
        return this.defaultDataPackProperties; // TODO datapack
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
        ItemStack magazine = ItemStack.of(itemStack.getOrCreateTag().getCompound("DetachedMagazine"));
        return magazine.getItem() instanceof MagazineItem magazineItem ? magazineItem.getMagazineCapacity(magazine) : 0;
    }

    public List<ItemStack> getLoadedAmmo(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.internalCapacity > 0) {
            return FirearmDataUtils.getRounds(modeTag, "InternalRounds");
        } else {
            List<ItemStack> list = new ArrayList<>();
            if (this.plusOneCapacity) {
                ItemStack loadedRound = ItemStack.of(modeTag.getCompound("LoadedRound"));
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
        if (this.plusOneCapacity && countPlusOne) {
            ItemStack loadedRound = ItemStack.of(modeTag.getCompound("LoadedRound"));
            if (!loadedRound.isEmpty())
                ++count;
        }
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
            List<ItemStack> internalStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast, !strip);
            count -= RFEItemUtils.countItems(internalStripped);
            totalStripped.addAll(internalStripped);
            if (strip)
                FirearmDataUtils.saveRounds(modeTag, "InternalRounds", list);
        } else {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack, entity);
                if (!loadedRound.isEmpty()) {
                    --count;
                    totalStripped.add(loadedRound.split(1));
                    if (strip)
                        this.setLoadedRound(itemStack, entity, ItemStack.EMPTY);
                }
            }
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> list = magazineItem.getStoredAmmo(magazine);
                List<ItemStack> magStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast, !strip);
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

    public ItemStack getLoadedRound(ItemStack itemStack, LivingEntity entity) {
        return ItemStack.of(this.getOrCreateModeTag(itemStack).getCompound("LoadedRound"));
    }

    public void setLoadedRound(ItemStack itemStack, LivingEntity entity, ItemStack roundStack) {
        if (roundStack.isEmpty()) {
            this.getOrCreateModeTag(itemStack).remove("LoadedRound");
        } else {
            this.getOrCreateModeTag(itemStack).put("LoadedRound", roundStack.save(new CompoundTag()));
        }
    }

    public boolean canFireProjectile(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (this.fireMode == FireMode.SAFETY || !FirearmDataUtils.isCharged(modeTag) || FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        // TODO check for secondary ammo
        return !this.isJammed(itemStack) && !this.getNextRoundsInItem(itemStack, entity, this.ammoConsumed, false).isEmpty();
    }

    public void fireProjectile(ItemStack itemStack, LivingEntity entity) {
        // TODO windup
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        FirearmModeDataPackProperties firearmProperties = this.getDataPackProperties();
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);

        Vec3 upDirection = entity.getUpVector(1f);
        Vec3 aimDirection = entity.getViewVector(1f);

        if (this.ammoConsumed > 0) {
            List<ItemStack> strippedAmmo = this.getNextRoundsInItem(itemStack, entity, this.ammoConsumed, true);
            // TODO consume secondary ammo if required
            for (ItemStack ammoStack : strippedAmmo) {
                for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : ammoProperties.primaryAmmo().entrySet()) {
                    if (!entry.getKey().test(ammoStack))
                        continue;
                    RFEProjectileInstance instance = entry.getValue().createInstance();
                    // TODO shooter positioning
                    instance.setOwner(entity);
                    instance.setPosition(new Vec3(entity.getX(), entity.getEyeY(), entity.getZ()));
                    instance.shoot(aimDirection.x, aimDirection.y, aimDirection.z);
                    RFEProjectileManager.queueAddedProjectile(instance, entity.level());
                    break;
                }
            }
        } else {
            // TODO spawn anyway if ammo not consumed, usable for infinity guns/blasters
            // TODO Figure out ammo type
        }
        this.playFiringEffects(itemStack, entity);
        if (this.canOverheat) {
            FirearmDataUtils.addHeat(modeTag, firearmProperties.heatAddedOnFiring());
            FirearmDataUtils.setCoolingDelay(modeTag, firearmProperties.coolingDelayTime());
            if (FirearmDataUtils.getHeat(modeTag) > firearmProperties.heatCapacity())
                FirearmDataUtils.setOverheated(modeTag, true);
        }
        this.setCharged(itemStack, entity, false);
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.FIRING);
        if (this.firingCooldown > 0)
            FirearmDataUtils.setActionTime(itemStack, this.firingCooldown);
        if (this.fireMode == FireMode.SINGLE_ACTION && !firearmProperties.manualCharging())
            itemStack.getOrCreateTag().putBoolean("HoldAutomaticCycle", true);
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
        if (this.fireMode == FireMode.SINGLE_ACTION && tag.contains("HoldAutomaticCycle"))
            return;

        if (this.canOverheat && FirearmDataUtils.isOverheated(this.getOrCreateModeTag(itemStack))) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
            FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            return;
        }
        if (this.fireMode == FireMode.SINGLE_ACTION
                && this.automaticSingleActionCycle(itemStack, entity)
                && this.canCharge(itemStack, entity)) {
            this.onCharge(itemStack, entity);
            return;
        }
        if (this.fireMode.isSelfLoading()) {
            if (this.shouldJam(itemStack, entity)) {
                this.setJammed(itemStack, entity, true);
                return;
            } else {
                this.finishCharge(itemStack, entity);
            }
        }
        if (this.fireMode == FireMode.FULL_AUTO && this.canFireProjectile(itemStack, entity)) {
            if (tag.contains("StopAutoFire")) {
                tag.remove("StopAutoFire");
            } else {
                this.fireProjectile(itemStack, entity);
            }
            return;
        }
        if (this.fireMode == FireMode.BURST && this.canBurstFire(itemStack, entity) && this.canFireProjectile(itemStack, entity)) {
            this.fireProjectile(itemStack, entity);
        }
    }

    public boolean automaticSingleActionCycle(ItemStack itemStack, LivingEntity entity) {
        return this.getDataPackProperties().manualCharging();
    }

    public boolean canBurstFire(ItemStack itemStack, LivingEntity entity) {
        if (this.burstRoundCount <= 1)
            return false;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (!modeTag.contains("BurstFireCount"))
            modeTag.putInt("BurstFireCount", this.burstRoundCount);
        int burstFireCount = modeTag.getInt("BurstFireCount") - 1;
        if (burstFireCount <= 0) {
            modeTag.remove("BurstFireCount");
            return false;
        } else {
            modeTag.putInt("BurstFireCount", burstFireCount);
            return true;
        }
    }

    // TODO secondary ammo
    public boolean tryRunningReloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType) {
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        for (ListIterator<ReloadPhase> lister = this.reloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (!phase.test(itemStack, entity))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.RELOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
            phase.playEffects(itemStack, entity);
            modeTag.putString("ReloadPhase", phase.phaseType().getSerializedName());
            modeTag.putInt("ReloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    public void onTickReload(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
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
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.RELOAD)
            this.executeReloadPhase(itemStack, entity, phase, actionTime);

        if (actionTime > 0)
            return;
        if (phase.chargeFirearm())
            this.finishCharge(itemStack, entity);
        if (phaseType == ReloadPhase.PhaseType.PREPARE && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phase.endReload() && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelReload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelReload(itemStack, modeTag);
    }

    // TODO secondary ammo
    public void executeReloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);

        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.unloadMagazineTime() == actionTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            if (phase.reloadMagazineTime() == actionTime) {
                Predicate<ItemStack> magPred = RFEUtils.orAllPredicates(ammoProperties.magazines());
                Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
                ItemStack foundMagazine = RFEItemUtils.findFullestMagazine(entity, magPred, ammoPred, true);
                if (!foundMagazine.isEmpty())
                    this.setMagazine(itemStack, entity, foundMagazine);
            }
            return;
        }
        int reloadCount = phase.reloadsAtTime(phase.time() - actionTime);
        if (reloadCount < 1)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        List<ItemStack> ammoList;
        int capacity;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
            capacity = this.internalCapacity;
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
            capacity = magazineItem.getMagazineCapacity(magazine);
        }
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        if (phase.reloadType() == ReloadPhase.ReloadType.ROUNDS) {
            List<ItemStack> foundAmmo = RFEItemUtils.getItemsFromEntity(entity, ammoPred.and(s -> s != itemStack), reloadCount, true);
            FirearmDataUtils.addMultipleAmmo(ammoList, foundAmmo, phase.ammoAddedLast(), false, capacity);
        } else {
            Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(ammoProperties.speedloaders());
            int reloadCount1 = capacity - this.getLoadedAmmoCount(itemStack, entity, false);
            ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, true);
            if (bestSpeedloaderStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> strippedAmmo = magazineItem.getStoredAmmo(bestSpeedloaderStack);
                FirearmDataUtils.addMultipleAmmo(ammoList, strippedAmmo, phase.ammoAddedLast(), false, capacity);
            }
        }
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(modeTag, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                magazineItem.writeStoredAmmo(magazine, ammoList);
            magazineTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
        }
    }

    public boolean tryRunningUnloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType) {
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        for (ListIterator<ReloadPhase> lister = this.unloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (!phase.test(itemStack, entity))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.UNLOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
            phase.playEffects(itemStack, entity);
            modeTag.putString("UnloadPhase", phase.phaseType().getSerializedName());
            modeTag.putInt("UnloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    // TODO secondary ammo
    public void onTickUnload(ItemStack itemStack, LivingEntity entity) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
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
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.RELOAD)
            this.executeUnloadPhase(itemStack, entity, phase, actionTime);

        if (actionTime > 0)
            return;
        if (phaseType == ReloadPhase.PhaseType.PREPARE && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phase.endReload() && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelUnload(itemStack, modeTag);
    }

    // TODO secondary ammo
    public void executeUnloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.unloadMagazineTime() == actionTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            return;
        }
        int unloadCount = phase.reloadsAtTime(phase.time() - actionTime);
        if (unloadCount < 1)
            return;
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        List<ItemStack> ammoList;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeTag, "InternalRounds");
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
        }
        List<ItemStack> items = FirearmDataUtils.stripMultipleAmmo(ammoList, unloadCount, phase.ammoAddedLast(), false);
        for (ItemStack item : items)
            RFEItemUtils.addItemToEntity(item, entity);
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(modeTag, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = modeTag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                magazineItem.writeStoredAmmo(magazine, ammoList);
            magazineTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
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

    public boolean canCharge(ItemStack itemStack, LivingEntity entity) {
        return this.fireMode != FireMode.SAFETY && FirearmDataUtils.getActionTime(itemStack) <= 0 && !this.isCharged(itemStack);
    }

    public void onCharge(ItemStack itemStack, LivingEntity entity) {
        for (ChargeAction action : this.chargeActions) {
            if (action.tryExecute(itemStack, entity))
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
        this.finishCharge(itemStack, entity);
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void finishCharge(ItemStack itemStack, LivingEntity entity) {
        if (this.plusOneCapacity && this.getLoadedRound(itemStack, entity).isEmpty()) {
            List<ItemStack> nextAmmoList = this.getNextRoundsInItem(itemStack, entity, 1, true);
            ItemStack nextAmmoStack = FirearmDataUtils.stripFirstAmmo(nextAmmoList, false);
            this.setLoadedRound(itemStack, entity, nextAmmoStack);
        }
        this.setCharged(itemStack, entity, true);
        this.setJammed(itemStack, entity, false);
        if (this.canOverheat) {
            CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
            float heat = FirearmDataUtils.getHeat(modeTag);
            heat -= this.getDataPackProperties().heatRemovedOnCharge();
            heat = Math.max(0, heat);
            FirearmDataUtils.setHeat(modeTag, heat);
        }
    }

    public boolean shouldJam(ItemStack itemStack, LivingEntity entity) {
        return entity.getRandom().nextFloat() < this.getDataPackProperties().jamChance();
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
        int currentUnaimingTime = FirearmDataUtils.getAimingTime(itemStack);
        float frac = this.unaimTime == 0 ? 0 : (float) currentUnaimingTime / (float) this.unaimTime;
        frac = 1f - frac;
        FirearmDataUtils.setAimingTime(itemStack, Mth.ceil(this.aimTime * frac));
        if (this.aimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.aimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void stopAiming(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAiming(itemStack, false);
        int currentAimingTime = FirearmDataUtils.getAimingTime(itemStack);
        float frac = this.aimTime == 0 ? 0 : (float) currentAimingTime / (float) this.aimTime;
        frac = 1f - frac;
        FirearmDataUtils.setAimingTime(itemStack, Mth.ceil(this.unaimTime * frac));
        entity.stopUsingItem();
        if (this.unaimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.unaimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public boolean isAiming(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.isAiming(itemStack);
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
            return;
        }
        CompoundTag tag = itemStack.getOrCreateTag();
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        FirearmModeDataPackProperties properties = this.getDataPackProperties();

        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
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
            } else if (this.fireMode == FireMode.SINGLE_ACTION && !properties.manualCharging()
                    && !tag.contains("HoldAutomaticCycle") && this.canCharge(itemStack, entity)) {
                this.onCharge(itemStack, entity);
            }
        }

        if (this.isAiming(itemStack, entity) && !this.canAim(itemStack, entity)) {
            this.stopAiming(itemStack, entity);
        }
        int aimingTime = this.getAimingTime(itemStack, entity);
        if (aimingTime > 0) {
            --aimingTime;
            this.setAimingTime(itemStack, entity, aimingTime);
        }

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
    }

    public void startIdleEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        CompoundTag tag = itemStack.getOrCreateTag();
        if (this.fireMode == FireMode.SINGLE_ACTION) {
            tag.remove("HoldAutomaticCycle");
        } else if (this.fireMode == FireMode.FULL_AUTO) {
            tag.putBoolean("StopAutoFire", true);
        }
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

    public boolean hasChamberedRound(ItemStack itemStack) {
        CompoundTag modeTag = this.getOrCreateModeTag(itemStack);
        if (!this.isCharged(itemStack))
            return false;
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
        int reloadCount1 = this.getNominalCapacity(itemStack, entity);
        ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, false);
        return bestSpeedloaderStack.getItem() instanceof MagazineItem magazine ? magazine.countAmmo(bestSpeedloaderStack) : 0;
    }
    
}
