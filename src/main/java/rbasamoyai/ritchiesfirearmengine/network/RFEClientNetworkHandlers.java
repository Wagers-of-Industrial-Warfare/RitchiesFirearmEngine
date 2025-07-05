package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;

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

}
