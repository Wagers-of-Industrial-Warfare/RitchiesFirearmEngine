package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEClientPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.SimpleSlotAttachmentRenderData;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;

public class ScopeAttachmentRenderProperties extends SimpleSlotAttachmentRenderData {

    protected final AimOverlayTexture aimOverlayTexture;
    protected final ScopeGlint scopeGlint;

    protected ScopeAttachmentRenderProperties(ModelResourceLocation model, Matrix4f transforms,
                                              AimOverlayTexture aimOverlayTexture, ScopeGlint scopeGlint) {
        super(model, transforms);
        this.aimOverlayTexture = aimOverlayTexture;
        this.scopeGlint = scopeGlint;
    }

    @Override
    public void onRenderItemModelPre(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                     ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                     int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        boolean isAiming = parentItem.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.isAiming(parentItem, null);
        if (isAiming && displayContext.firstPerson())
            renderContext.hideItem = true;
    }

    @Override
    public void onRenderItemModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                  ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        poseStack.pushPose();
        super.onRenderItemModel(renderOp, modelShaper, parentItem, attachmentStack, displayContext, leftHand, poseStack,
                bufferSource, combinedLight, combinedOverlay, renderContext);
        poseStack.popPose();
        boolean isAiming = parentItem.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.isAiming(parentItem, null);
        if (isAiming && !displayContext.firstPerson() && this.scopeGlint.visualScale > 0
                && (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            poseStack.pushPose();
            poseStack.translate(this.scopeGlint.visualOffset.x / 16f, this.scopeGlint.visualOffset.y / 16f, this.scopeGlint.visualOffset.z / 16f);
            double limitSqr = this.scopeGlint.visibleRange * this.scopeGlint.visibleRange;
            Matrix4f lastMat = poseStack.last().pose();
            Vector3f cameraRenderPos = lastMat.getTranslation(new Vector3f());
            if (cameraRenderPos.lengthSquared() < limitSqr) {
                Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

                float cameraXRot = camera.getXRot() * Mth.DEG_TO_RAD;
                float cameraYRot = -camera.getYRot() * Mth.DEG_TO_RAD;
                float camCosX = Mth.cos(cameraXRot);
                Vector3f cameraViewVec = new Vector3f(Mth.sin(cameraYRot) * camCosX, -Mth.sin(cameraXRot), Mth.cos(cameraYRot) * camCosX);

                Quaternionf qf = lastMat.getUnnormalizedRotation(new Quaternionf()).normalize();
                // TODO possible customization of orientation to fit model, but very rare and probably not needed.
                Vector3f modelViewVec = new Vector3f(0, 0, -1).rotate(qf);

                Vector3f cameraPosVec = cameraRenderPos.normalize(new Vector3f());

                float cameraAndPositionAlignment = cameraViewVec.dot(cameraPosVec);
                float positionAndModelAlignment = cameraPosVec.dot(modelViewVec);

                final float FADE_TO_CUTOFF_ANGLE = 30;
                float glintCutoffAlignment = Mth.cos((float) (this.scopeGlint.fieldOfVisibility + FADE_TO_CUTOFF_ANGLE) * Mth.DEG_TO_RAD / 2f);
                float glintFadeAlignment = Mth.cos((float) this.scopeGlint.fieldOfVisibility * Mth.DEG_TO_RAD / 2f);

                if (cameraAndPositionAlignment > glintCutoffAlignment && positionAndModelAlignment < -glintCutoffAlignment) {
                    float fadeScale = Mth.clamp((cameraAndPositionAlignment - glintCutoffAlignment) / (glintFadeAlignment - glintCutoffAlignment), 0, 1);
                    float rotation = cameraViewVec.cross(cameraPosVec, new Vector3f()).dot(0, 1, 0);

                    Quaternionf glintRot = new Quaternionf().set(camera.rotation());
                    glintRot.rotateZ(rotation * Mth.PI);

                    RenderType glintRenderType = RenderType.entityTranslucentCull(this.scopeGlint.textureLocation);

                    float glintScale = this.scopeGlint.calculateVisualScale(cameraRenderPos.length()) / 4f * fadeScale * fadeScale;
                    this.renderRotatedQuad(bufferSource.getBuffer(glintRenderType), glintRot, cameraRenderPos.x,
                            cameraRenderPos.y, cameraRenderPos.z, LightTexture.FULL_BRIGHT, glintScale);
                }
            }
            poseStack.popPose();
        }
    }

    // Cribbed the following two methods from SingleQuadParticle --ritchie

    protected void renderRotatedQuad(VertexConsumer vcons, Quaternionf quaternion, float x, float y, float z, int packedLight, float scale) {
        renderVertex(vcons, quaternion, x, y, z, -0.5f, -0.5f, scale, 0, 1, packedLight);
        renderVertex(vcons, quaternion, x, y, z, 0.5f, -0.5f, scale, 1, 1, packedLight);
        renderVertex(vcons, quaternion, x, y, z, 0.5f, 0.5f, scale, 1, 0, packedLight);
        renderVertex(vcons, quaternion, x, y, z, -0.5f, 0.5f, scale, 0, 0, packedLight);
    }

    private static void renderVertex(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float xOffset,
            float yOffset, float quadSize, float u, float v, int packedLight) {
        Vector3f vector3f = new Vector3f(xOffset, yOffset, 0.0F).rotate(quaternion).mul(quadSize).add(x, y, z);
        buffer.addVertex(vector3f.x(), vector3f.y(), vector3f.z())
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    @Override
    public void onRenderOverlay(GuiGraphics graphics, float partialTick, ItemStack parentItem, ItemStack attachmentStack) {
        if (parentItem.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.isAiming(parentItem, null)) {
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            float minDim = Math.min(width, height);
            float minScaling = Math.min(width / minDim, height / minDim);
            int texWidth = Mth.floor(minDim * minScaling);
            int texHeight = Mth.floor(minDim * minScaling);
            int blitTopLeftX = (width - texWidth) / 2;
            int blitTopLeftY = (height - texHeight) / 2;
            int blitBottomRightX = blitTopLeftX + texWidth;
            int blitBottomRightY = blitTopLeftY + texHeight;
            RenderSystem.enableBlend();
            graphics.blit(this.aimOverlayTexture.location, blitTopLeftX, blitTopLeftY, -90, 0, 0,
                    texWidth, texHeight, texWidth, texHeight);
            RenderSystem.disableBlend();

            graphics.fill(RenderType.guiOverlay(), 0, blitBottomRightY, width, height, -90, 0xFF000000);
            graphics.fill(RenderType.guiOverlay(), 0, 0, width, blitTopLeftY, -90, 0xFF000000);
            graphics.fill(RenderType.guiOverlay(), 0, blitTopLeftY, blitTopLeftX, blitBottomRightY, -90, 0xFF000000);
            graphics.fill(RenderType.guiOverlay(), blitBottomRightX, blitTopLeftY, width, blitBottomRightY, -90, 0xFF000000);
        }
    }

    @Override
    public RFEItemAttachmentRenderProperties.Serializer<?> getSerializer() {
        return BuiltInRFEClientPlugin.ItemAttachmentRendering.SCOPE;
    }

    public static class Serializer implements RFEItemAttachmentRenderProperties.Serializer<ScopeAttachmentRenderProperties> {
        public static final MapCodec<ScopeAttachmentRenderProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                SimpleSlotAttachmentRenderData.Serializer.CODEC.forGetter(p -> p),
                AimOverlayTexture.CODEC.fieldOf("aim_overlay_texture").forGetter(p -> p.aimOverlayTexture),
                ScopeGlint.CODEC.optionalFieldOf("scope_glint", ScopeGlint.INVISIBLE).forGetter(p -> p.scopeGlint)
        ).apply(o, Serializer::fromCodec));

        protected static ScopeAttachmentRenderProperties fromCodec(SimpleSlotAttachmentRenderData parentData,
                                                                   AimOverlayTexture overlayTexture, ScopeGlint scopeGlint) {
            return new ScopeAttachmentRenderProperties(parentData.model(), parentData.transforms(), overlayTexture, scopeGlint);
        }

        @Override public MapCodec<ScopeAttachmentRenderProperties> codec() { return CODEC; }
    }

    public record AimOverlayTexture(ResourceLocation location) {
        public static final Codec<AimOverlayTexture> CODEC = RecordCodecBuilder.create(o -> o.group(
                ResourceLocation.CODEC.fieldOf("location").forGetter(AimOverlayTexture::location)
        ).apply(o, AimOverlayTexture::new));
    }

    public record ScopeGlint(ResourceLocation textureLocation, Vector3f visualOffset, float visualScale, double visibleRange,
                             double distanceScalingExponent, double fieldOfVisibility) {
        public static final ScopeGlint INVISIBLE = new ScopeGlint(RitchiesFirearmEngine.resource("invalid"), new Vector3f(),
                0f, 0d, 0, 0);

        public static final Codec<ScopeGlint> CODEC = RecordCodecBuilder.create(o -> o.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(ScopeGlint::textureLocation),
                ExtraCodecs.VECTOR3F.fieldOf("visual_offset").forGetter(ScopeGlint::visualOffset),
                Codec.floatRange(0f, 4f).optionalFieldOf("visual_scale", 1f).forGetter(ScopeGlint::visualScale),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("visible_range", 1024d).forGetter(ScopeGlint::visibleRange),
                Codec.doubleRange(0, 1).optionalFieldOf("distance_scaling_exponent", 0.5d).forGetter(ScopeGlint::distanceScalingExponent),
                Codec.doubleRange(0, 120).optionalFieldOf("field_of_visibility", 95d).forGetter(ScopeGlint::fieldOfVisibility)
        ).apply(o, ScopeGlint::new));

        public float calculateVisualScale(double distance) {
            return this.visualScale * (float) Math.pow(distance, this.distanceScalingExponent);
        }
    }

}
