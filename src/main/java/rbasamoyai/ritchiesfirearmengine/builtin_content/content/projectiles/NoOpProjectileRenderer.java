package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;

public class NoOpProjectileRenderer extends RFEProjectileRenderer {

    public static NoOpProjectileRenderer INSTANCE = new NoOpProjectileRenderer();

    private NoOpProjectileRenderer() {}

    @Override
    public void renderProjectile(RFEProjectileInstance instance, Level level, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
    }

    @Override
    public boolean shouldRender(RFEProjectileInstance instance, Level level, Frustum camera,
                                double camX, double camY, double camZ) {
        return false;
    }

    public static class Serializer implements RFEProjectileRenderer.Serializer {
        @Override public RFEProjectileRenderer apply(JsonObject jsonObject) { return INSTANCE; }
    }

}
