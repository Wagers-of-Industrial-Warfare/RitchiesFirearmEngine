package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_compatibility.RFEPackMismatchDisconnectScreen;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;

public class RFEClientNetworkHandlers {

    public static void spawnRFEProjectile(RFEProjectileManager.ClientboundSpawnRFEProjectilePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != packet.level())
            return;
        if (packet.ownerId() != null) {
            Entity owner = mc.level.getEntity(packet.ownerId());
            packet.instance().setOwner(owner);
        }
        RFEProjectileManager.queueAddedProjectile(packet.instance(), mc.level);
    }

    public static void updateRFEProjectile(RFEProjectileManager.ClientboundUpdateRFEProjectilePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != packet.level())
            return;
        RFEProjectileManager.updateProjectile(packet.uuid(), packet.position(), packet.oldPosition(), packet.velocity(),
                packet.leftOwner(), packet.distanceTravelled(), packet.age(), mc.level);
    }

    public static void removeRFEProjectile(RFEProjectileManager.ClientboundRemoveRFEProjectilePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != packet.level())
            return;
        RFEProjectileManager.removeProjectile(packet.uuid(), mc.level);
    }

    public static void validateRFEContentPacks(ClientboundValidateRFEContentPacksPacket packet) {
        Map<String, String> clientVersions = RFEPackLoader.getPackVersions();
        if (packet.versions().equals(clientVersions))
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.level == null)
            return;
        mc.level.disconnect();
        Component reason = Component.literal("Different RFE pack versions on client and server, please ensure they are the same");
        mc.getConnection().onDisconnect(reason);

        Map<String, Tuple<String, String>> mismatched = RFEUtils.provideMismatchedRFEContentPacks(clientVersions, packet.versions());
        RitchiesFirearmEngine.LOGGER.error("Pack mismatch between client and server ({} pack(s)). See below:", mismatched.size());
        for (Map.Entry<String, Tuple<String, String>> entry : mismatched.entrySet()) {
            Tuple<String, String> versions = entry.getValue();
            String clientVersion = versions.getA() == null ? "(missing)" : versions.getA();
            String serverVersion = versions.getB() == null ? "(missing)" : versions.getB();
            RitchiesFirearmEngine.LOGGER.error("RFE Pack {}: {} on client, {} on server", entry.getKey(), clientVersion, serverVersion);
        }

        mc.setScreen(new RFEPackMismatchDisconnectScreen(new JoinMultiplayerScreen(new TitleScreen()), CommonComponents.CONNECT_FAILED, reason, mismatched));
    }

}
