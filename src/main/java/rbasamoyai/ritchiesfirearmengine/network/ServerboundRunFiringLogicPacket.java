package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.IFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFiringInput;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

public record ServerboundRunFiringLogicPacket(List<RFEFiringInput> firingInputs, boolean jam, InteractionHand hand, @Nullable UUID recoilUUID) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRunFiringLogicPacket> STREAM_CODEC = StreamCodec.composite(
            RFEFiringInput.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerboundRunFiringLogicPacket::firingInputs,
            ByteBufCodecs.BOOL, ServerboundRunFiringLogicPacket::jam,
            NeoForgeStreamCodecs.enumCodec(InteractionHand.class), ServerboundRunFiringLogicPacket::hand,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).map(o -> o.orElse(null), Optional::ofNullable), ServerboundRunFiringLogicPacket::recoilUUID,
            ServerboundRunFiringLogicPacket::new);

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player == null)
            return;
        ItemStack itemStack = player.getItemInHand(this.hand);
        if (itemStack.getItem() instanceof IFirearmItem firearm)
            firearm.handleClientFireInputOnServer(itemStack, player, this.firingInputs, this.jam, this.recoilUUID, this.hand);
    }

}
