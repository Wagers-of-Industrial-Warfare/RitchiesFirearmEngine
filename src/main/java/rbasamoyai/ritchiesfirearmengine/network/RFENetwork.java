package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItemPropertiesHandler.ClientboundSyncAmmoPacketPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItemPropertiesHandler.ClientboundSyncMagazinePropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler.ClientboundSyncFirearmAmmoPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler.ClientboundSyncFirearmHandlingPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler.ClientboundSyncHitMultipliersPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundRemoveAllProjectilesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundRemoveRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundSpawnRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundUpdateRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler.ClientboundSyncRFEProjectileTypesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler.ClientboundSyncProjectilePenetrationPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler.ClientboundSyncRecoilProvidersPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler.ClientboundSyncSpreadProvidersPacket;

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

        buildMessage(network, id++, ClientboundValidateRFEContentPacksPacket.class, ClientboundValidateRFEContentPacksPacket::decode);
        buildMessage(network, id++, ServerboundFirearmActionPacket.class, ServerboundFirearmActionPacket::new);
        buildMessage(network, id++, ServerboundSetAttackKeyPacket.class, ServerboundSetAttackKeyPacket::new);
        buildMessage(network, id++, ServerboundRunFiringLogicPacket.class, ServerboundRunFiringLogicPacket::decode);
        buildMessage(network, id++, ClientboundSyncMagazinePropertiesPacket.class, ClientboundSyncMagazinePropertiesPacket::decode);
        buildMessage(network, id++, ClientboundSyncAmmoPacketPropertiesPacket.class, ClientboundSyncAmmoPacketPropertiesPacket::decode);
        buildMessage(network, id++, ClientboundSpawnRFEProjectilePacket.class, ClientboundSpawnRFEProjectilePacket::decode);
        buildMessage(network, id++, ClientboundUpdateRFEProjectilePacket.class, ClientboundUpdateRFEProjectilePacket::decode);
        buildMessage(network, id++, ClientboundRemoveRFEProjectilePacket.class, ClientboundRemoveRFEProjectilePacket::decode);
        buildMessage(network, id++, ClientboundRemoveAllProjectilesPacket.class, ClientboundRemoveAllProjectilesPacket::decode);
        buildMessage(network, id++, ClientboundSyncRFEProjectileTypesPacket.class, ClientboundSyncRFEProjectileTypesPacket::decode);
        buildMessage(network, id++, ClientboundSyncFirearmAmmoPropertiesPacket.class, ClientboundSyncFirearmAmmoPropertiesPacket::decode);
        buildMessage(network, id++, ClientboundSyncFirearmHandlingPropertiesPacket.class, ClientboundSyncFirearmHandlingPropertiesPacket::decode);
        buildMessage(network, id++, ClientboundSyncSpreadProvidersPacket.class, ClientboundSyncSpreadProvidersPacket::decode);
        buildMessage(network, id++, ClientboundSyncRecoilProvidersPacket.class, ClientboundSyncRecoilProvidersPacket::decode);
        buildMessage(network, id++, ClientboundSyncHitMultipliersPacket.class, ClientboundSyncHitMultipliersPacket::decode);
        buildMessage(network, id++, ClientboundRunFiringLogicPacket.class, ClientboundRunFiringLogicPacket::decode);
        buildMessage(network, id++, ClientboundSyncProjectilePenetrationPacket.class, ClientboundSyncProjectilePenetrationPacket::decode);

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

    public static <MSG extends RFEPacket> void sendToAllInDimension(MSG msg, Level level) {
        NETWORK.send(PacketDistributor.DIMENSION.with(level::dimension), msg);
    }

    public static void init() {}

}
