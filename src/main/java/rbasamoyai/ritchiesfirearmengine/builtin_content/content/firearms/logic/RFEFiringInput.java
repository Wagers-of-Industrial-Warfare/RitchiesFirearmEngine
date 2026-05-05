package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

public record RFEFiringInput(ResourceLocation projectile, Vec3 aim, Vec3 pos) {

    public static final StreamCodec<FriendlyByteBuf, RFEFiringInput> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RFEFiringInput::projectile,
            RFEByteBufCodecUtils.VEC3_STREAM_CODEC, RFEFiringInput::aim,
            RFEByteBufCodecUtils.VEC3_STREAM_CODEC, RFEFiringInput::pos,
            RFEFiringInput::new);

    public static void toNetwork(FriendlyByteBuf buf, RFEFiringInput input) {
        buf.writeResourceLocation(input.projectile)
                .writeDouble(input.aim.x)
                .writeDouble(input.aim.y)
                .writeDouble(input.aim.z)
                .writeDouble(input.pos.x)
                .writeDouble(input.pos.y)
                .writeDouble(input.pos.z);
    }
    
    public static RFEFiringInput fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation projectile = buf.readResourceLocation();
        Vec3 aim = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new RFEFiringInput(projectile, aim, pos);
    }
    
}
