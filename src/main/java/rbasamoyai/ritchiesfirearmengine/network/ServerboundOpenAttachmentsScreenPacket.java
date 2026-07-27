package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsMenu;

import java.util.concurrent.Executor;

public class ServerboundOpenAttachmentsScreenPacket implements RFEPacket {

    public static final ServerboundOpenAttachmentsScreenPacket INSTANCE = new ServerboundOpenAttachmentsScreenPacket();
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenAttachmentsScreenPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ServerboundOpenAttachmentsScreenPacket() {}

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player.containerMenu instanceof RFEItemAttachmentsMenu)
            return;
        if (!(player.getMainHandItem().getItem() instanceof IHasRFEItemAttachments))
            return;
        if (player instanceof ServerPlayer splayer)
            splayer.openMenu(new SimpleMenuProvider(RFEItemAttachmentsMenu::server, Component.translatable("gui.ritchiesfirearmengine.attachments_menu")),
                    buf -> buf.writeVarInt(player.getInventory().selected));
    }

}
