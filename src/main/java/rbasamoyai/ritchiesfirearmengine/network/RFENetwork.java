package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItemPropertiesHandler.ClientboundSyncAmmoPacketPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItemPropertiesHandler.ClientboundSyncMagazinePropertiesPacket;

import java.util.function.Function;
import java.util.function.Supplier;

public class RFENetwork {

    private static SimpleChannel NETWORK = construct();
    public static final String VERSION = "1.0.0";

    private static SimpleChannel construct() {
        SimpleChannel network = NetworkRegistry.ChannelBuilder.named(RitchiesFirearmEngine.resource("network"))
                .clientAcceptedVersions(VERSION::equals)
                .serverAcceptedVersions(VERSION::equals)
                .networkProtocolVersion(() -> VERSION)
                .simpleChannel();

        int id = 0;

        buildMessage(network, id++, ServerboundFirearmActionPacket.class, ServerboundFirearmActionPacket::new);
        buildMessage(network, id++, ServerboundReleaseAttackKeyPacket.class, ServerboundReleaseAttackKeyPacket::new);
        buildMessage(network, id++, ClientboundSyncMagazinePropertiesPacket.class, ClientboundSyncMagazinePropertiesPacket::decode);
        buildMessage(network, id++, ClientboundSyncAmmoPacketPropertiesPacket.class, ClientboundSyncAmmoPacketPropertiesPacket::decode);

        return network;
    }

    private static <MSG extends RFEPacket> void buildMessage(SimpleChannel network, int id, Class<MSG> msg, Function<FriendlyByteBuf, MSG> decoder) {
        network.messageBuilder(msg, id)
                .decoder(decoder)
                .encoder(RFEPacket::rootEncode)
                .consumerMainThread(RFENetwork::consumeRFEPacket)
                .add();
    }

    private static <MSG extends RFEPacket> void consumeRFEPacket(MSG packet, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        packet.handle(ctx::enqueueWork, ctx.getNetworkManager().getPacketListener(), ctx.getSender());
        ctx.setPacketHandled(true);
    }

    public static <MSG extends RFEPacket> void sendToServer(MSG msg) { NETWORK.sendToServer(msg); }

    public static <MSG extends RFEPacket> void sendToPlayer(MSG msg, ServerPlayer player) {
        NETWORK.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static <MSG extends RFEPacket> void sendToAll(MSG msg) {
        NETWORK.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void init() {}

}
