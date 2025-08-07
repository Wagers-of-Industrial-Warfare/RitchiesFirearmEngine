package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;

public record RFERecoilClientImpulse(RFEAimAngles aimRecoil, RFEAimAngles cameraRecoil, float cameraRoll) {
}
