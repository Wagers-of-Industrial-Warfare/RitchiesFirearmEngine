package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;

public interface RFERecoilInstance {

    RFEAimAngles getAimRecoil(float dt);
    RFEAimAngles getCameraRecoil(float dt);
    float getCameraRoll(float dt);

    RFERecoilClientImpulse updateRecoil(ItemStack itemStack, LivingEntity entity);
    void updateRecoilWithImpulse(ItemStack itemStack, LivingEntity entity, RFERecoilClientImpulse recoil);
    void tickRecoilBehavior();
    boolean isRemoved();

}
