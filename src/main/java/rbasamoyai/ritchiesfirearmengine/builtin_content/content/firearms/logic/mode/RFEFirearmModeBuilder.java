package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargeAction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.CompareValueSource;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FireMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;

import javax.annotation.Nullable;
import java.util.*;

public class RFEFirearmModeBuilder {
    protected final String modeId;
    protected String modeDisplayId;
    protected String modeTagId = "MainMode";
    protected boolean resetChargeOnUnequip = false;
    protected float itemLength = 2;
    protected float movementSpeedModifier = 0; // Datapackable

    protected int drawTime = 1;
    @Nullable
    protected SoundEvent drawSound = null;

    protected int modeChangeTime = 1;
    @Nullable
    protected SoundEvent modeChangeSound = null;

    protected boolean canAim = true;
    protected int aimTime = -1;
    protected int unaimTime = -1;
    @Nullable
    protected SoundEvent aimSound = null;
    @Nullable
    protected SoundEvent unaimSound = null;

    protected boolean ammoRequired = true;
    protected int internalCapacity = 0;
    protected int nominalCapacity = 0;
    protected boolean explicitNominalCapacity = false;
    protected boolean plusOneCapacity = false;
    protected boolean requiresSecondaryAmmo = false;
    protected boolean trackEmptySlots = false;

    protected FireMode fireMode = null;
    protected ChargingBehavior chargingBehavior = ChargingBehavior.HOLD; // Datapackable
    protected float firingCooldown = -1;
    protected boolean ammoConsumedLast = false;
    protected boolean ignoreEmptySlotsWhenFiring = false;
    protected int shotsFired = 1;
    protected int burstRoundCount = 3;
    protected boolean slamfire = false;
    protected int slotsCycledAfterFiring = 0;
    protected boolean canDryFire = false;
    @Nullable
    protected SoundEvent firingSound = null;
    protected float firingSoundRange = 16;
    @Nullable
    protected SoundEvent dryFireSound = null;
    protected List<RFEMisfire> misfires = new ArrayList<>(); // Datapackable
    @Nullable
    protected SoundEvent misfireSound = null;
    protected int windUpTime = 0;
    protected boolean canInterruptWindUp = false;
    @Nullable
    protected SoundEvent windUpSound = null;
    protected int windDownTime = 0;
    @Nullable
    protected SoundEvent windDownSound = null;
    protected boolean ejectMagazineOnLastShot = false;
    @Nullable
    protected SoundEvent ejectMagazineOnLastShotSound = null;

    protected List<ReloadPhase> reloadPhases = new LinkedList<>();
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> finalReloadPhases = new EnumMap<>(ReloadPhase.PhaseType.class);
    protected Map<ResourceLocation, CompareValueSource> reloadingCompareValues = new Object2ReferenceOpenHashMap<>();

    protected List<ReloadPhase> unloadPhases = new LinkedList<>();
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> finalUnloadPhases = new EnumMap<>(ReloadPhase.PhaseType.class);
    protected Map<ResourceLocation, CompareValueSource> unloadingCompareValues = new Object2ReferenceOpenHashMap<>();

    protected List<ChargeAction> chargeActions = new LinkedList<>();
    protected Map<ResourceLocation, CompareValueSource> chargingCompareValues = new Object2ReferenceOpenHashMap<>();

    protected boolean canOverheat = false;
    protected int cooldownTime = -1;
    @Nullable
    protected SoundEvent cooldownSound = null;
    protected float heatCapacity = 0; // Datapackable
    protected float heatRemovedPerTick = 0; // Datapackable
    protected float heatRemovedOnCharge = 0; // Datapackable
    protected float heatAddedOnFiring = 0; // Datapackable
    protected int coolingDelayTime = 0; // Datapackable

    public RFEFirearmModeBuilder(String modeId) {
        this.modeId = modeId;
        this.modeDisplayId = this.modeId;
    }

    public RFEFirearmModeBuilder forkBuilder(String newModeId) {
        return this.forkBuilder(new RFEFirearmModeBuilder(newModeId));
    }

    public RFEFirearmModeBuilder forkBuilder(RFEFirearmModeBuilder newBuilder) {
        // Display id is NOT forked!
        newBuilder.modeTagId = this.modeTagId;

        newBuilder.drawTime = this.drawTime;
        newBuilder.drawSound = this.drawSound;
        newBuilder.resetChargeOnUnequip = this.resetChargeOnUnequip;
        newBuilder.itemLength = this.itemLength;
        newBuilder.movementSpeedModifier = this.movementSpeedModifier;

        newBuilder.modeChangeTime = this.modeChangeTime;
        newBuilder.modeChangeSound = this.modeChangeSound;

        newBuilder.canAim = this.canAim;
        newBuilder.aimTime = this.aimTime;
        newBuilder.unaimTime = this.unaimTime;
        newBuilder.aimSound = this.aimSound;
        newBuilder.unaimSound = this.unaimSound;

        newBuilder.ammoRequired = this.ammoRequired;
        newBuilder.internalCapacity = this.internalCapacity;
        newBuilder.nominalCapacity = this.nominalCapacity;
        newBuilder.explicitNominalCapacity = this.explicitNominalCapacity;
        newBuilder.plusOneCapacity = this.plusOneCapacity;
        newBuilder.requiresSecondaryAmmo = this.requiresSecondaryAmmo;
        newBuilder.trackEmptySlots = this.trackEmptySlots;

        newBuilder.fireMode = this.fireMode;
        newBuilder.chargingBehavior = this.chargingBehavior;
        newBuilder.firingCooldown = this.firingCooldown;
        newBuilder.ammoConsumedLast = this.ammoConsumedLast;
        newBuilder.ignoreEmptySlotsWhenFiring = this.ignoreEmptySlotsWhenFiring;
        newBuilder.shotsFired = this.shotsFired;
        newBuilder.burstRoundCount = this.burstRoundCount;
        newBuilder.slamfire = this.slamfire;
        newBuilder.slotsCycledAfterFiring = this.slotsCycledAfterFiring;
        newBuilder.canDryFire = this.canDryFire;
        newBuilder.firingSound = this.firingSound;
        newBuilder.firingSoundRange = this.firingSoundRange;
        newBuilder.dryFireSound = this.dryFireSound;
        newBuilder.misfires = new ArrayList<>(this.misfires);
        newBuilder.misfireSound = this.misfireSound;
        newBuilder.windUpTime = this.windUpTime;
        newBuilder.canInterruptWindUp = this.canInterruptWindUp;
        newBuilder.windUpSound = this.windUpSound;
        newBuilder.windDownTime = this.windDownTime;
        newBuilder.windDownSound = this.windDownSound;
        newBuilder.ejectMagazineOnLastShot = this.ejectMagazineOnLastShot;
        newBuilder.ejectMagazineOnLastShotSound = this.ejectMagazineOnLastShotSound;

        newBuilder.reloadPhases = new LinkedList<>(this.reloadPhases);
        newBuilder.unloadPhases = new LinkedList<>(this.unloadPhases);
        newBuilder.chargeActions = new LinkedList<>(this.chargeActions);

        newBuilder.reloadingCompareValues = new Object2ReferenceOpenHashMap<>(this.reloadingCompareValues);
        newBuilder.unloadingCompareValues = new Object2ReferenceOpenHashMap<>(this.unloadingCompareValues);
        newBuilder.chargingCompareValues = new Object2ReferenceOpenHashMap<>(this.chargingCompareValues);

        newBuilder.canOverheat = this.canOverheat;
        newBuilder.cooldownTime = this.cooldownTime;
        newBuilder.cooldownSound = this.cooldownSound;

        newBuilder.heatCapacity = this.heatCapacity;
        newBuilder.heatRemovedPerTick = this.heatRemovedPerTick;
        newBuilder.heatRemovedOnCharge = this.heatRemovedOnCharge;
        newBuilder.heatAddedOnFiring = this.heatAddedOnFiring;
        newBuilder.coolingDelayTime = this.coolingDelayTime;

        return newBuilder;
    }

    public RFEFirearmModeBuilder modeDisplayId(String modeDisplayId) {
        this.modeDisplayId = modeDisplayId;
        return this;
    }

    public RFEFirearmModeBuilder modeTag(String modeTag) {
        this.modeTagId = modeTag;
        return this;
    }

    public RFEFirearmModeBuilder resetChargeOnUnequip(boolean resetChargeOnUnequip) {
        this.resetChargeOnUnequip = resetChargeOnUnequip;
        return this;
    }

    public RFEFirearmModeBuilder itemLength(float itemLength) {
        this.itemLength = Mth.clamp(itemLength, 0, 100);
        return this;
    }

    public RFEFirearmModeBuilder movementSpeedMultiplier(float movementSpeedMultiplier) {
        if (movementSpeedMultiplier < -1 || 10 < movementSpeedMultiplier)
            throw new IllegalStateException("Movement speed multiplier must be in range [-1, 10] (was " + movementSpeedMultiplier + ")");
        this.movementSpeedModifier = movementSpeedMultiplier;
        return this;
    }

    public RFEFirearmModeBuilder drawTime(int drawTime) {
        if (drawTime < 1)
            throw new IllegalStateException("Cannot have draw time less than 1");
        this.drawTime = drawTime;
        return this;
    }

    public RFEFirearmModeBuilder drawSound(SoundEvent drawSound) {
        this.drawSound = drawSound;
        return this;
    }

    public RFEFirearmModeBuilder modeChangeTime(int modeChangeTime) {
        if (modeChangeTime < 1)
            throw new IllegalStateException("Cannot have mode change time less than 1");
        this.modeChangeTime = modeChangeTime;
        return this;
    }

    public RFEFirearmModeBuilder modeChangeSound(SoundEvent modeChangeSound) {
        this.modeChangeSound = modeChangeSound;
        return this;
    }

    public RFEFirearmModeBuilder canAim(boolean canAim) {
        this.canAim = canAim;
        return this;
    }

    public RFEFirearmModeBuilder aimTime(int aimTime) {
        this.aimTime = aimTime;
        return this;
    }

    public RFEFirearmModeBuilder unaimTime(int unaimTime) {
        this.unaimTime = unaimTime;
        return this;
    }

    public RFEFirearmModeBuilder aimSound(SoundEvent aimSound) {
        this.aimSound = aimSound;
        return this;
    }

    public RFEFirearmModeBuilder unaimSound(SoundEvent unaimSound) {
        this.unaimSound = unaimSound;
        return this;
    }

    private void resetInternalCapacityOptions() {
        this.internalCapacity = 0;
        this.nominalCapacity = 0;
    }

    private void resetMagazineOptions() {
        this.plusOneCapacity = false;
    }

    public RFEFirearmModeBuilder ammoRequired(boolean ammoRequired) {
        this.ammoRequired = ammoRequired;
        return this;
    }

    public RFEFirearmModeBuilder internalCapacity(int internalCapacity) {
        if (internalCapacity < 1)
            throw new IllegalStateException("Cannot specify internal capacity less than 1");
        this.internalCapacity = internalCapacity;
        this.nominalCapacity = this.internalCapacity;
        this.resetMagazineOptions();
        return this;
    }

    public RFEFirearmModeBuilder nominalCapacity(int nominalCapacity) {
        if (this.internalCapacity < 1)
            throw new IllegalStateException("Cannot specify nominal capacity without specifying internal capacity first");
        if (nominalCapacity < 1)
            throw new IllegalStateException("Cannot specify nominal capacity less than 1");
        this.nominalCapacity = nominalCapacity;
        this.resetMagazineOptions();
        return this;
    }

    public RFEFirearmModeBuilder plusOneCapacity(boolean plusOneCapacity) {
        if (this.internalCapacity > 0)
            throw new IllegalStateException("Can only specify +1 capacity for magazine firearms");
        this.plusOneCapacity = plusOneCapacity;
        this.resetInternalCapacityOptions();
        return this;
    }

    public RFEFirearmModeBuilder requiresSecondaryAmmo(boolean requiresSecondaryAmmo) {
        this.requiresSecondaryAmmo = requiresSecondaryAmmo;
        return this;
    }

    public RFEFirearmModeBuilder trackEmptySlots(boolean trackEmptySlots) {
        this.trackEmptySlots = trackEmptySlots;
        return this;
    }

    public RFEFirearmModeBuilder fireMode(FireMode fireMode) {
        this.fireMode = fireMode;
        return this;
    }

    public RFEFirearmModeBuilder chargingBehavior(ChargingBehavior chargingBehavior) {
        if (this.fireMode != FireMode.SINGLE_ACTION) {
            RitchiesFirearmEngine.LOGGER.warn("Charging behavior only applies to fire mode single_action, not {}", this.fireMode.getSerializedName());
            return this;
        }
        this.chargingBehavior = chargingBehavior;
        return this;
    }

    public RFEFirearmModeBuilder firingRPM(float rpm) { return this.firingCooldown(1200 / rpm); }

    public RFEFirearmModeBuilder firingCooldown(float firingCooldown) {
        if (firingCooldown < 0 || !Float.isFinite(firingCooldown))
            throw new IllegalStateException("Cannot specify firing cooldown less than 0 or non-finite");
        if (firingCooldown < 1) {
            RitchiesFirearmEngine.LOGGER.warn("Ritchie's Firearm Engine does not support RPM higher than 1200 (cooldown < 1), defaulting to cooldown = 1");
            firingCooldown = 1;
        }
        this.firingCooldown = firingCooldown;
        return this;
    }

    public RFEFirearmModeBuilder ammoConsumedLast(boolean ammoConsumedLast) {
        this.ammoConsumedLast = ammoConsumedLast;
        return this;
    }

    public RFEFirearmModeBuilder ignoreEmptySlotsWhenFiring(boolean ignoreEmptySlotsWhenFiring) {
        this.ignoreEmptySlotsWhenFiring = ignoreEmptySlotsWhenFiring;
        return this;
    }

    public RFEFirearmModeBuilder shotsFired(int shotsFired) {
        if (shotsFired < 1)
            throw new IllegalStateException("Cannot specify shots fired less than 1");
        this.shotsFired = shotsFired;
        return this;
    }

    public RFEFirearmModeBuilder burstRoundCount(int burstRoundCount) {
        if (burstRoundCount < 2)
            throw new IllegalStateException("Cannot specify burst round count less than 2");
        this.burstRoundCount = burstRoundCount;
        return this;
    }

    public RFEFirearmModeBuilder slamfire(boolean slamfire) {
        if (this.fireMode != FireMode.SINGLE_ACTION) {
            RitchiesFirearmEngine.LOGGER.warn("Slamfire only applies to fire mode single_action, not {}", this.fireMode.getSerializedName());
            return this;
        }
        this.slamfire = slamfire;
        return this;
    }

    public RFEFirearmModeBuilder slotsCycledAfterFiring(int slotsCycledAfterFiring) {
        this.slotsCycledAfterFiring = slotsCycledAfterFiring;
        return this;
    }

    public RFEFirearmModeBuilder canDryFire(boolean canDryFire) {
        this.canDryFire = canDryFire;
        return this;
    }

    public RFEFirearmModeBuilder firingSound(SoundEvent firingSound) {
        this.firingSound = firingSound;
        return this;
    }

    public RFEFirearmModeBuilder firingSoundRange(float firingSoundRange) {
        if (firingSoundRange < 0 || !Float.isFinite(firingSoundRange))
            throw new IllegalStateException("Firing sound range must be a positive, finite value");
        this.firingSoundRange = firingSoundRange;
        return this;
    }

    public RFEFirearmModeBuilder dryFireSound(SoundEvent dryFireSound) {
        if (!this.canDryFire)
            throw new IllegalStateException("Cannot specify dry fire sound if cannot dry fire");
        this.dryFireSound = dryFireSound;
        return this;
    }

    public RFEFirearmModeBuilder addMisfire(RFEMisfire misfire) {
        this.misfires.add(misfire);
        return this;
    }

    public RFEFirearmModeBuilder misfireSound(SoundEvent misfireSound) {
        this.misfireSound = misfireSound;
        return this;
    }

    public RFEFirearmModeBuilder windUpTime(int windUpTime) {
        if (windUpTime < 0)
            throw new IllegalStateException("Cannot specify wind-up time less than 0");
        this.windUpTime = windUpTime;
        return this;
    }

    public RFEFirearmModeBuilder canInterruptWindUp(boolean canInterruptWindUp) {
        this.canInterruptWindUp = canInterruptWindUp;
        return this;
    }

    public RFEFirearmModeBuilder windUpSound(SoundEvent windUpSound) {
        this.windUpSound = windUpSound;
        return this;
    }

    public RFEFirearmModeBuilder windDownTime(int windDownTime) {
        if (windDownTime < 0)
            throw new IllegalStateException("Cannot specify wind-down time less than 0");
        this.windDownTime = windDownTime;
        return this;
    }

    public RFEFirearmModeBuilder windDownSound(SoundEvent windDownSound) {
        this.windDownSound = windDownSound;
        return this;
    }

    public RFEFirearmModeBuilder ejectMagazineOnLastShot(boolean ejectMagazineOnLastShot) {
        this.ejectMagazineOnLastShot = ejectMagazineOnLastShot;
        return this;
    }

    public RFEFirearmModeBuilder ejectMagazineOnLastShotSound(SoundEvent ejectMagazineOnLastShotSound) {
        this.ejectMagazineOnLastShotSound = ejectMagazineOnLastShotSound;
        return this;
    }

    public RFEFirearmModeBuilder resetFiring() {
        this.fireMode = null;
        this.chargingBehavior = ChargingBehavior.HOLD;
        this.firingCooldown = -1;
        this.ammoConsumedLast = false;
        this.ignoreEmptySlotsWhenFiring = false;
        this.shotsFired = 1;
        this.burstRoundCount = 3;
        this.slamfire = false;
        this.slotsCycledAfterFiring = 0;
        this.firingSound = null;
        this.firingSoundRange = 16;
        this.canDryFire = false;
        this.dryFireSound = null;
        this.misfires.clear();
        this.misfireSound = null;
        this.windUpTime = 0;
        this.canInterruptWindUp = false;
        this.windUpSound = null;
        this.windDownTime = 0;
        this.windDownSound = null;
        this.ejectMagazineOnLastShot = false;
        this.ejectMagazineOnLastShotSound = null;
        return this;
    }

    public RFEFirearmModeBuilder addReloadPhase(ReloadPhase phase) {
        this.reloadPhases.add(phase);
        phase.getCompareValueSources(this.reloadingCompareValues);
        return this;
    }

    public RFEFirearmModeBuilder resetReloadPhases() {
        this.reloadPhases.clear();
        this.reloadingCompareValues.clear();
        return this;
    }

    public RFEFirearmModeBuilder addUnloadPhase(ReloadPhase phase) {
        this.unloadPhases.add(phase);
        phase.getCompareValueSources(this.unloadingCompareValues);
        return this;
    }

    public RFEFirearmModeBuilder resetUnloadPhases() {
        this.unloadPhases.clear();
        this.unloadingCompareValues.clear();
        return this;
    }

    public RFEFirearmModeBuilder addChargeAction(ChargeAction action) {
        this.chargeActions.add(action);
        action.getCompareValueSources(this.chargingCompareValues);
        return this;
    }

    public RFEFirearmModeBuilder resetChargeActions() {
        this.chargeActions.clear();
        this.chargingCompareValues.clear();
        return this;
    }

    public RFEFirearmModeBuilder cantOverheat() {
        this.canOverheat = false;
        this.heatCapacity = 0;
        this.cooldownTime = -1;
        this.heatRemovedPerTick = 0;
        this.heatRemovedOnCharge = 0;
        this.heatAddedOnFiring = 0;
        this.coolingDelayTime = 0;
        this.cooldownSound = null;
        return this;
    }

    public RFEFirearmModeBuilder canOverheat() {
        this.canOverheat = true;
        return this;
    }

    public RFEFirearmModeBuilder heatCapacity(float heatCapacity) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set default heat capacity outside of overheating module");
        if (heatCapacity < 0 || !Float.isFinite(heatCapacity))
            throw new IllegalStateException("Cannot specify heat capacity less than 0 or non-finite");
        this.heatCapacity = heatCapacity;
        return this;
    }

    public RFEFirearmModeBuilder cooldownTime(int cooldownTime) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set cooldown time outside of overheating module");
        if (cooldownTime < 0)
            throw new IllegalStateException("Cannot specify cooldown time less than 0");
        this.cooldownTime = cooldownTime;
        return this;
    }

    public RFEFirearmModeBuilder cooldownSound(SoundEvent cooldownSound) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set cooldown sound outside of overheating module");
        this.cooldownSound = cooldownSound;
        return this;
    }
    
    public RFEFirearmModeBuilder heatRemovedPerTick(float heatRemovedPerTick) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set default heat removed per tick outside of overheating module");
        if (heatRemovedPerTick < 0 || !Float.isFinite(heatRemovedPerTick))
            throw new IllegalStateException("Cannot specify default heat removed per tick less than 0 or non-finite");
        this.heatRemovedPerTick = heatRemovedPerTick;
        return this;
    }

    public RFEFirearmModeBuilder heatRemovedOnCharge(float heatRemovedOnCharge) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set default heat removed on charge outside of overheating module");
        if (heatRemovedOnCharge < 0 || !Float.isFinite(heatRemovedOnCharge))
            throw new IllegalStateException("Cannot specify default heat removed on charge less than 0 or non-finite");
        this.heatRemovedOnCharge = heatRemovedOnCharge;
        return this;
    }

    public RFEFirearmModeBuilder heatAddedOnFiring(float heatAddedOnFiring) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set default heat added on firing outside of overheating module");
        if (heatAddedOnFiring < 0 || !Float.isFinite(heatAddedOnFiring))
            throw new IllegalStateException("Cannot specify default heat added on firing less than 0 or non-finite");
        this.heatAddedOnFiring = heatAddedOnFiring;
        return this;
    }

    public RFEFirearmModeBuilder coolingDelay(int coolingDelayTime) {
        if (!this.canOverheat)
            throw new IllegalStateException("Internal error: cannot set default cooling delay outside of overheating module");
        if (coolingDelayTime < 0)
            throw new IllegalStateException("Cannot specify default cooling delay less than 0");
        this.coolingDelayTime = coolingDelayTime;
        return this;
    }

    public RFEFirearmMode build() {
        Objects.requireNonNull(this.fireMode, "Fire mode must be specified, must be one of 'safety', 'single_action', 'semi_auto', 'full_auto', or 'burst'");

        if (this.fireMode != FireMode.SAFETY && this.firingCooldown < 0)
            throw new IllegalStateException("Must specify firing cooldown");
        if (this.canOverheat && this.cooldownTime < 0)
            throw new IllegalStateException("Must specify cooldown time");
        if (this.aimTime < 0)
            throw new IllegalStateException("Must specify aiming time");
        if (this.unaimTime < 0)
            this.unaimTime = this.aimTime;

        if (!this.reloadPhases.isEmpty()) {
            Set<ReloadPhase.PhaseType> absentReloadPhaseTypes = EnumSet.allOf(ReloadPhase.PhaseType.class);
            absentReloadPhaseTypes.remove(ReloadPhase.PhaseType.UNLOAD);
            absentReloadPhaseTypes.remove(ReloadPhase.PhaseType.INDEX);
            for (ReloadPhase phase : this.reloadPhases) {
                this.finalReloadPhases.computeIfAbsent(phase.phaseType(), $ -> new LinkedList<>()).add(phase);
                absentReloadPhaseTypes.remove(phase.phaseType());
            }
            if (!absentReloadPhaseTypes.isEmpty())
                throw new IllegalStateException("Missing reload phases, must have all of 'prepare', 'reload', and 'finish' ('unload' optional)");
        }
        if (!this.unloadPhases.isEmpty()) {
            Set<ReloadPhase.PhaseType> absentUnloadPhaseTypes = EnumSet.allOf(ReloadPhase.PhaseType.class);
            absentUnloadPhaseTypes.remove(ReloadPhase.PhaseType.RELOAD);
            absentUnloadPhaseTypes.remove(ReloadPhase.PhaseType.INDEX);
            for (ReloadPhase phase : this.unloadPhases) {
                this.finalUnloadPhases.computeIfAbsent(phase.phaseType(), $ -> new LinkedList<>()).add(phase);
                absentUnloadPhaseTypes.remove(phase.phaseType());
            }
            if (!absentUnloadPhaseTypes.isEmpty())
                throw new IllegalStateException("Missing unload phases, must have all of 'prepare', 'unload', and 'finish'");
        }

        return new RFEFirearmMode(this, this.modeId);
    }

    public static class Parser extends RFEFirearmModeParser<RFEFirearmModeBuilder> {
        @Override
        public RFEFirearmModeBuilder getBuilder(String modeId) {
            return new RFEFirearmModeBuilder(modeId);
        }
    }

}
