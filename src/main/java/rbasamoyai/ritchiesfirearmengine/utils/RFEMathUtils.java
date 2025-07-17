package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.util.Mth;

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

    private RFEMathUtils() {}

}
