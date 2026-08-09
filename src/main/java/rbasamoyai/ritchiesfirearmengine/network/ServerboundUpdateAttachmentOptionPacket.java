package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.IRFEItemAttachmentsMenu;

import java.util.concurrent.Executor;

public record ServerboundUpdateAttachmentOptionPacket(ResourceLocation slotId, int option) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundUpdateAttachmentOptionPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ServerboundUpdateAttachmentOptionPacket::slotId,
            ByteBufCodecs.VAR_INT, ServerboundUpdateAttachmentOptionPacket::option,
            ServerboundUpdateAttachmentOptionPacket::new);

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (!(player.containerMenu instanceof IRFEItemAttachmentsMenu attachmentsMenu))
            return;
        attachmentsMenu.modifyAttachmentOption(player, this.slotId, this.option);
    }
}
