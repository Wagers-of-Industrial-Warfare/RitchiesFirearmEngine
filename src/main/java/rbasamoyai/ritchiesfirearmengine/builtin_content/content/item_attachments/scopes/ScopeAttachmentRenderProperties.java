package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.SimpleSlotAttachmentRenderData;
import rbasamoyai.ritchiesfirearmengine.remix.ItemRendererModificationContext;

public class ScopeAttachmentRenderProperties extends SimpleSlotAttachmentRenderData {

    protected final AimOverlayTexture aimOverlayTexture;

    protected ScopeAttachmentRenderProperties(ModelResourceLocation model, Matrix4f transforms, AimOverlayTexture aimOverlayTexture) {
        super(model, transforms);
        this.aimOverlayTexture = aimOverlayTexture;
    }

    @Override
    public void onRenderItemModel(Operation<Void> renderOp, ItemModelShaper modelShaper, ItemStack parentItem, ItemStack attachmentStack,
                                  ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, ItemRendererModificationContext renderContext) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if ((displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                && parentItem.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.isAiming(parentItem, player)) {
            renderContext.hideItem = true;
        } else {
            super.onRenderItemModel(renderOp, modelShaper, parentItem, attachmentStack, displayContext, leftHand, poseStack, bufferSource, combinedLight, combinedOverlay, renderContext);
        }
    }

    @Override
    public void onRenderOverlay(GuiGraphics graphics, float partialTick, ItemStack parentItem, ItemStack attachmentStack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (parentItem.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.isAiming(parentItem, player)) {
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

    public static class Serializer implements RFEItemAttachmentRenderProperties.Serializer<ScopeAttachmentRenderProperties> {
        public static final MapCodec<ScopeAttachmentRenderProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                SimpleSlotAttachmentRenderData.Serializer.CODEC.forGetter(p -> p),
                AimOverlayTexture.CODEC.fieldOf("aim_overlay_texture").forGetter(p -> p.aimOverlayTexture)
        ).apply(o, Serializer::fromCodec));

        protected static ScopeAttachmentRenderProperties fromCodec(SimpleSlotAttachmentRenderData parentData, AimOverlayTexture overlayTexture) {
            return new ScopeAttachmentRenderProperties(parentData.model(), parentData.transforms(), overlayTexture);
        }

        @Override public MapCodec<ScopeAttachmentRenderProperties> codec() { return CODEC; }
    }

    public record AimOverlayTexture(ResourceLocation location) {
        public static final MapCodec<AimOverlayTexture> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                ResourceLocation.CODEC.fieldOf("location").forGetter(AimOverlayTexture::location)
        ).apply(o, AimOverlayTexture::new));
    }

}
