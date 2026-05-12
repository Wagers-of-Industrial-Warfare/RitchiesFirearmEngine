package rbasamoyai.ritchiesfirearmengine.foundation.effects.explosions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface RFECustomExplosion {

    void playLocalSound(Level level, double x, double y, double z);
    void sendToServerPlayer(ServerPlayer player);

}
