package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.Executor;

public interface RFEPacket {

    void handle(Executor exec, PacketListener listener, Player player);

}
