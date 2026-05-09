package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.function.Function;

public abstract class RFEProjectileRenderer {

    public final int getPackedLightCoords(RFEProjectileInstance instance, float partialTicks, Level level) {
        BlockPos blockpos = BlockPos.containing(instance.getPosition(partialTicks));
        return LightTexture.pack(this.getBlockLightLevel(instance, level, blockpos), this.getSkyLightLevel(instance, level, blockpos));
    }

    protected int getSkyLightLevel(RFEProjectileInstance instance, Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    protected int getBlockLightLevel(RFEProjectileInstance instance, Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos);
    }

    public abstract void renderProjectile(RFEProjectileInstance instance, Level level, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light);

    public boolean shouldRender(RFEProjectileInstance instance, Level level, Frustum camera, double camX, double camY, double camZ) {
        AABB aabb = instance.getAABB(level).inflate(0.5D);
        if (aabb.hasNaN() || aabb.getSize() == 0.0D) {
            Vec3 pos = instance.position();
            aabb = new AABB(pos.x - 2.0D, pos.y - 2.0D, pos.z - 2.0D, pos.x + 2.0D, pos.y + 2.0D, pos.z + 2.0D);
        }
        return camera.isVisible(aabb);
    }

    @FunctionalInterface
    public interface Serializer extends Function<JsonObject, RFEProjectileRenderer> {
    }

}
