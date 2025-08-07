package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;

public class NoRecoilInstance implements RFERecoilInstance {

    public static final NoRecoilInstance INSTANCE = new NoRecoilInstance();
    private static final RFERecoilClientImpulse NO_IMPULSE = new RFERecoilClientImpulse(RFEAimAngles.ZERO_ANGLES, RFEAimAngles.ZERO_ANGLES, 0);

    @Override public RFEAimAngles getAimRecoil(float dt) { return RFEAimAngles.ZERO_ANGLES; }
    @Override public RFEAimAngles getCameraRecoil(float dt) { return RFEAimAngles.ZERO_ANGLES; }
    @Override public float getCameraRoll(float dt) { return 0; }

    @Override public RFERecoilClientImpulse updateRecoil(ItemStack itemStack, LivingEntity entity) { return NO_IMPULSE; }
    @Override public void updateRecoilWithImpulse(ItemStack itemStack, LivingEntity entity, RFERecoilClientImpulse recoil) {}

    @Override public void tickRecoilBehavior() {}

    @Override public boolean isRemoved() { return true; }

}
