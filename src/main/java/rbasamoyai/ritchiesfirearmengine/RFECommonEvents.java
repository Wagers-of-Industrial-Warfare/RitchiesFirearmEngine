package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.projectiles.RFEProjectileTypeHandler;

public class RFECommonEvents {

    public static void onDatapackReload() {
        RFEProjectileManager.clearAllProjectiles();
        RFENetwork.sendToAll(new RFEProjectileManager.ClientboundRemoveAllProjectilesPacket());

        RFEProjectileTypeHandler.syncToAll();
        MagazineItemPropertiesHandler.syncToAll();
        AmmoPacketItemPropertiesHandler.syncToAll();
    }

    public static void onDatapackSync(ServerPlayer player) {
        RFEProjectileTypeHandler.syncToPlayer(player);
        MagazineItemPropertiesHandler.syncToPlayer(player);
        AmmoPacketItemPropertiesHandler.syncToPlayer(player);
    }

    public static void onLevelUnload(LevelAccessor level) {
        RFEProjectileManager.onLevelUnload(level);
    }

    public static void onEntityJoin(Entity entity, Level level) {
        if (!level.isClientSide && entity instanceof ServerPlayer player)
            RFEProjectileManager.syncAllProjectilesToPlayer(player, level);
    }

    public static void onLevelTick(Level level) {
        RFEProjectileManager.tick(level);
    }

}
