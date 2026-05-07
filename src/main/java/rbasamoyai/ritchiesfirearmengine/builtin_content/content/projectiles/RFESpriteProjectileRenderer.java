package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;

public class RFESpriteProjectileRenderer extends RFEProjectileRenderer {

    protected final ResourceLocation textureLoc;
    protected final boolean tracerLight;
    protected final float scale;
    protected final RenderType renderType;

    public RFESpriteProjectileRenderer(ResourceLocation textureLoc, boolean tracerLight, float scale) {
        this.textureLoc = textureLoc;
        this.tracerLight = tracerLight;
        this.scale = scale;
        this.renderType = RenderType.entityCutoutNoCull(this.textureLoc);
    }

    @Override
    public void renderProjectile(RFEProjectileInstance instance, Level level, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.scale(this.scale, this.scale, this.scale);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vcons = buffers.getBuffer(this.renderType);
        vertex(vcons, pose, light, 0.0F, 0, 0, 1);
        vertex(vcons, pose, light, 1.0F, 0, 1, 1);
        vertex(vcons, pose, light, 1.0F, 1, 1, 0);
        vertex(vcons, pose, light, 0.0F, 1, 0, 0);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float)y - 0.25F, 0.0F)
                .setColor(-1)
                .setUv((float)u, (float)v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    protected int getBlockLightLevel(RFEProjectileInstance instance, Level level, BlockPos pos) {
        return this.tracerLight ? 15 : super.getBlockLightLevel(instance, level, pos);
    }

    public static class Serializer implements RFEProjectileRenderer.Serializer {
        @Override
        public RFEProjectileRenderer apply(JsonObject obj) {
            ResourceLocation modelLoc = ResourceLocation.parse(GsonHelper.getAsString(obj, "model"));
            boolean tracerLight = GsonHelper.getAsBoolean(obj, "tracer_light", false);
            float scale = Mth.clamp(GsonHelper.getAsFloat(obj, "scale", 1), 0, 4);
            return new RFESpriteProjectileRenderer(modelLoc, tracerLight, scale);
        }
    }

}
