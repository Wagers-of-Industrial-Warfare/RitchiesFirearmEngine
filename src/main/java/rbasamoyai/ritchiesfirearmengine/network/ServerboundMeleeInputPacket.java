package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

import java.util.concurrent.Executor;

public record ServerboundMeleeInputPacket(boolean toggleMelee) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundMeleeInputPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ServerboundMeleeInputPacket::new, ServerboundMeleeInputPacket::toggleMelee).cast();

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player == null)
            return;
        // TODO offhand
        ItemStack mainhandItem = player.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem mainFirearm) {
            mainFirearm.handleMeleeInput(mainhandItem, player, InteractionHand.MAIN_HAND, this.toggleMelee);
        }
    }

}
