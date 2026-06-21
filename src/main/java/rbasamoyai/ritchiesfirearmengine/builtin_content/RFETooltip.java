package rbasamoyai.ritchiesfirearmengine.builtin_content;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;

import java.util.List;

public class RFETooltip {

    public static void addAmmoHighlightingTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean currentlyHighlighting = Screen.hasShiftDown();
        String suffix = RitchiesFirearmEngine.MOD_ID + ".tooltip.item.hold_key.ammo";
        tooltipComponents.add(Component.translatable(suffix, Component.translatable(RitchiesFirearmEngine.MOD_ID + ".tooltip.key_shift")
                        .withStyle(currentlyHighlighting ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (currentlyHighlighting) {
            // Do not need to mask out the alpha in 0xAARRGGBB; #withColor already does this for us
            tooltipComponents.add(Component.literal("- ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(suffix + ".compatible").withColor(RFEConfig.CLIENT.compatibleAmmoHighlightColor.getAsInt())));
            tooltipComponents.add(Component.literal("- ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(suffix + ".incompatible").withColor(RFEConfig.CLIENT.incompatibleAmmoHighlightColor.getAsInt())));
        }
    }

}
