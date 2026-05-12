package rbasamoyai.ritchiesfirearmengine.builtin_content;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

import java.util.List;

public class RFETooltip {

    public static void addAmmoHighlightingTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean allAmmo = Screen.hasShiftDown() && Screen.hasControlDown();
        boolean modeAmmo = !allAmmo && Screen.hasShiftDown();
        tooltipComponents.add(Component.translatable(RitchiesFirearmEngine.MOD_ID + ".tooltip.item.hold_key.mode_ammo",
                        Component.translatable(RitchiesFirearmEngine.MOD_ID + ".tooltip.key_shift").withStyle(modeAmmo ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable(RitchiesFirearmEngine.MOD_ID + ".tooltip.item.hold_key.all_ammo",
                        Component.translatable(RitchiesFirearmEngine.MOD_ID + ".tooltip.key_ctrl_shift").withStyle(allAmmo ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

}
