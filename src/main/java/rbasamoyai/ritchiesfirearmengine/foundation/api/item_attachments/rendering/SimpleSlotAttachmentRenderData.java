package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;

public class SimpleSlotAttachmentRenderData implements RFEItemAttachmentRenderProperties {

    protected final ModelResourceLocation model;
    protected final Matrix4f transforms;

    public SimpleSlotAttachmentRenderData(ModelResourceLocation model, Matrix4f transforms) {
        this.model = model;
        this.transforms = transforms;
    }

    public static SimpleSlotAttachmentRenderData fromSRTVectors(ModelResourceLocation model, Vector3f scale,
                                                                Vector3f rotationXYZDeg, Vector3f translation) {
        Matrix4f transforms = new Matrix4f().identity();
        transforms.translation(translation.div(16f));
        transforms.rotateAffineZYX(rotationXYZDeg.z * Mth.DEG_TO_RAD, rotationXYZDeg.y * Mth.DEG_TO_RAD, rotationXYZDeg.x * Mth.DEG_TO_RAD);
        transforms.scale(scale);
        return new SimpleSlotAttachmentRenderData(model, transforms);
    }

    public ModelResourceLocation model() { return this.model; }
    public Matrix4f transforms() { return this.transforms; }
    public Vector3f scale() { return this.transforms.getScale(new Vector3f()); }
    public Vector3f rotations() { return this.transforms.getEulerAnglesZYX(new Vector3f()).mul(Mth.RAD_TO_DEG); }
    public Vector3f translation() { return this.transforms.getTranslation(new Vector3f()); }

    @Override
    public void onRenderItemModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                  ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        BakedModel attachmentModel = modelShaper.getModelManager().getModel(this.model);
        poseStack.mulPose(this.transforms);
        renderOp.call(attachmentStack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, attachmentModel);
    }

    @Override
    public RFEItemAttachmentRenderProperties.Serializer<?> getSerializer() {
        return null;
    }

    public static class Serializer implements RFEItemAttachmentRenderProperties.Serializer<SimpleSlotAttachmentRenderData> {
        public static final MapCodec<SimpleSlotAttachmentRenderData> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                ResourceLocation.CODEC.xmap(ModelResourceLocation::standalone, ModelResourceLocation::id).fieldOf("model").forGetter(SimpleSlotAttachmentRenderData::model),
                ExtraCodecs.VECTOR3F.optionalFieldOf("scale", new Vector3f(1f, 1f, 1f)).forGetter(SimpleSlotAttachmentRenderData::scale),
                ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(SimpleSlotAttachmentRenderData::rotations),
                ExtraCodecs.VECTOR3F.optionalFieldOf("translation", new Vector3f()).forGetter(SimpleSlotAttachmentRenderData::translation)
        ).apply(o, SimpleSlotAttachmentRenderData::fromSRTVectors));

        @Override public MapCodec<SimpleSlotAttachmentRenderData> codec() { return CODEC; }
    }

}
