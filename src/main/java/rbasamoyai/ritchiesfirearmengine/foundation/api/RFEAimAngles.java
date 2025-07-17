package rbasamoyai.ritchiesfirearmengine.foundation.api;

public record RFEAimAngles(float pitch, float yaw) {

    public static final RFEAimAngles ZERO_ANGLES = new RFEAimAngles(0f, 0f);

}
