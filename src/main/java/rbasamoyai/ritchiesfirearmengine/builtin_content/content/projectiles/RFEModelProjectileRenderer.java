package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;

import java.util.List;

public class RFEModelProjectileRenderer extends RFEProjectileRenderer {

    protected final ResourceLocation modelLoc;
    protected final boolean tracerLight;
    protected final float scale;

    public RFEModelProjectileRenderer(ResourceLocation modelLoc, boolean tracerLight, float scale) {
        this.modelLoc = modelLoc;
        this.tracerLight = tracerLight;
        this.scale = scale;
    }

    @Override
    public void renderProjectile(RFEProjectileInstance instance, Level level, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        RandomSource rand = RandomSource.create();
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.inventory(this.modelLoc));
        VertexConsumer vCons = buffers.getBuffer(Sheets.translucentItemSheet());
        for (Direction dir : Direction.values()) {
            rand.setSeed(42L);
            renderQuadList(poseStack.last(), vCons, 1f, 1f, 1f, 1f, model.getQuads(null, dir, rand), light);
        }
        rand.setSeed(42L);
        renderQuadList(poseStack.last(), vCons, 1f, 1f, 1f, 1f, model.getQuads(null, null, rand), light);
    }

    private static void renderQuadList(PoseStack.Pose pose, VertexConsumer consumer, float red, float green, float blue, float alpha, List<BakedQuad> quads, int packedLight) {
        for (BakedQuad quad : quads) {
            float f;
            float f1;
            float f2;
            if (quad.isTinted()) {
                f = Mth.clamp(red, 0.0F, 1.0F);
                f1 = Mth.clamp(green, 0.0F, 1.0F);
                f2 = Mth.clamp(blue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }
            consumer.putBulkData(pose, quad, f, f1, f2, alpha, packedLight, OverlayTexture.NO_OVERLAY, true);
        }
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
            return new RFEModelProjectileRenderer(modelLoc, tracerLight, scale);
        }
    }

}
