package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.gson.JsonObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmCondition;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;

public class ChargeAction {

    protected final FirearmCondition firearmCondition;
    protected final int time;
    @Nullable protected final SoundEvent sound;

    public ChargeAction(FirearmCondition firearmCondition, int time, SoundEvent sound) {
        this.firearmCondition = firearmCondition;
        this.time = time;
        this.sound = sound;
    }

    public boolean tryExecute(ItemStack itemStack, LivingEntity entity) {
        if (!this.firearmCondition.test(itemStack, entity))
            return false;
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.CHARGING);
        FirearmDataUtils.setActionTime(itemStack, this.time);
        this.playEffects(itemStack, entity);
        return true;
    }

    protected void playEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.sound != null)
            entity.level().playSound(null, entity.blockPosition(), this.sound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public static ChargeAction fromJson(JsonObject obj) {
        FirearmCondition condition = GsonHelper.isObjectNode(obj, "condition") ? FirearmCondition.fromJson(obj.getAsJsonObject("condition"))
                : FirearmCondition.AlwaysTrue.ALWAYS_TRUE;
        int time = GsonHelper.getAsInt(obj, "time");
        if (time < 1)
            throw new IllegalStateException("Charge action time cannot be less than 1");
        SoundEvent soundEvent = null;
        if (GsonHelper.isStringValue(obj, "sound")) {
            String str = GsonHelper.getAsString(obj, "sound");
            soundEvent = SoundEvent.createVariableRangeEvent(RFEUtils.location(str));
        }
        return new ChargeAction(condition, time, soundEvent);
    }

}
