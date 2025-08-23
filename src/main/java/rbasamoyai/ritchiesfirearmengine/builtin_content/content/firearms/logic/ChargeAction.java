package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmCondition;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;

public class ChargeAction {

    protected final FirearmCondition firearmCondition;
    protected final int time;
    protected final boolean ejectMagazine;
    protected final boolean cycleMagazine;
    protected final ImmutableMultimap<Integer, SoundEvent> soundTimeline;

    public ChargeAction(FirearmCondition firearmCondition, int time, boolean ejectMagazine, boolean cycleMagazine,
                        ImmutableMultimap<Integer, SoundEvent> soundTimeline) {
        this.firearmCondition = firearmCondition;
        this.time = time;
        this.ejectMagazine = ejectMagazine;
        this.cycleMagazine = cycleMagazine;
        this.soundTimeline = soundTimeline;
    }

    public boolean canExecute(ItemStack itemStack, LivingEntity entity, Map<ResourceLocation, Float> context) {
        return this.firearmCondition.test(context);
    }

    public void playEffects(ItemStack itemStack, LivingEntity entity, int actionTime) {
        for (SoundEvent event : this.soundTimeline.get(actionTime))
            entity.level().playSound(null, entity.blockPosition(), event, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void getCompareValueSources(Map<ResourceLocation, CompareValueSource> toEvaluate) {
        this.firearmCondition.getCompareValueSources(toEvaluate);
    }

    public int time() { return this.time; }

    public boolean ejectMagazine() { return this.ejectMagazine; }
    public boolean cycleMagazine() { return this.cycleMagazine; }

    public static ChargeAction fromJson(JsonObject obj) {
        FirearmCondition condition = GsonHelper.isObjectNode(obj, "condition") ? FirearmCondition.fromJson(obj.getAsJsonObject("condition"), true)
                : FirearmCondition.AlwaysTrue.ALWAYS_TRUE;
        int time = GsonHelper.getAsInt(obj, "time");
        if (time < 1)
            throw new IllegalStateException("Charge action time cannot be less than 1");
        ImmutableMultimap.Builder<Integer, SoundEvent> soundTimeline = ImmutableMultimap.builder();
        if (GsonHelper.isStringValue(obj, "sound")) {
            String str = GsonHelper.getAsString(obj, "sound");
            SoundEvent evt = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
            soundTimeline.put(0, evt);
        } else if (GsonHelper.isArrayNode(obj, "sounds")) {
            JsonArray sounds = GsonHelper.getAsJsonArray(obj, "sounds");
            for (JsonElement el : sounds) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Sound timeline object must be a JSON object");
                JsonObject soundObj = el.getAsJsonObject();
                int soundTime = GsonHelper.getAsInt(soundObj, "time");
                String str = GsonHelper.getAsString(soundObj, "sound");
                SoundEvent evt = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
                soundTimeline.put(soundTime, evt);
            }
        }
        boolean ejectMagazine = GsonHelper.getAsBoolean(obj, "eject_magazine", false);
        boolean cycleMagazine = GsonHelper.getAsBoolean(obj, "cycle_magazine", false);
        return new ChargeAction(condition, time, ejectMagazine, cycleMagazine, soundTimeline.build());
    }

}
