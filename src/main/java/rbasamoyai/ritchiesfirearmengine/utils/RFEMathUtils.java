package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;

public class RFEMathUtils {

    /**
     * Taken from sym.gg
     *
     * @param current
     * @param decrease
     * @param time in ticks
     * @return
     */
    public static float battlefieldRecoilDecrease(float current, float decrease, float time) {
        current = innerBFRecoilDecrease(current, decrease, time);
        current = innerBFRecoilDecrease(current, decrease, time + 1/3f);
        return innerBFRecoilDecrease(current, decrease, time + 2/3f);
    }

    private static float innerBFRecoilDecrease(float current, float decrease, float time) {
        if (Math.abs(current) < 1e-2d)
            return 0;
        float timeSec = time / 20f;
        double recoilTerm = Math.pow(Mth.abs(current) * 2d, 0.6d) + 0.001d;
        double decrease1 = recoilTerm * decrease / 60f * Mth.sqrt(timeSec) * 5.0f;
        double newRecoil = current > 0 ? current - decrease1 : current + decrease1;
        if (Math.signum(current) != Math.signum(newRecoil))
            return 0;
        return Math.abs(newRecoil) > 1e-2d ? (float) newRecoil : 0f;
    }

    /**
     *
     * @param vec
     * @param fallbackPitch
     * @param fallbackYaw
     * @return the angles, in degrees
     */
    public static RFEAimAngles getAnglesFromVec(Vec3 vec, float fallbackPitch, float fallbackYaw) {
        double horiz = vec.horizontalDistance();
        if (horiz >= 1e-4d) {
            return new RFEAimAngles((float) -Math.atan(vec.y / horiz) * Mth.RAD_TO_DEG, (float) Math.atan2(-vec.x, vec.z) * Mth.RAD_TO_DEG);
        } else {
            return new RFEAimAngles(fallbackPitch, fallbackYaw);
        }
    }

    /**
     * Adapted from {@link net.minecraft.world.entity.Entity#calculateViewVector(float, float)}
     * @param pitch in degrees
     * @param yaw in degrees
     * @return the pointing vector
     */
    public static Vec3 calculateAimVector(float pitch, float yaw) {
        float f = pitch * Mth.DEG_TO_RAD;
        float f1 = -yaw * Mth.DEG_TO_RAD;
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    private RFEMathUtils() {}

}
