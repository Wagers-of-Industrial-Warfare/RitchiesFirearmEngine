package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record RFEFiringInput(ResourceLocation projectile, Vec3 aim, Vec3 pos) {
    
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
