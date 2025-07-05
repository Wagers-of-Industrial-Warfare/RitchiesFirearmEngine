package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public record ClientboundValidateRFEContentPacksPacket(Map<String, String> versions) implements RFEPacket {

    public static ClientboundValidateRFEContentPacksPacket decode(FriendlyByteBuf buf) {
        Map<String, String> versions = new LinkedHashMap<>();
        int sz = buf.readVarInt();
        for (int i = 0; i < sz; ++i) {
            String packId = buf.readUtf();
            String versionRange = buf.readUtf();
            versions.put(packId, versionRange);
        }
        return new ClientboundValidateRFEContentPacksPacket(versions);
    }

    @Override
    public void rootEncode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.versions.size());
        for (Map.Entry<String, String> entry : this.versions.entrySet())
            buf.writeUtf(entry.getKey()).writeUtf(entry.getValue());
    }

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.validateRFEContentPacks(this));
    }

}
