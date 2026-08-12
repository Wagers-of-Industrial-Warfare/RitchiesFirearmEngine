package rbasamoyai.ritchiesfirearmengine.builtin_content;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.RFEClient;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentTooltipContext;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.mixin.client.AbstractContainerScreenAccessor;

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

    public static void addAttachmentsKeyTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (context instanceof AttachmentTooltipContext atCtx && atCtx.inAttachmentsScreen())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?>))
            return;
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) mc.screen).getHoveredSlot();
        if (hoveredSlot == null || mc.player == null || hoveredSlot.container != mc.player.getInventory())
            return;
        if (RFEClient.isOpeningFieldAttachmentsScreen()) {
            int openingTime = RFEClient.getFieldAttachmentsScreenOpeningTime();
            if (openingTime < 1)
                return;
            int openingProgress = RFEClient.getFieldAttachmentsScreenOpeningProgress();
            int totalBars = RFEConfig.CLIENT.tooltipProgressBarLength.getAsInt();
            int progressBars = Math.min(totalBars, Mth.ceil((float) openingProgress / (float) openingTime * totalBars));
            int emptyBars = totalBars - progressBars;
            tooltipComponents.add(Component.literal("|".repeat(progressBars)).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("|".repeat(emptyBars)).withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            tooltipComponents.add(Component.translatable("ritchiesfirearmengine.tooltip.item.attachment_screen",
                            RFEClient.OPEN_ATTACHMENTS_SCREEN.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

}
