package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import java.util.LinkedHashMap;
import java.util.concurrent.Executor;

public record ClientboundValidateRFEContentPacksPacket(LinkedHashMap<String, String> versions) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundValidateRFEContentPacksPacket> STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)
                    .map(ClientboundValidateRFEContentPacksPacket::new, ClientboundValidateRFEContentPacksPacket::versions).cast();

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.validateRFEContentPacks(this));
    }

}
