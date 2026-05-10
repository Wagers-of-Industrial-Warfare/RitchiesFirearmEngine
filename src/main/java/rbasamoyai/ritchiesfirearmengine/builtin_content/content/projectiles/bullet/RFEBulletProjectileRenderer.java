package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMatrixUtils;

public class RFEBulletProjectileRenderer extends RFEProjectileRenderer {

    private static final ResourceLocation COLOR_LOCATION = RitchiesFirearmEngine.resource("textures/entity/color.png");
    private static final RenderType COLOR = RenderType.entityTranslucentCull(COLOR_LOCATION);
    
    protected final int rHead;
    protected final int gHead;
    protected final int bHead;
    protected final int rTail;
    protected final int gTail;
    protected final int bTail;
    protected final boolean enableTrail;
    protected final float trailMultiplier;
    protected final boolean tracerLight;
    protected final float thickness;

    public RFEBulletProjectileRenderer(int color, int color1, boolean enableTrail, float trailMultiplier, boolean tracerLight, float thickness) {
        this.rHead = (color >> 16) & 255;
        this.gHead = (color >> 8) & 255;
        this.bHead = color & 255;
        this.rTail = (color1 >> 16) & 255;
        this.gTail = (color1 >> 8) & 255;
        this.bTail = color1 & 255;
        this.enableTrail = enableTrail;
        this.trailMultiplier = trailMultiplier;
        this.tracerLight = tracerLight;
        this.thickness = thickness;
    }

    @Override
    public void renderProjectile(RFEProjectileInstance instance, Level level, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Vec3 previous = instance.oldPosition();
        Vec3 diff = instance.position().subtract(previous);
        double dlSqr = diff.lengthSqr();
        boolean isFastButNotTeleported = 1e-4d <= dlSqr && dlSqr <= instance.velocity().lengthSqr() * 4;
        double diffLength = isFastButNotTeleported ? diff.length() : 0;
        double displacement = instance.distanceTravelled() - diffLength * (1 - partialTick);
        float length = this.enableTrail ? (float) Math.min(diffLength, displacement) * this.trailMultiplier : this.thickness * 2;

        Vec3 vel = instance.velocity();
        if (vel.lengthSqr() < 1e-4d)
            vel = new Vec3(0, -1, 0);
        poseStack.pushPose();
        if (vel.horizontalDistanceSqr() > 1e-4d && Math.abs(vel.y) > 1e-2d) {
            Vec3 horizontal = new Vec3(vel.x, 0, vel.z).normalize();
            poseStack.mulPose(RFEMatrixUtils.mat4x4fFacing(vel.normalize().reverse(), horizontal));
            poseStack.mulPose(RFEMatrixUtils.mat4x4fFacing(horizontal, new Vec3(0, 0, 1)));
        } else {
            poseStack.mulPose(RFEMatrixUtils.mat4x4fFacing(vel.normalize(), new Vec3(0, 0, 1)));
        }

        light = this.tracerLight ? LightTexture.FULL_BRIGHT : light;
        PoseStack.Pose lastPose = poseStack.last();

        VertexConsumer vcons = buffers.getBuffer(COLOR);
        renderBox(vcons, lastPose, this.rHead, this.gHead, this.bHead, this.rTail, this.gTail, this.bTail, length, this.thickness, light);

        poseStack.popPose();
    }

    private static void renderBox(VertexConsumer builder, PoseStack.Pose pose, int rHead, int gHead, int bHead,
                                  int rTail, int gTail, int bTail, float length, float thickness, int light) {
        float x1 = -thickness;
        float y1 = -thickness;
        float z1 = -thickness - length;
        float x2 = thickness;
        float y2 = thickness;
        float z2 = thickness;

        // Front
        vertex(builder, pose, rTail, gTail, bTail, x1, y1, z1, light);
        vertex(builder, pose, rTail, gTail, bTail, x1, y2, z1, light);
        vertex(builder, pose, rTail, gTail, bTail, x2, y2, z1, light);
        vertex(builder, pose, rTail, gTail, bTail, x2, y1, z1, light);

        // Right
        vertex(builder, pose, rTail, gTail, bTail, x1, y1, z1, light);
        vertex(builder, pose, rHead, gHead, bHead, x1, y1, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x1, y2, z2, light);
        vertex(builder, pose, rTail, gTail, bTail, x1, y2, z1, light);

        // Back
        vertex(builder, pose, rHead, gHead, bHead, x1, y1, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x2, y1, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x2, y2, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x1, y2, z2, light);

        // Left
        vertex(builder, pose, rTail, gTail, bTail, x2, y1, z1, light);
        vertex(builder, pose, rTail, gTail, bTail, x2, y2, z1, light);
        vertex(builder, pose, rHead, gHead, bHead, x2, y2, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x2, y1, z2, light);

        // Down
        vertex(builder, pose, rHead, gHead, bHead, x2, y1, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x1, y1, z2, light);
        vertex(builder, pose, rTail, gTail, bTail, x1, y1, z1, light);
        vertex(builder, pose, rTail, gTail, bTail, x2, y1, z1, light);

        // Up
        vertex(builder, pose, rTail, gTail, bTail, x1, y2, z1, light);
        vertex(builder, pose, rHead, gHead, bHead, x1, y2, z2, light);
        vertex(builder, pose, rHead, gHead, bHead, x2, y2, z2, light);
        vertex(builder, pose, rTail, gTail, bTail, x2, y2, z1, light);
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, int r, int g, int b, float x, float y, float z, int light) {
        builder.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    protected int getBlockLightLevel(RFEProjectileInstance instance, Level level, BlockPos pos) {
        return this.tracerLight ? 15 : super.getBlockLightLevel(instance, level, pos);
    }

    public static class Serializer implements RFEProjectileRenderer.Serializer {
        @Override
        public RFEProjectileRenderer apply(JsonObject obj) {
            int color = GsonHelper.getAsInt(obj, "color");
            int color1 = GsonHelper.getAsInt(obj, "color1", color);
            boolean enableTrail = GsonHelper.isNumberValue(obj, "trail_multiplier");
            float trailMultiplier = enableTrail ? Math.max(0, GsonHelper.getAsFloat(obj, "trail_multiplier")) : -1;
            boolean tracerLight = GsonHelper.getAsBoolean(obj, "tracer_light", false);
            float thickness = Mth.clamp(GsonHelper.getAsFloat(obj, "thickness", 1 / 8f), 0, 2);
            return new RFEBulletProjectileRenderer(color, color1, enableTrail, trailMultiplier, tracerLight, thickness);
        }
    }

}
