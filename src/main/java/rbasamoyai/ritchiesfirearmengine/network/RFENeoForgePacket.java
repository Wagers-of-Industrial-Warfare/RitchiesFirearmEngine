package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

public record RFENeoForgePacket(RFEPacket pkt) implements CustomPacketPayload {

    public static final Type<RFENeoForgePacket> TYPE = new Type<>(RitchiesFirearmEngine.resource("neoforge_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFENeoForgePacket> STREAM_CODEC = RFENetwork.PACKET_STREAM_CODEC
        .map(RFENeoForgePacket::new, RFENeoForgePacket::pkt);

    public static void handlePacket(RFENeoForgePacket nfPkt, IPayloadContext ctx) {
        nfPkt.pkt.handle(ctx::enqueueWork, ctx.listener(), ctx.player());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

}
