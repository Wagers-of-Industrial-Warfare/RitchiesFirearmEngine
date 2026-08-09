package rbasamoyai.ritchiesfirearmengine.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItemPropertiesHandler.ClientboundSyncAmmoPacketPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItemPropertiesHandler.ClientboundSyncMagazinePropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler.ClientboundSyncFirearmAmmoPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler.ClientboundSyncFirearmHandlingPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler.ClientboundSyncHitMultipliersPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.ClientboundSyncItemAttachmentsMenuSlotsPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler.ClientboundSyncItemAttachmentsPropertiesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundRemoveAllProjectilesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundRemoveRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundSpawnRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager.ClientboundUpdateRFEProjectilePacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler.ClientboundSyncRFEProjectileTypesPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler.ClientboundSyncProjectilePenetrationPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler.ClientboundSyncRecoilProvidersPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler.ClientboundSyncSpreadProvidersPacket;
import rbasamoyai.ritchiesfirearmengine.foundation.effects.explosions.QuietExplosion;

public class RFENetwork {

    private static final Int2ObjectMap<StreamCodec<RegistryFriendlyByteBuf, ? extends RFEPacket>> ID_TO_STREAM_CODEC = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<Class<? extends RFEPacket>> TYPE_TO_ID = new Object2IntOpenHashMap<>();

    public static final String VERSION = "2.0.0";

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEPacket> PACKET_STREAM_CODEC = ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast()
            .dispatch(RFENetwork::getPacketId, RFENetwork::getStreamCodec);

    public static void init() {
        int id = 0;

        addMessage(id++, ClientboundValidateRFEContentPacksPacket.class, ClientboundValidateRFEContentPacksPacket.STREAM_CODEC);
        addMessage(id++, ServerboundFirearmActionPacket.class, ServerboundFirearmActionPacket.STREAM_CODEC);
        addMessage(id++, ServerboundSetAttackKeyPacket.class, ServerboundSetAttackKeyPacket.STREAM_CODEC);
        addMessage(id++, ServerboundRunFiringLogicPacket.class, ServerboundRunFiringLogicPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncMagazinePropertiesPacket.class, ClientboundSyncMagazinePropertiesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncAmmoPacketPropertiesPacket.class, ClientboundSyncAmmoPacketPropertiesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSpawnRFEProjectilePacket.class, ClientboundSpawnRFEProjectilePacket.STREAM_CODEC);
        addMessage(id++, ClientboundUpdateRFEProjectilePacket.class, ClientboundUpdateRFEProjectilePacket.STREAM_CODEC);
        addMessage(id++, ClientboundRemoveRFEProjectilePacket.class, ClientboundRemoveRFEProjectilePacket.STREAM_CODEC);
        addMessage(id++, ClientboundRemoveAllProjectilesPacket.class, ClientboundRemoveAllProjectilesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncRFEProjectileTypesPacket.class, ClientboundSyncRFEProjectileTypesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncFirearmAmmoPropertiesPacket.class, ClientboundSyncFirearmAmmoPropertiesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncFirearmHandlingPropertiesPacket.class, ClientboundSyncFirearmHandlingPropertiesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncSpreadProvidersPacket.class, ClientboundSyncSpreadProvidersPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncRecoilProvidersPacket.class, ClientboundSyncRecoilProvidersPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncHitMultipliersPacket.class, ClientboundSyncHitMultipliersPacket.STREAM_CODEC);
        addMessage(id++, ClientboundRunFiringLogicPacket.class, ClientboundRunFiringLogicPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncProjectilePenetrationPacket.class, ClientboundSyncProjectilePenetrationPacket.STREAM_CODEC);
        addMessage(id++, QuietExplosion.ClientboundExplosionPacket.class, QuietExplosion.ClientboundExplosionPacket.STREAM_CODEC);
        addMessage(id++, ServerboundMeleeInputPacket.class, ServerboundMeleeInputPacket.STREAM_CODEC);
        addMessage(id++, ServerboundOpenAttachmentsScreenPacket.class, ServerboundOpenAttachmentsScreenPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncItemAttachmentsPropertiesPacket.class, ClientboundSyncItemAttachmentsPropertiesPacket.STREAM_CODEC);
        addMessage(id++, ClientboundSyncItemAttachmentsMenuSlotsPacket.class, ClientboundSyncItemAttachmentsMenuSlotsPacket.STREAM_CODEC);
        addMessage(id++, ServerboundUpdateAttachmentOptionPacket.class, ServerboundUpdateAttachmentOptionPacket.STREAM_CODEC);
    }

    private static <MSG extends RFEPacket> void addMessage(int id, Class<MSG> clazz, StreamCodec<RegistryFriendlyByteBuf, MSG> streamCodec) {
        TYPE_TO_ID.put(clazz, id);
        ID_TO_STREAM_CODEC.put(id, streamCodec);
    }

    public static int getPacketId(RFEPacket pkt) {
        int id = TYPE_TO_ID.getOrDefault(pkt.getClass(), -1);
        if (id == -1)
            throw new IllegalStateException("Attempted to serialize packet with illegal id: " + id);
        return id;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ? extends RFEPacket> getStreamCodec(int id) {
        if (!ID_TO_STREAM_CODEC.containsKey(id))
            throw new IllegalStateException("Attempted to deserialize packet with illegal id: " + id);
        return ID_TO_STREAM_CODEC.get(id);
    }

    public static void sendToServer(RFEPacket msg) {
        PacketDistributor.sendToServer(new RFENeoForgePacket(msg));
    }

    public static void sendToPlayer(RFEPacket msg, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RFENeoForgePacket(msg));
    }

    public static void sendToAll(RFEPacket msg) {
        PacketDistributor.sendToAllPlayers(new RFENeoForgePacket(msg));
    }

    public static void sendToAllInDimension(RFEPacket msg, ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, new RFENeoForgePacket(msg));
    }

}
