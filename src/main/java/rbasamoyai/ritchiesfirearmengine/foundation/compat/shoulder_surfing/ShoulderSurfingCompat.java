package rbasamoyai.ritchiesfirearmengine.foundation.compat.shoulder_surfing;

import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;

public class ShoulderSurfingCompat {

    public static boolean isShoulderSurfing() {
        return ShoulderSurfing.getInstance().isShoulderSurfing();
    }

    private ShoulderSurfingCompat() {}

}
