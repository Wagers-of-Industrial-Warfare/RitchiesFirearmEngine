package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;

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
        RFEProjectileManager.updateProjectile(packet.uuid(), packet.position(), packet.velocity(), packet.leftOwner(),
                packet.distanceTravelled(), packet.age(), mc.level);
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
        mc.getConnection().onDisconnect(Component.literal("Different RFE pack versions on client and server, please ensure they are the same"));
        // TODO screen
    }

}
