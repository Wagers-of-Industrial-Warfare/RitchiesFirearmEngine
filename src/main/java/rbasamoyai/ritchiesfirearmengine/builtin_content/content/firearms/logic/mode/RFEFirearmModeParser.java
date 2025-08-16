package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargeAction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FireMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;

public abstract class RFEFirearmModeParser<T extends RFEFirearmModeBuilder> {

    public abstract T getBuilder(String modeId);

    public T modifyBuilder(T builder, JsonObject obj, String modeId) {
        if (GsonHelper.isStringValue(obj, "mode_display_id")) {
            String modeDisplayId = GsonHelper.getAsString(obj, "mode_display_id");
            builder.modeDisplayId(modeDisplayId);
        }

        if (GsonHelper.isStringValue(obj, "mode_tag")) {
            String modeTag = GsonHelper.getAsString(obj, "mode_tag");
            builder.modeTag(modeTag);
        }

        boolean resetChargeOnUnequip = GsonHelper.getAsBoolean(obj, "reset_charge_on_unequip", builder.resetChargeOnUnequip);
        builder.resetChargeOnUnequip(resetChargeOnUnequip);

        if (GsonHelper.isObjectNode(obj, "drawing")) {
            JsonObject drawing = obj.getAsJsonObject("drawing");
            int drawTime = GsonHelper.getAsInt(drawing, "draw_time", 1);
            builder.drawTime(drawTime);
            this.drawingEffects(builder, drawing, modeId);
        }
        if (GsonHelper.isObjectNode(obj, "mode_change")) {
            JsonObject modeChange = obj.getAsJsonObject("mode_change");
            int modeChangeTime = GsonHelper.getAsInt(modeChange, "mode_change_time", 1);
            builder.modeChangeTime(modeChangeTime);
            this.modeChangeEffects(builder, modeChange, modeId);
        }
        if (GsonHelper.isObjectNode(obj, "aiming")) {
            JsonObject aiming = obj.getAsJsonObject("aiming");
            int aimTime = GsonHelper.getAsInt(aiming, "aim_time");
            int unaimTime = GsonHelper.getAsInt(aiming, "unaim_time", aimTime);
            builder.aimTime(aimTime)
                    .unaimTime(unaimTime);
            this.aimingEffects(builder, aiming, modeId);
        }

        if (GsonHelper.isObjectNode(obj, "ammo")) {
            JsonObject ammo = obj.getAsJsonObject("ammo");
            boolean ammoRequired = GsonHelper.getAsBoolean(ammo, "ammo_required", true);
            builder.ammoRequired(ammoRequired);
            if (ammoRequired) {
                int internalCapacity = GsonHelper.getAsInt(ammo, "internal_capacity", 0);
                if (internalCapacity > 0) {
                    builder.internalCapacity(internalCapacity);
                    int nominalCapacity = GsonHelper.getAsInt(ammo, "nominal_capacity", internalCapacity);
                    builder.nominalCapacity(nominalCapacity);
                } else {
                    boolean plusOneCapacity = GsonHelper.getAsBoolean(ammo, "plus_1_capacity", true);
                    builder.plusOneCapacity(plusOneCapacity);
                }
                boolean requiresSecondaryAmmo = GsonHelper.getAsBoolean(ammo, "requires_secondary_ammo", false);
                boolean trackEmptySlots = GsonHelper.getAsBoolean(ammo, "track_empty_slots", false);
                builder.requiresSecondaryAmmo(requiresSecondaryAmmo)
                        .trackEmptySlots(trackEmptySlots);
            }
        }

        if (GsonHelper.isObjectNode(obj, "firing")) {
            JsonObject firing = obj.getAsJsonObject("firing");
            builder.resetFiring();
            FireMode fireMode = FireMode.byId(GsonHelper.getAsString(firing, "fire_mode"));
            builder.fireMode(fireMode);
            if (fireMode != FireMode.SAFETY) {
                if (firing.has("rpm")) {
                    float rpm = GsonHelper.getAsFloat(firing, "rpm");
                    if (fireMode == FireMode.SINGLE_ACTION)
                        RitchiesFirearmEngine.LOGGER.warn("Should use cooldown for fire mode single_action");
                    builder.firingRPM(rpm);
                } else {
                    float cooldown = GsonHelper.getAsFloat(firing, "cooldown");
                    builder.firingCooldown(cooldown);
                }
                boolean ammoConsumedLast = GsonHelper.getAsBoolean(firing, "ammo_consumed_last", false);
                int shotsFired = GsonHelper.getAsInt(firing, "shots_fired", 1);
                builder.ammoConsumedLast(ammoConsumedLast)
                        .shotsFired(shotsFired);
                if (builder.trackEmptySlots) {
                    boolean ignoreEmptySlots = GsonHelper.getAsBoolean(firing, "ignore_empty_slots_when_firing", false);
                    builder.ignoreEmptySlotsWhenFiring(ignoreEmptySlots);
                }
                if (fireMode == FireMode.SINGLE_ACTION) {
                    ChargingBehavior chargingBehavior = ChargingBehavior.byIdStrict(GsonHelper.getAsString(firing, "charging_behavior",
                            ChargingBehavior.HOLD.getSerializedName()));
                    boolean slamfire = GsonHelper.getAsBoolean(firing, "slamfire", false);
                    builder.chargingBehavior(chargingBehavior)
                            .slamfire(slamfire);
                } else if (fireMode == FireMode.BURST) {
                    int burstRounds = GsonHelper.getAsInt(firing, "burst_rounds");
                    builder.burstRoundCount(burstRounds);
                }
                boolean canDryFire = GsonHelper.getAsBoolean(firing, "can_dry_fire", false);
                builder.canDryFire(canDryFire);
                if (canDryFire)
                    this.dryFiringEffects(builder, firing, modeId);
                if (GsonHelper.isObjectNode(firing, "misfire")) {
                    JsonObject misfire = firing.getAsJsonObject("misfire");
                    JsonObject misfireChances = GsonHelper.getAsJsonObject(misfire, "chances", new JsonObject());
                    for (Map.Entry<String, JsonElement> misfireEntry : misfireChances.entrySet()) {
                        ResourceLocation id = RFEUtils.location(misfireEntry.getKey());
                        builder.addMisfire(RFEContentBuilderRegistry.getMisfireProvider(id).apply(misfireEntry.getValue().getAsFloat()));
                    }
                    this.misfireEffects(builder, misfire, modeId);
                }
                if (GsonHelper.isObjectNode(firing, "wind_up")) {
                    JsonObject windUp = firing.getAsJsonObject("wind_up");
                    int time = GsonHelper.getAsInt(windUp, "time");
                    builder.windUpTime(time);
                    this.windUpEffects(builder, windUp, modeId);
                }
                if (GsonHelper.isObjectNode(firing, "wind_down")) {
                    JsonObject windDown = firing.getAsJsonObject("wind_down");
                    int time = GsonHelper.getAsInt(windDown, "time");
                    builder.windDownTime(time);
                    this.windDownEffects(builder, windDown, modeId);
                }
                this.firingEffects(builder, firing, modeId);
            }
        }

        if (GsonHelper.isObjectNode(obj, "reload")) {
            JsonObject reload = obj.getAsJsonObject("reload");
            if (GsonHelper.isArrayNode(reload, "phases")) {
                JsonArray reloadPhasesArr = GsonHelper.getAsJsonArray(reload, "phases");
                builder.resetReloadPhases();
                for (JsonElement reloadPhaseEl : reloadPhasesArr) {
                    if (!reloadPhaseEl.isJsonObject())
                        throw new JsonParseException("Invalid reload phase");
                    ReloadPhase reloadPhase = ReloadPhase.fromJson(reloadPhaseEl.getAsJsonObject(), false);
                    builder.addReloadPhase(reloadPhase);
                }
            }
        }

        if (GsonHelper.isObjectNode(obj, "unload")) {
            JsonObject unload = obj.getAsJsonObject("unload");
            if (GsonHelper.isArrayNode(unload, "phases")) {
                JsonArray unloadPhasesArr = GsonHelper.getAsJsonArray(unload, "phases");
                builder.resetUnloadPhases();
                for (JsonElement unloadPhaseEl : unloadPhasesArr) {
                    if (!unloadPhaseEl.isJsonObject())
                        throw new JsonParseException("Invalid unload phase");
                    ReloadPhase unloadPhase = ReloadPhase.fromJson(unloadPhaseEl.getAsJsonObject(), true);
                    builder.addUnloadPhase(unloadPhase);
                }
            }
        }

        if (GsonHelper.isObjectNode(obj, "charge")) {
            JsonObject charge = obj.getAsJsonObject("charge");
            if (GsonHelper.isArrayNode(charge, "actions")) {
                builder.resetChargeActions();
                JsonArray arr = charge.getAsJsonArray("actions");
                for (JsonElement chargeActionEl : arr) {
                    if (!chargeActionEl.isJsonObject())
                        throw new JsonParseException("Invalid charging action");
                    ChargeAction action = ChargeAction.fromJson(chargeActionEl.getAsJsonObject());
                    builder.addChargeAction(action);
                }
            }
        }

        if (GsonHelper.isObjectNode(obj, "overheating")) {
            JsonObject overheating = obj.getAsJsonObject("overheating");
            if (GsonHelper.getAsBoolean(overheating, "cant_overheat", true)) {
                builder.cantOverheat();
            } else {
                float heatCapacity = GsonHelper.getAsFloat(overheating, "heat_capacity");
                float heatRemovedPerTick = GsonHelper.getAsFloat(overheating, "heat_removed_per_tick");
                float heatRemovedOnCharge = GsonHelper.getAsFloat(overheating, "heat_removed_on_charge", 0);
                float heatAddedOnFiring = GsonHelper.getAsFloat(overheating, "heat_added_on_firing", 0);
                int coolingDelayTime = GsonHelper.getAsInt(overheating, "cooling_delay_time", 0);
                int cooldownTime = GsonHelper.getAsInt(overheating, "cooldown_time");
                builder.canOverheat()
                        .heatCapacity(heatCapacity)
                        .heatRemovedPerTick(heatRemovedPerTick)
                        .heatRemovedOnCharge(heatRemovedOnCharge)
                        .heatAddedOnFiring(heatAddedOnFiring)
                        .coolingDelay(coolingDelayTime)
                        .cooldownTime(cooldownTime);
                this.cooldownEffects(builder, overheating, modeId);
            }
        }

        return builder;
    }

    protected void drawingEffects(T builder, JsonObject drawingObj, String modeId) {
        if (GsonHelper.isStringValue(drawingObj, "sound")) {
            String str = GsonHelper.getAsString(drawingObj, "sound");
            SoundEvent drawSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.drawSound(drawSound);
        }
    }

    protected void modeChangeEffects(T builder, JsonObject modeChangeObj, String modeId) {
        if (GsonHelper.isStringValue(modeChangeObj, "sound")) {
            String str = GsonHelper.getAsString(modeChangeObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.modeChangeSound(modeChangeSound);
        }
    }

    protected void aimingEffects(T builder, JsonObject aimingObj, String modeId) {
        if (GsonHelper.isStringValue(aimingObj, "aim_sound")) {
            String str = GsonHelper.getAsString(aimingObj, "aim_sound");
            SoundEvent aimSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.aimSound(aimSound);
        }
        if (GsonHelper.isStringValue(aimingObj, "unaim_sound")) {
            String str = GsonHelper.getAsString(aimingObj, "unaim_sound");
            SoundEvent unaimSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.unaimSound(unaimSound);
        }
    }

    protected void windUpEffects(T builder, JsonObject windUpObj, String modeId) {
        if (GsonHelper.isStringValue(windUpObj, "sound")) {
            String str = GsonHelper.getAsString(windUpObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.windUpSound(modeChangeSound);
        }
    }

    protected void windDownEffects(T builder, JsonObject windDownObj, String modeId) {
        if (GsonHelper.isStringValue(windDownObj, "sound")) {
            String str = GsonHelper.getAsString(windDownObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.windDownSound(modeChangeSound);
        }
    }

    protected void firingEffects(T builder, JsonObject firingObj, String modeId) {
        if (GsonHelper.isStringValue(firingObj, "sound")) {
            String str = GsonHelper.getAsString(firingObj, "sound");
            SoundEvent firingSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.firingSound(firingSound);
        }
    }

    protected void dryFiringEffects(T builder, JsonObject firingObj, String modeId) {
        if (GsonHelper.isStringValue(firingObj, "dry_fire_sound")) {
            String str = GsonHelper.getAsString(firingObj, "dry_fire_sound");
            SoundEvent dryFireSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.dryFireSound(dryFireSound);
        }
    }

    protected void misfireEffects(T builder, JsonObject misfireObj, String modeId) {
        if (GsonHelper.isStringValue(misfireObj, "sound")) {
            String str = GsonHelper.getAsString(misfireObj, "sound");
            SoundEvent misfireSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.misfireSound(misfireSound);
        }
    }

    protected void cooldownEffects(T builder, JsonObject overheatingObj, String modeId) {
        if (GsonHelper.isStringValue(overheatingObj, "sound")) {
            String str = GsonHelper.getAsString(overheatingObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            builder.cooldownSound(modeChangeSound);
        }
    }

}
