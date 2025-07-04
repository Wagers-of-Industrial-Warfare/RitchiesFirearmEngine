package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float ritchiesfirearmengine$tick(float original, @Local LocalPlayer localPlayer, @Local(ordinal = 0) ItemStack itemStack) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearmItem && firearmItem.disableAttackAnimation(itemStack, localPlayer))
            return 1;
        return original;
    }

    @WrapMethod(method = "itemUsed")
    private void ritchiesfirearmengine$tick(InteractionHand hand, Operation<Void> original) {
        ItemStack itemStack = this.minecraft.player.getItemInHand(hand);
        if (!(itemStack.getItem() instanceof RFEFirearmItem))
            original.call(hand);
    }

}
