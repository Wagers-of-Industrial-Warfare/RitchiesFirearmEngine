package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;

public abstract class RFEFirearmModeParser<T extends RFEFirearmModeBuilder> {

    protected abstract T getBuilder(String modeId);

    public RFEFirearmMode fromJson(JsonObject obj, String modeId) {
        T builder = this.getBuilder(modeId);

        if (GsonHelper.isStringValue(obj, "mode_tag")) {
            String modeTag = GsonHelper.getAsString(obj, "mode_tag");
            builder.modeTag(modeTag);
        }

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

        if (GsonHelper.isObjectNode(obj, "ammo")) {
            JsonObject ammo = obj.getAsJsonObject("ammo");
            if (GsonHelper.isArrayNode(ammo, "primary")) {
                builder.resetPrimaryAmmo();
                JsonArray arr = GsonHelper.getAsJsonArray(ammo, "primary");
                for (JsonElement primaryEl : arr) {
                    if (!GsonHelper.isStringValue(primaryEl))
                        throw new JsonParseException("Invalid primary ammo predicate");
                    AmmoPredicate primaryPred = AmmoPredicate.fromString(primaryEl.getAsString());
                    if (primaryPred == null)
                        throw new JsonParseException("Invalid primary ammo predicate");
                    builder.addPrimaryAmmo(primaryPred);
                }
            }
            if (GsonHelper.isArrayNode(ammo, "speedloaders")) {
                builder.resetSpeedloaders();
                JsonArray arr = GsonHelper.getAsJsonArray(ammo, "speedloaders");
                for (JsonElement speedloaderEl : arr) {
                    if (!GsonHelper.isStringValue(speedloaderEl))
                        throw new JsonParseException("Invalid speedloader ammo predicate");
                    AmmoPredicate speedloaderPred = AmmoPredicate.fromString(speedloaderEl.getAsString());
                    if (speedloaderPred == null)
                        throw new JsonParseException("Invalid speedloader ammo predicate");
                    builder.addSpeedloader(speedloaderPred);
                }
            }
            if (GsonHelper.isArrayNode(ammo, "secondary")) {
                builder.resetSecondaryAmmo();
                JsonArray arr = GsonHelper.getAsJsonArray(ammo, "secondary");
                for (JsonElement secondaryEl : arr) {
                    if (!GsonHelper.isStringValue(secondaryEl))
                        throw new JsonParseException("Invalid secondary ammo predicate");
                    AmmoPredicate secondaryPred = AmmoPredicate.fromString(secondaryEl.getAsString());
                    if (secondaryPred == null)
                        throw new JsonParseException("Invalid secondary ammo predicate");
                    builder.addSecondaryAmmo(secondaryPred);
                }
            }
            int internalCapacity = GsonHelper.getAsInt(ammo, "internal_capacity", 0);
            if (internalCapacity > 0) {
                builder.internalCapacity(internalCapacity);
                int nominalCapacity = GsonHelper.getAsInt(ammo, "nominal_capacity", internalCapacity);
                builder.nominalCapacity(nominalCapacity);
            } else {
                if (GsonHelper.isArrayNode(ammo, "magazines")) {
                    builder.resetMagazines();
                    JsonArray arr = GsonHelper.getAsJsonArray(ammo, "magazines");
                    for (JsonElement magazineEl : arr) {
                        if (!GsonHelper.isStringValue(magazineEl))
                            throw new JsonParseException("Invalid magazine ammo predicate");
                        AmmoPredicate magazinePred = AmmoPredicate.fromString(magazineEl.getAsString());
                        if (magazinePred == null)
                            throw new JsonParseException("Invalid magazine ammo predicate");
                        builder.addMagazine(magazinePred);
                    }
                    boolean canLoadSingleRounds = GsonHelper.getAsBoolean(ammo, "can_load_single_rounds");
                    boolean plusOneCapacity = GsonHelper.getAsBoolean(ammo, "plus_1_capacity", true);
                    builder.canLoadSingleRounds(canLoadSingleRounds)
                            .plusOneCapacity(plusOneCapacity);
                }
            }
        }

        if (GsonHelper.isObjectNode(obj, "firing")) {
            JsonObject firing = obj.getAsJsonObject("firing");
            builder.resetFiring();
            FireMode fireMode = FireMode.byId(GsonHelper.getAsString(firing, "fire_mode"));
            builder.fireMode(fireMode);
            if (fireMode != FireMode.SAFETY) {
                int cooldown = GsonHelper.getAsInt(firing, "cooldown");
                boolean ammoConsumedLast = GsonHelper.getAsBoolean(firing, "ammo_consumed_last", false);
                int ammoConsumed = GsonHelper.getAsInt(firing, "ammo_consumed", 1);
                float verticalRecoil = GsonHelper.getAsFloat(firing, "vertical_recoil", 0);
                float horizontalRecoil = GsonHelper.getAsFloat(firing, "horizontal_recoil", 0);
                float spread = GsonHelper.getAsFloat(firing, "spread", 0);
                float jamChance = GsonHelper.getAsFloat(firing, "jam_chance", 0);
                builder.firingCooldown(cooldown)
                        .ammoConsumedLast(ammoConsumedLast)
                        .ammoConsumed(ammoConsumed)
                        .verticalRecoil(verticalRecoil)
                        .horizontalRecoil(horizontalRecoil)
                        .spread(spread)
                        .jamChance(jamChance);
                if (fireMode == FireMode.SINGLE_ACTION) {
                    boolean manualCharge = GsonHelper.getAsBoolean(firing, "manual_charge", false);
                    builder.manualCharging(manualCharge);
                } else if (fireMode == FireMode.BURST) {
                    int burstRounds = GsonHelper.getAsInt(firing, "burst_rounds");
                    builder.burstRoundCount(burstRounds);
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

        return builder.build();
    }

    protected void drawingEffects(T builder, JsonObject drawingObj, String modeId) {
        if (GsonHelper.isStringValue(drawingObj, "draw_sound")) {
            String str = GsonHelper.getAsString(drawingObj, "draw_sound");
            SoundEvent drawSound = SoundEvent.createVariableRangeEvent(new ResourceLocation(str));
            builder.drawSound(drawSound);
        }
    }

    protected void modeChangeEffects(T builder, JsonObject modeChangeObj, String modeId) {
        if (GsonHelper.isStringValue(modeChangeObj, "mode_change_sound")) {
            String str = GsonHelper.getAsString(modeChangeObj, "mode_change_sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(new ResourceLocation(str));
            builder.modeChangeSound(modeChangeSound);
        }
    }

    protected void windUpEffects(T builder, JsonObject windUpObj, String modeId) {
        if (GsonHelper.isStringValue(windUpObj, "sound")) {
            String str = GsonHelper.getAsString(windUpObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(new ResourceLocation(str));
            builder.windUpSound(modeChangeSound);
        }
    }

    protected void windDownEffects(T builder, JsonObject windDownObj, String modeId) {
        if (GsonHelper.isStringValue(windDownObj, "sound")) {
            String str = GsonHelper.getAsString(windDownObj, "sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(new ResourceLocation(str));
            builder.windDownSound(modeChangeSound);
        }
    }

    protected void cooldownEffects(T builder, JsonObject overheatingObj, String modeId) {
        if (GsonHelper.isStringValue(overheatingObj, "cooldown_sound")) {
            String str = GsonHelper.getAsString(overheatingObj, "cooldown_sound");
            SoundEvent modeChangeSound = SoundEvent.createVariableRangeEvent(new ResourceLocation(str));
            builder.cooldownSound(modeChangeSound);
        }
    }

}
