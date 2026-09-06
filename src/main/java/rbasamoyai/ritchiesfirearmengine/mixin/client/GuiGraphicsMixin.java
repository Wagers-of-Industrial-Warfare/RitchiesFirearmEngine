package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsScreen;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow public abstract PoseStack pose();

    @Unique
    private final GuiGraphics ritchiesfirearmengine$self = (GuiGraphics) (Object) this;

    @WrapMethod(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V")
    private void ritchiesfirearmengine$renderItemDecorations(Font font, ItemStack itemStack, int x, int y, @Nullable String text, Operation<Void> original) {
        original.call(font, itemStack, x, y, text);
        if (!itemStack.isEmpty())
            RFEClientRemix.renderSlotOverlays(this.ritchiesfirearmengine$self, font, itemStack, x, y);
    }

    @WrapMethod(method = "renderTooltipInternal")
    private void ritchiesfirearmengine$renderTooltipInternal(Font font, List<ClientTooltipComponent> components, int mouseX,
                                                             int mouseY, ClientTooltipPositioner tooltipPositioner,
                                                             Operation<Void> original) {
        this.pose().pushPose();
        if (Minecraft.getInstance().screen instanceof RFEItemAttachmentsScreen)
            this.pose().translate(0, 0, 300);
        original.call(font, components, mouseX, mouseY, tooltipPositioner);
        this.pose().popPose();
    }

}
