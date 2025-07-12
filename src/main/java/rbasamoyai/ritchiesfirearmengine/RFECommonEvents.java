package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.network.ClientboundValidateRFEContentPacksPacket;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;

public class RFECommonEvents {

    public static void loadTagsAndTypes() {
        RFEFirearmAmmoHandler.loadProjectileTypes();
    }

    public static void onDatapackReload(boolean singleplayer) {
        loadTagsAndTypes();
        RFEProjectileManager.clearAllProjectiles();
        RFENetwork.sendToAll(new RFEProjectileManager.ClientboundRemoveAllProjectilesPacket());

        if (singleplayer)
            return;
        RFEProjectileTypeHandler.syncToAll();
        MagazineItemPropertiesHandler.syncToAll();
        AmmoPacketItemPropertiesHandler.syncToAll();
        RFEFirearmAmmoHandler.syncToAll();
        RFEFirearmHandlingPropertiesHandler.syncToAll();
    }

    public static void onDatapackSync(ServerPlayer player, boolean singleplayer) {
        if (singleplayer)
            return;
        RFEProjectileTypeHandler.syncToPlayer(player);
        MagazineItemPropertiesHandler.syncToPlayer(player);
        AmmoPacketItemPropertiesHandler.syncToPlayer(player);
        RFEFirearmAmmoHandler.syncToPlayer(player);
        RFEFirearmHandlingPropertiesHandler.syncToPlayer(player);
    }

    public static void onLevelLoad(LevelAccessor level) {
        if (level.getServer() != null && !level.isClientSide() && level.getServer().overworld() == level)
            loadTagsAndTypes();
    }

    public static void onLevelUnload(LevelAccessor level) {
        RFEProjectileManager.onLevelUnload(level);
    }

    public static void onEntityJoin(Entity entity, Level level) {
        if (!level.isClientSide && entity instanceof ServerPlayer player)
            RFEProjectileManager.syncAllProjectilesToPlayer(player, level);
    }

    public static void onPlayerLoggedIn(Player entity) {
        if (entity instanceof ServerPlayer splayer)
            RFENetwork.sendToPlayer(new ClientboundValidateRFEContentPacksPacket(RFEPackLoader.getPackVersions()), splayer);
    }

    public static void onLevelTick(Level level) {
        RFEProjectileManager.tick(level);
    }

}
