package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentsRenderingPacksHandler;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import java.util.Map;
import java.util.Optional;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow public abstract ItemModelShaper getItemModelShaper();

    @WrapMethod(method = "render")
    private void ritchiesfirearmengine$render(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight,
                                              int combinedOverlay, BakedModel model, Operation<Void> original) {
        Item parentItem = itemStack.getItem();
        ItemRendererModificationContext renderContext = new ItemRendererModificationContext();

        if (parentItem instanceof IHasRFEItemAttachments hasAttachments) {
            Map<ResourceLocation, ItemStack> renderedAttachments = hasAttachments.getAttachments(itemStack);
            poseStack.pushPose();
            RFEClientRemix.handleItemCameraTransforms(poseStack, model, displayContext, leftHand); // Restore parent item transforms
            for (Map.Entry<ResourceLocation, ItemStack> entry : renderedAttachments.entrySet()) {
                ItemStack attachmentStack = entry.getValue();
                if (attachmentStack.isEmpty())
                    continue;
                Optional<RFEItemAttachmentRenderProperties> op = RFEItemAttachmentsRenderingPacksHandler.getRenderProperties(itemStack, attachmentStack, entry.getKey());
                if (op.isEmpty())
                    continue;
                poseStack.pushPose();
                op.get().onRenderItemModel(original, this.getItemModelShaper(), itemStack, attachmentStack, displayContext,
                        leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, renderContext);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (renderContext.hideItem)
            return;
        original.call(itemStack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, model);
    }

}
