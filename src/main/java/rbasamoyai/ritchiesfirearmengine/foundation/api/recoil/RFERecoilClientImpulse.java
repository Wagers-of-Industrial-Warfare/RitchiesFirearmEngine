package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;

public record RFERecoilClientImpulse(RFEAimAngles aimRecoil, RFEAimAngles cameraRecoil, float cameraRoll) {

    public static final StreamCodec<ByteBuf, RFERecoilClientImpulse> STREAM_CODEC = StreamCodec.composite(
            RFEAimAngles.STREAM_CODEC, RFERecoilClientImpulse::aimRecoil,
            RFEAimAngles.STREAM_CODEC, RFERecoilClientImpulse::cameraRecoil,
            ByteBufCodecs.FLOAT, RFERecoilClientImpulse::cameraRoll,
            RFERecoilClientImpulse::new);

}
