package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEClientPlugin;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;

public class SimpleSlotAttachmentRenderData implements RFEItemAttachmentRenderProperties {

    protected final ModelResourceLocation model;
    protected final Vector3f scale;
    protected final Vector3f rotations;
    protected final Matrix4f rotationMat;
    protected final Vector3f translation;

    public SimpleSlotAttachmentRenderData(ModelResourceLocation model, Vector3f scale, Vector3f rotations, Vector3f translation) {
        this.model = model;
        this.scale = scale;
        this.rotations = rotations;
        this.rotationMat = new Matrix4f().rotateAffineZYX(this.rotations.z * Mth.DEG_TO_RAD, this.rotations.y * Mth.DEG_TO_RAD, this.rotations.x * Mth.DEG_TO_RAD);
        this.translation = translation;
    }

    public ModelResourceLocation model() { return this.model; }
    public Vector3f scale() { return this.scale; }
    public Vector3f rotations() { return this.rotations; }
    public Vector3f translation() { return this.translation; }

    @Override
    public void onRenderItemModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                  ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        if (renderContext.hideItem)
            return;
        BakedModel attachmentModel = modelShaper.getModelManager().getModel(this.model);
        // TODO combine context from both attachment and firearm item, or pass extra context to attachment stack
        BakedModel overrideModel = attachmentModel.getOverrides().resolve(attachmentModel, attachmentStack, null, null, 42);
        poseStack.scale(this.scale.x, this.scale.y, this.scale.z);
        poseStack.translate(this.translation.x / 16f, this.translation.y / 16f, this.translation.z / 16f);
        poseStack.mulPose(this.rotationMat);
        renderOp.call(attachmentStack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, overrideModel);
    }

    @Override
    public void onRenderIntegralAttachmentModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem,
                                                DataComponentPatch attachmentData, ItemDisplayContext displayContext,
                                                boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                                int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        if (renderContext.hideItem)
            return;
        // TODO combine context from both attachment and firearm item, or pass extra context to attachment stack
        ItemStack contextItemStack = parentItem.copy();
        contextItemStack.applyComponents(attachmentData); // This could be better? e.g. different item
        BakedModel attachmentModel = modelShaper.getModelManager().getModel(this.model);
        BakedModel overrideModel = attachmentModel.getOverrides().resolve(attachmentModel, contextItemStack, null, null, 42);
        poseStack.scale(this.scale.x, this.scale.y, this.scale.z);
        poseStack.translate(this.translation.x / 16f, this.translation.y / 16f, this.translation.z / 16f);
        poseStack.mulPose(this.rotationMat);
        renderOp.call(parentItem, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, overrideModel);
    }

    @Override
    public RFEItemAttachmentRenderProperties.Serializer<?> getSerializer() {
        return BuiltInRFEClientPlugin.ItemAttachmentRendering.SIMPLE;
    }

    public static class Serializer implements RFEItemAttachmentRenderProperties.Serializer<SimpleSlotAttachmentRenderData> {
        public static final MapCodec<SimpleSlotAttachmentRenderData> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                ResourceLocation.CODEC.xmap(ModelResourceLocation::standalone, ModelResourceLocation::id).fieldOf("model").forGetter(SimpleSlotAttachmentRenderData::model),
                ExtraCodecs.VECTOR3F.optionalFieldOf("scale", new Vector3f(1f, 1f, 1f)).forGetter(SimpleSlotAttachmentRenderData::scale),
                ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(SimpleSlotAttachmentRenderData::rotations),
                ExtraCodecs.VECTOR3F.optionalFieldOf("translation", new Vector3f()).forGetter(SimpleSlotAttachmentRenderData::translation)
        ).apply(o, SimpleSlotAttachmentRenderData::new));

        @Override public MapCodec<SimpleSlotAttachmentRenderData> codec() { return CODEC; }
    }

}
