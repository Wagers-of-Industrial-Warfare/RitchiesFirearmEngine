package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;

import java.util.concurrent.Executor;

public record ServerboundSetAttackKeyPacket(boolean down) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetAttackKeyPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ServerboundSetAttackKeyPacket::new, ServerboundSetAttackKeyPacket::down).cast();

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player == null)
            return;
        ItemStack mainhandItem = player.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction interactable) {
            if (this.down) {
                interactable.onPressAttackKey(mainhandItem, player);
            } else {
                interactable.onReleaseAttackKey(mainhandItem, player);
            }
        }
    }
}
