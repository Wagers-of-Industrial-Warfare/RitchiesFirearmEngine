package rbasamoyai.ritchiesfirearmengine.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.remix.RFEClientRemix;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow @Nullable protected Slot hoveredSlot;

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void ritchiesfirearmengine$render(AbstractContainerScreen<?> instance, GuiGraphics guiGraphics, int mouseX, int mouseY, Operation<Void> original) {
        if (this.hoveredSlot != null) {
            ItemStack itemStack = this.hoveredSlot.getItem();
            if (itemStack.getItem() instanceof RFEFirearmItem firearmItem) {
                Predicate<ItemStack> pred = RFEFirearmItem.getGuiValidAmmoPredicate(itemStack, firearmItem, Screen.hasShiftDown(), Screen.hasControlDown());
                for (int k = 0; k < this.menu.slots.size(); k++) {
                    Slot slot = this.menu.slots.get(k);
                    if (pred.test(slot.getItem()))
                        RFEClientRemix.renderValidAmmoHighlight(guiGraphics, slot);
                }
            }
        }
        original.call(instance, guiGraphics, mouseX, mouseY);
    }

}
