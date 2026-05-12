package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import java.util.concurrent.Executor;

public interface RFEExplosionPacket<T extends Explosion> extends RFEPacket {

    T getExplosion(Level level);
    Vec3 getKnockback();

    @Override
    default void handle(Executor exec, PacketListener listener, Player player) {
        EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.handleExplosionPacket(this));
    }

}
