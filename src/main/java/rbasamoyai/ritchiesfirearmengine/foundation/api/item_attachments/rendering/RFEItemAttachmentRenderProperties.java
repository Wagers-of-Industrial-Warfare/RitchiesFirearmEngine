package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEClientContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;

public interface RFEItemAttachmentRenderProperties {

    MapCodec<RFEItemAttachmentRenderProperties> CODEC = ResourceLocation.CODEC
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEClientContentBuilderRegistry.getItemAttachmentRenderingSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment rendering type: " + e.getMessage());
                        }
                    },
                    ser -> {
                        try {
                            return DataResult.success(RFEClientContentBuilderRegistry.getItemAttachmentRenderingSerializerId(ser));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment rendering type id: " + e.getMessage());
                        }
                    })
            .dispatchMap(RFEItemAttachmentRenderProperties::getSerializer, Serializer::codec);

    default void onRenderItemModelPre(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                      ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {}

    default void onRenderItemModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                   ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                   int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {}

    default void onRenderIntegralAttachmentPre(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem,
                                               DataComponentPatch attachmentData, ItemDisplayContext displayContext,
                                               boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                               int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {}

    default void onRenderIntegralAttachmentModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem,
                                                 DataComponentPatch attachmentData, ItemDisplayContext displayContext,
                                                 boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {}

    default void onRenderOverlay(GuiGraphics graphics, float partialTick, ItemStack parentItem, ItemStack attachmentStack) {}

    default void onRenderIntegralOverlay(GuiGraphics graphics, float partialTick, ItemStack parentItem, DataComponentPatch attachmentData) {}

    Serializer<?> getSerializer();

    interface Serializer<T extends RFEItemAttachmentRenderProperties> {
        MapCodec<T> codec();
    }

}
