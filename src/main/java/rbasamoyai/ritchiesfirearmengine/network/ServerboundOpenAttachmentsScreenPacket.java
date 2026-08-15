package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsMenu;

import java.util.concurrent.Executor;

public record ServerboundOpenAttachmentsScreenPacket(int slot) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenAttachmentsScreenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundOpenAttachmentsScreenPacket::slot,
            ServerboundOpenAttachmentsScreenPacket::new);

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player.containerMenu instanceof RFEItemAttachmentsMenu)
            return;
        ItemStack itemStack = player.getInventory().getItem(this.slot);
        if (!(itemStack.getItem() instanceof IHasRFEItemAttachments))
            return;
        if (player instanceof ServerPlayer splayer)
            splayer.openMenu(new SimpleMenuProvider((id, inv, p) -> RFEItemAttachmentsMenu.server(id, inv, p, this.slot),
                            Component.translatable("gui.ritchiesfirearmengine.attachments_menu")),
                    buf -> buf.writeVarInt(this.slot));
    }

}
