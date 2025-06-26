package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.content.firearms.IFirearmItem;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final protected Minecraft minecraft;

    @WrapMethod(method = "renderCrosshair")
    private void ritchiesfirearmengine$renderCrosshair(GuiGraphics guiGraphics, Operation<Void> original) {
        ItemStack mainhandItem = this.minecraft.player.getMainHandItem();
        // TODO offhand, custom crosshairs
        if (mainhandItem.getItem() instanceof IFirearmItem firearm && firearm.isAiming(mainhandItem, this.minecraft.player))
            return;
        original.call(guiGraphics);
    }

}
