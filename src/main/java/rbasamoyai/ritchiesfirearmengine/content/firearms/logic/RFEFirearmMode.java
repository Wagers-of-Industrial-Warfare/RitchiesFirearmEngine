package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Basic firearm mode class.
 */
public class RFEFirearmMode {

    protected final FirearmModeDataPackProperties defaultDataPackProperties;

    // Drawing
    protected final int drawTime;
    @Nullable protected final SoundEvent drawSound;

    // Mode change
    protected final int modeChangeTime;
    @Nullable protected final SoundEvent modeChangeSound;

    // Ammo
    protected final int internalCapacity;
    protected final int nominalCapacity;
    protected final boolean canLoadSingleRounds;
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

    // TODO validation for some required properties (e.g. requiredSecondaryAmmo)
    public RFEFirearmMode(RFEFirearmModeBuilder builder) {
        this.defaultDataPackProperties = new FirearmModeDataPackProperties(builder);

        this.drawTime = builder.drawTime;
        this.drawSound = builder.drawSound;

        this.modeChangeTime = builder.modeChangeTime;
        this.modeChangeSound = builder.modeChangeSound;

        this.internalCapacity = builder.internalCapacity;
        this.nominalCapacity = builder.nominalCapacity;
        this.canLoadSingleRounds = builder.canLoadSingleRounds;
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

    public int getNominalCapacity(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0)
            return this.nominalCapacity;
        ItemStack magazine = ItemStack.of(itemStack.getOrCreateTag().getCompound("DetachedMagazine"));
        return magazine.getItem() instanceof MagazineItem magazineItem ? magazineItem.getMagazineCapacity(magazine) : 0;
    }

    public int getLoadedAmmoCount(ItemStack itemStack, LivingEntity entity, boolean countPlusOne) {
        if (this.internalCapacity > 0)
            return RFEItemUtils.countItems(FirearmDataUtils.getRounds(itemStack, "InternalRounds"));
        int count = 0;
        CompoundTag tag = itemStack.getOrCreateTag();
        if (this.plusOneCapacity && countPlusOne) {
            ItemStack loadedRound = ItemStack.of(tag.getCompound("LoadedRound"));
            if (!loadedRound.isEmpty())
                ++count;
        }
        CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
        ItemStack magazine = ItemStack.of(magazineTag);
        if (magazine.getItem() instanceof MagazineItem magazineItem)
            count += RFEItemUtils.countItems(magazineItem.getStoredAmmo(magazine));
        return count;
    }

    public List<ItemStack> getNextRoundsInItem(ItemStack itemStack, LivingEntity entity, int count, boolean strip) {
        List<ItemStack> totalStripped = new LinkedList<>();
        CompoundTag tag = itemStack.getOrCreateTag();
        if (this.internalCapacity > 0) {
            List<ItemStack> list = FirearmDataUtils.getRounds(itemStack, "InternalRounds");
            List<ItemStack> internalStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast, !strip);
            count -= RFEItemUtils.countItems(internalStripped);
            totalStripped.addAll(internalStripped);
            if (strip)
                FirearmDataUtils.saveRounds(itemStack, "InternalRounds", list);
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
            CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> list = magazineItem.getStoredAmmo(magazine);
                List<ItemStack> magStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast, !strip);
                count -= RFEItemUtils.countItems(magStripped);
                totalStripped.addAll(magStripped);
                if (strip)
                    magazineItem.writeStoredAmmo(magazine, list);
            }
        }
        return totalStripped;
    }

    public ItemStack getLoadedRound(ItemStack itemStack, LivingEntity entity) {
        return ItemStack.of(itemStack.getOrCreateTag().getCompound("LoadedRound"));
    }

    public void setLoadedRound(ItemStack itemStack, LivingEntity entity, ItemStack roundStack) {
        if (roundStack.isEmpty()) {
            itemStack.getOrCreateTag().remove("LoadedRound");
        } else {
            itemStack.getOrCreateTag().put("LoadedRound", roundStack.save(new CompoundTag()));
        }
    }

    public boolean canFireProjectile(ItemStack itemStack, LivingEntity entity) {
        if (this.fireMode == FireMode.SAFETY || !FirearmDataUtils.isCharged(itemStack) || FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        // TODO check for secondary ammo
        return !this.isJammed(itemStack, entity) && !this.getNextRoundsInItem(itemStack, entity, this.ammoConsumed, false).isEmpty();
    }

    public void fireProjectile(ItemStack itemStack, LivingEntity entity) {
        FirearmModeDataPackProperties properties = this.getDataPackProperties();
        // TODO consume secondary ammo if required
        List<ItemStack> strippedAmmo = this.getNextRoundsInItem(itemStack, entity, this.ammoConsumed, true);
        for (ItemStack ammoStack : strippedAmmo) {
            int summons = ammoStack.getCount();
            RitchiesFirearmEngine.LOGGER.info("Bang!");
            // TODO actually spawn projectile
            // TODO effects
        }
        if (this.canOverheat) {
            FirearmDataUtils.addHeat(itemStack, properties.heatAddedOnFiring());
            FirearmDataUtils.setCoolingDelay(itemStack, properties.coolingDelayTime());
            if (FirearmDataUtils.getHeat(itemStack) > properties.heatCapacity())
                FirearmDataUtils.setOverheated(itemStack, true);
        }
        this.setCharged(itemStack, entity, false);
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.FIRING);
        if (this.firingCooldown > 0)
            FirearmDataUtils.setActionTime(itemStack, this.firingCooldown);
        if (this.fireMode == FireMode.SINGLE_ACTION && !properties.manualCharging())
            itemStack.getOrCreateTag().putBoolean("HoldAutomaticCycle", true);
    }

    public void onTickFiring(ItemStack itemStack, LivingEntity entity) {
        // TODO winding up and winding down
        CompoundTag tag = itemStack.getOrCreateTag();
        if (this.fireMode == FireMode.SINGLE_ACTION && tag.contains("HoldAutomaticCycle"))
            return;
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);

        if (this.canOverheat && FirearmDataUtils.isOverheated(itemStack)) {
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
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("BurstFireCount"))
            tag.putInt("BurstFireCount", this.burstRoundCount);
        int burstFireCount = tag.getInt("BurstFireCount") - 1;
        if (burstFireCount <= 0) {
            tag.remove("BurstFireCount");
            return false;
        } else {
            tag.putInt("BurstFireCount", burstFireCount);
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
            CompoundTag tag = itemStack.getOrCreateTag();
            phase.playEffects(itemStack, entity);
            tag.putString("ReloadPhase", phase.phaseType().getSerializedName());
            tag.putInt("ReloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    public void onTickReload(ItemStack itemStack, LivingEntity entity) {
        CompoundTag tag = itemStack.getOrCreateTag();
        ReloadPhase.PhaseType phaseType = ReloadPhase.PhaseType.byId(tag.getString("ReloadPhase"));
        if (phaseType == null) {
            FirearmDataUtils.cancelReload(itemStack);
            return;
        }
        List<ReloadPhase> phaseList = this.reloadPhases.get(phaseType);
        int phaseIndex = tag.contains("ReloadPhaseIndex", Tag.TAG_INT) ? tag.getInt("ReloadPhaseIndex") : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            FirearmDataUtils.cancelReload(itemStack);
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
            FirearmDataUtils.cancelReload(itemStack);
            return;
        }
        if (phase.endReload() && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelReload(itemStack);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)
            && !this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelReload(itemStack);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelReload(itemStack);
    }

    // TODO secondary ammo
    public void executeReloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        FirearmModeDataPackProperties properties = this.getDataPackProperties();
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.unloadMagazineTime() == actionTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            if (phase.reloadMagazineTime() == actionTime) {
                Predicate<ItemStack> magPred = RFEUtils.orAllPredicates(properties.magazineAmmoPredicates());
                Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(properties.primaryAmmoPredicates());
                RFEItemUtils.consumeItemsFromEntity(entity, s -> {
                    if (s == itemStack || !(s.getItem() instanceof MagazineItem magazineItem) || !magPred.test(s))
                        return false;
                    List<ItemStack> ammo = magazineItem.getStoredAmmo(s);
                    if (ammo.isEmpty())
                        return false;
                    for (ItemStack ammoStack : ammo) {
                        if (!ammoPred.test(ammoStack))
                            return false;
                    }
                    return true;
                }, s -> {
                    this.setMagazine(itemStack, entity, s);
                    return ItemStack.EMPTY;
                });
            }
            return;
        }
        int reloadCount = phase.reloadsAtTime(phase.time() - actionTime);
        if (reloadCount < 1)
            return;
        CompoundTag tag = itemStack.getOrCreateTag();
        List<ItemStack> ammoList;
        int capacity;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(itemStack, "InternalRounds");
            capacity = this.internalCapacity;
        } else if (this.canLoadSingleRounds) {
            CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
            capacity = magazineItem.getMagazineCapacity(magazine);
        } else {
            return;
        }
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(properties.primaryAmmoPredicates());
        if (phase.reloadType() == ReloadPhase.ReloadType.ROUNDS) {
            List<ItemStack> foundAmmo = RFEItemUtils.getItemsFromEntity(entity, ammoPred.and(s -> s != itemStack), reloadCount);
            FirearmDataUtils.addMultipleAmmo(ammoList, foundAmmo, phase.ammoAddedLast(), false, capacity);
        } else {
            Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(properties.speedloaderAmmoPredicates());
            Predicate<ItemStack> totalPredicate = s -> {
                if (s == itemStack || !(s.getItem() instanceof MagazineItem magazineItem) || !speedloaderPred.test(s))
                    return false;
                List<ItemStack> ammo = magazineItem.getStoredAmmo(s);
                if (ammo.isEmpty())
                    return false;
                for (ItemStack ammoStack : ammo) {
                    if (!ammoPred.test(ammoStack))
                        return false;
                }
                return true;
            };
            int reloadCount1 = capacity - this.getLoadedAmmoCount(itemStack, entity, false);
            ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, totalPredicate, reloadCount1);
            if (bestSpeedloaderStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> strippedAmmo = magazineItem.getStoredAmmo(bestSpeedloaderStack);
                FirearmDataUtils.addMultipleAmmo(ammoList, strippedAmmo, phase.ammoAddedLast(), false, capacity);
            }
        }
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(itemStack, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
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
            CompoundTag tag = itemStack.getOrCreateTag();
            phase.playEffects(itemStack, entity);
            tag.putString("UnloadPhase", phase.phaseType().getSerializedName());
            tag.putInt("UnloadPhaseIndex", index);
            return true;
        }
        return false;
    }

    // TODO secondary ammo
    public void onTickUnload(ItemStack itemStack, LivingEntity entity) {
        CompoundTag tag = itemStack.getOrCreateTag();
        ReloadPhase.PhaseType phaseType = ReloadPhase.PhaseType.byId(tag.getString("UnloadPhase"));
        if (phaseType == null) {
            FirearmDataUtils.cancelReload(itemStack);
            return;
        }
        List<ReloadPhase> phaseList = this.reloadPhases.get(phaseType);
        int phaseIndex = tag.contains("UnloadPhaseIndex", Tag.TAG_INT) ? tag.getInt("UnloadPhaseIndex") : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            FirearmDataUtils.cancelUnload(itemStack);
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
            FirearmDataUtils.cancelUnload(itemStack);
            return;
        }
        if (phase.endReload() && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelUnload(itemStack);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.RELOAD
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.RELOAD)
                && !this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH)) {
            FirearmDataUtils.cancelUnload(itemStack);
            return;
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH)
            FirearmDataUtils.cancelUnload(itemStack);
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
        CompoundTag tag = itemStack.getOrCreateTag();
        List<ItemStack> ammoList;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(itemStack, "InternalRounds");
        } else if (this.canLoadSingleRounds) {
            CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
        } else {
            return;
        }
        List<ItemStack> items = FirearmDataUtils.stripMultipleAmmo(ammoList, unloadCount, phase.ammoAddedLast(), false);
        for (ItemStack item : items)
            RFEItemUtils.addItemToEntity(item, entity);
        if (this.internalCapacity > 0) {
            FirearmDataUtils.saveRounds(itemStack, "InternalRounds", ammoList);
        } else {
            CompoundTag magazineTag = tag.getCompound("DetachedMagazine");
            ItemStack magazine = ItemStack.of(magazineTag);
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                magazineItem.writeStoredAmmo(magazine, ammoList);
            magazineTag.put("DetachedMagazine", magazine.save(new CompoundTag()));
        }
    }

    public ItemStack setMagazine(ItemStack itemStack, LivingEntity entity, ItemStack magazineStack) {
        if (this.internalCapacity > 0)
            return ItemStack.EMPTY;
        CompoundTag tag = itemStack.getOrCreateTag();
        ItemStack previous = ItemStack.of(tag.getCompound("DetachedMagazine"));
        if (magazineStack.isEmpty()) {
            tag.remove("DetachedMagazine");
        } else {
            tag.put("DetachedMagazine", magazineStack.save(new CompoundTag()));
        }
        return previous;
    }

    public void setCharged(ItemStack itemStack, LivingEntity entity, boolean charged) {
        FirearmDataUtils.setCharged(itemStack, charged);
    }

    public boolean canCharge(ItemStack itemStack, LivingEntity entity) {
        return this.fireMode != FireMode.SAFETY && FirearmDataUtils.getActionTime(itemStack) <= 0;
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
            float heat = FirearmDataUtils.getHeat(itemStack);
            heat -= this.getDataPackProperties().heatRemovedOnCharge();
            heat = Math.max(0, heat);
            FirearmDataUtils.setHeat(itemStack, heat);
        }
    }

    public boolean shouldJam(ItemStack itemStack, LivingEntity entity) {
        return entity.getRandom().nextFloat() < this.getDataPackProperties().jamChance();
    }

    public void setJammed(ItemStack itemStack, LivingEntity entity, boolean jammed) {
        if (jammed) {
            itemStack.getOrCreateTag().putBoolean("Jammed", true);
        } else {
            itemStack.getOrCreateTag().remove("Jammed");
        }
    }

    public boolean isJammed(ItemStack itemStack, LivingEntity entity) {
        return itemStack.getOrCreateTag().contains("Jammed");
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
            entity.level().playSound(null, entity.blockPosition(), this.drawSound, SoundSource.NEUTRAL, 0.25f, 1f);
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
            entity.level().playSound(null, entity.blockPosition(), this.cooldownSound, SoundSource.NEUTRAL, 0.25f, 1f);
    }

    public void startSwitchMode(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.SWITCH_MODE);
        FirearmDataUtils.setActionTime(itemStack, this.modeChangeTime);
    }

    public void playSwitchModeEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.modeChangeSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.modeChangeSound, SoundSource.NEUTRAL, 0.25f, 1f);
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

    public void onTick(ItemStack itemStack, LivingEntity entity, boolean selected) {
        if (!selected) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.DRAW);
            FirearmDataUtils.setActionTime(itemStack, this.drawTime);
            return;
        }
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
            if (FirearmDataUtils.isOverheated(itemStack)) {
                FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
                FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            }
        }
        FirearmModeDataPackProperties properties = this.getDataPackProperties();

        // TODO secondary ammo:
        //      TODO tick ammo slots
        //      TODO tick non-ammo slot

        if (this.canOverheat && !FirearmDataUtils.isOverheated(itemStack)) {
            int cooldownDelay = FirearmDataUtils.getCoolingDelay(itemStack);
            if (cooldownDelay > 0) {
                --cooldownDelay;
                FirearmDataUtils.setCoolingDelay(itemStack, cooldownDelay);
            } else {
                float heat = FirearmDataUtils.getHeat(itemStack);
                heat -= properties.heatRemovedPerTick();
                heat = Math.max(0, heat);
                FirearmDataUtils.setHeat(itemStack, heat);
            }
        }
    }

    public void startIdleEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        CompoundTag tag = itemStack.getOrCreateTag();
        if (action == RFEFirearmItem.Action.FIRING) {
            if (this.fireMode == FireMode.SINGLE_ACTION) {
                tag.remove("HoldAutomaticCycle");
            } else if (this.fireMode == FireMode.FULL_AUTO) {
                tag.putBoolean("StopAutoFire", true);
            }
        }
    }

}
