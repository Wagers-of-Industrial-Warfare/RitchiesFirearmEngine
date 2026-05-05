package rbasamoyai.ritchiesfirearmengine.foundation.api;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RFEAimAngles(float pitch, float yaw) {

    public static final RFEAimAngles ZERO_ANGLES = new RFEAimAngles(0f, 0f);

    public static final StreamCodec<ByteBuf, RFEAimAngles> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RFEAimAngles::pitch, ByteBufCodecs.FLOAT, RFEAimAngles::yaw, RFEAimAngles::new);

}
