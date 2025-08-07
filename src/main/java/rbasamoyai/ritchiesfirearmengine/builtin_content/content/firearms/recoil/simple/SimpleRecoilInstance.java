package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.simple;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMathUtils;

public class SimpleRecoilInstance implements RFERecoilInstance {

    private final float verticalRecoil;
    private final float horizontalRecoil;
    private final float shake;
    private final float recoilDecrease;

    private float pitch = 0;
    private float pitchO = 0;

    private float yaw = 0;
    private float yawO = 0;

    private float roll = 0;
    private float rollO = 0;

    int age = 0;

    public SimpleRecoilInstance(float verticalRecoil, float horizontalRecoil, float shake, float recoilDecrease) {
        this.verticalRecoil = verticalRecoil;
        this.horizontalRecoil = horizontalRecoil;
        this.shake = shake;
        this.recoilDecrease = recoilDecrease;
    }

    @Override
    public RFEAimAngles getAimRecoil(float dt) {
        if (this.age == -1) {
            this.age = 0;
            this.pitchO = this.pitch;
            this.yawO = this.yaw;
            this.rollO = this.roll;
            return new RFEAimAngles(this.pitch, this.yaw);
        }
        float pitchTurn = Mth.lerp(dt, 0, this.pitch - this.pitchO);
        float yawTurn = Mth.lerp(dt, 0, this.yaw - this.yawO);
        return new RFEAimAngles(pitchTurn, yawTurn);
    }

    @Override public RFEAimAngles getCameraRecoil(float dt) { return RFEAimAngles.ZERO_ANGLES; }

    @Override
    public float getCameraRoll(float dt) {
        return Mth.lerp(dt, this.rollO, this.roll);
    }

    @Override
    public RFERecoilClientImpulse updateRecoil(ItemStack itemStack, LivingEntity entity) {
        this.pitchO = 0;
        this.yawO = 0;
        this.rollO = this.roll;

        RandomSource random = entity.getRandom();

        this.pitch = this.verticalRecoil;
        this.yaw = this.horizontalRecoil * (random.nextFloat() - 0.5f) * 2f;

        this.roll = this.shake * (0.9f + 0.1f * random.nextFloat());
        if (random.nextBoolean())
            this.roll *= -1;

        this.age = -1;

        return new RFERecoilClientImpulse(new RFEAimAngles(this.pitch, this.yaw), RFEAimAngles.ZERO_ANGLES, this.roll);
    }

    @Override
    public void updateRecoilWithImpulse(ItemStack itemStack, LivingEntity entity, RFERecoilClientImpulse recoil) {
        this.pitchO = 0;
        this.yawO = 0;
        this.rollO = this.roll;

        this.pitch = recoil.aimRecoil().pitch();
        this.yaw = recoil.aimRecoil().yaw();
        this.roll = recoil.cameraRoll();
        this.age = -1;
    }

    @Override
    public void tickRecoilBehavior() {
        if (this.age == -1)
            return;
        this.pitchO = this.pitch;
        this.yawO = this.yaw;
        this.rollO = this.roll;

        this.pitch = RFEMathUtils.battlefieldRecoilDecrease(this.pitch, this.recoilDecrease, this.age);
        this.yaw = RFEMathUtils.battlefieldRecoilDecrease(this.yaw, this.recoilDecrease, this.age);
        this.roll = RFEMathUtils.battlefieldRecoilDecrease(this.roll, this.recoilDecrease, this.age);

        ++this.age;
    }

    @Override
    public boolean isRemoved() {
        return this.age >= 120;
    }

}
