package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.RFEAttachmentSlotType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.IRFEItemAttachmentsMenu;

import java.util.concurrent.Executor;

public record ServerboundUpdateAttachmentOptionPacket(ResourceLocation slotId, RFEAttachmentSlotType slotType, int option, boolean removed) implements RFEPacket {

    public static ServerboundUpdateAttachmentOptionPacket forItem(ResourceLocation slotId, int option) {
        return new ServerboundUpdateAttachmentOptionPacket(slotId, RFEAttachmentSlotType.ITEM, option, false);
    }

    public static ServerboundUpdateAttachmentOptionPacket forIntegral(ResourceLocation slotId, int option, boolean removed) {
        return new ServerboundUpdateAttachmentOptionPacket(slotId, RFEAttachmentSlotType.INTEGRAL, option, removed);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundUpdateAttachmentOptionPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ServerboundUpdateAttachmentOptionPacket::slotId,
            NeoForgeStreamCodecs.enumCodec(RFEAttachmentSlotType.class), ServerboundUpdateAttachmentOptionPacket::slotType,
            ByteBufCodecs.VAR_INT, ServerboundUpdateAttachmentOptionPacket::option,
            ByteBufCodecs.BOOL, ServerboundUpdateAttachmentOptionPacket::removed,
            ServerboundUpdateAttachmentOptionPacket::new);

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player.containerMenu instanceof IRFEItemAttachmentsMenu attachmentsMenu)
            attachmentsMenu.modifyAttachmentOption(player, this);
    }

}
