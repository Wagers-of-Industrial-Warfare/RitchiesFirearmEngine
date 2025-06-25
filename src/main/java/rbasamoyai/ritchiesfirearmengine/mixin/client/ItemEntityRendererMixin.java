package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity> {

	@Shadow @Final private ItemRenderer itemRenderer;

	ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@WrapMethod(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
	public void ritchiesfirearmengine$render(ItemEntity entity, float yaw, float partialTicks, PoseStack poseStack,
											 MultiBufferSource buffers, int light, Operation<Void> original) {
		ItemStack stack = entity.getItem();
		if (stack.getItem() instanceof RFEFirearmItem firearm && firearm.laysFlatOnGround(stack)) {
			poseStack.pushPose();

			BakedModel model = this.itemRenderer.getModel(stack, entity.level(), null, entity.getId());
			poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

			this.itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, buffers, light, OverlayTexture.NO_OVERLAY, model);
			poseStack.popPose();

			super.render(entity, yaw, partialTicks, poseStack, buffers, light);
		} else {
			original.call(entity, yaw, partialTicks, poseStack, buffers, light);
		}
	}
}
