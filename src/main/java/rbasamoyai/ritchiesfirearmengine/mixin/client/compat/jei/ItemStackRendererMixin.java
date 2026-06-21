package rbasamoyai.ritchiesfirearmengine.mixin.client.compat.jei;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.mixin.client.AbstractContainerScreenAccessor;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import java.util.List;

@Mixin(ItemStackRenderer.class)
public class ItemStackRendererMixin {

    @WrapMethod(method = "renderBatch")
    private void ritchiesfirearmengine$renderBatch(GuiGraphics guiGraphics, List<BatchRenderElement<ItemStack>> batchRenderElements, Operation<Void> original) {
        original.call(guiGraphics, batchRenderElements);
        if (!Screen.hasShiftDown())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> ctScreen))
            return;
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) ctScreen).getHoveredSlot();
        if (hoveredSlot == null)
            return;
        ItemStack itemStack = hoveredSlot.getItem();
        if (itemStack.getItem() instanceof RFEFirearmItem firearmItem) {
            RFEFirearmItem.GuiAmmoPredicate pred = RFEFirearmItem.getGuiValidAmmoPredicate(itemStack, firearmItem);
            for (BatchRenderElement<ItemStack> el : batchRenderElements) {
                RFEFirearmItem.GuiAmmoPredicate.Result res = pred.test(el.ingredient());
                if (res != RFEFirearmItem.GuiAmmoPredicate.Result.INVALID)
                    RFEClientRemix.renderValidAmmoHighlight(guiGraphics, el.x(), el.y(), res == RFEFirearmItem.GuiAmmoPredicate.Result.VALID_CURRENT_MODE
                            ? RFEConfig.CLIENT.compatibleAmmoHighlightColor.getAsInt()
                            : RFEConfig.CLIENT.incompatibleAmmoHighlightColor.getAsInt());
            }
        }
    }

}
