package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rbasamoyai.ritchiesfirearmengine.utils.RFEClientRemix;

import javax.annotation.Nullable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Unique
    private final GuiGraphics ritchiesfirearmengine$self = (GuiGraphics) (Object) this;

    @WrapMethod(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V")
    private void ritchiesfirearmengine$renderItemDecorations(Font font, ItemStack itemStack, int x, int y, @Nullable String text, Operation<Void> original) {
        original.call(font, itemStack, x, y, text);
        if (!itemStack.isEmpty())
            RFEClientRemix.renderSlotOverlays(this.ritchiesfirearmengine$self, font, itemStack, x, y);
    }

}
