package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IRFEItemAttachmentsMenu {

    void modifyAttachmentOption(Player player, ResourceLocation slotId, int option);

}
