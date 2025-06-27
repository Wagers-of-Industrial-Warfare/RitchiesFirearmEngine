package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.server.level.ServerPlayer;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItemPropertiesHandler;

public class RFECommonEvents {

    public static void onDatapackReload() {
        MagazineItemPropertiesHandler.syncToAll();
        AmmoPacketItemPropertiesHandler.syncToAll();
    }

    public static void onDatapackSync(ServerPlayer player) {
        MagazineItemPropertiesHandler.syncToPlayer(player);
        AmmoPacketItemPropertiesHandler.syncToPlayer(player);
    }

}
