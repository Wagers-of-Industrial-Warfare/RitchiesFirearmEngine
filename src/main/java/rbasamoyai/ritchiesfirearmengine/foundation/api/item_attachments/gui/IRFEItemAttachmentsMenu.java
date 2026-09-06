package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import net.minecraft.world.entity.player.Player;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundUpdateAttachmentOptionPacket;

public interface IRFEItemAttachmentsMenu {

    void modifyAttachmentOption(Player player, ServerboundUpdateAttachmentOptionPacket packet);

}
