package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Nullable public Screen screen;

    @Shadow @Final public Options options;

    @Shadow @Final public MouseHandler mouseHandler;

    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 10))
    private boolean ritchiesfirearmengine$handleKeybinds$useAttack(KeyMapping instance, Operation<Boolean> original) {
        boolean result = original.call(instance);
        if (result)
            RFEClientRemix.handleAttackKeybinds();
        return result;
    }

    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 13))
    private boolean ritchiesfirearmengine$handleKeybinds$attack(KeyMapping instance, Operation<Boolean> original) {
        boolean result = original.call(instance);
        if (result)
            RFEClientRemix.handleAttackKeybinds();
        return result;
    }

    @WrapMethod(method = "continueAttack")
    private void ritchiesfirearmengine$continueAttack(boolean leftClick, Operation<Void> original) {
        original.call(leftClick);
        boolean dontInterruptFiring = this.screen == null && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed();
        if (!dontInterruptFiring)
            RFEClientRemix.interruptAttack();
    }

}
