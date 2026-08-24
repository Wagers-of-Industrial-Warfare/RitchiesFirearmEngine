package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

public record ClientboundRunFiringLogicPacket(InteractionHand hand, RFERecoilClientImpulse recoil, @Nullable UUID recoilUUID, boolean inBipodPosition) implements RFEPacket {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRunFiringLogicPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(InteractionHand.class), ClientboundRunFiringLogicPacket::hand,
            RFERecoilClientImpulse.STREAM_CODEC, ClientboundRunFiringLogicPacket::recoil,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).map(o -> o.orElse(null), Optional::ofNullable), ClientboundRunFiringLogicPacket::recoilUUID,
            ByteBufCodecs.BOOL, ClientboundRunFiringLogicPacket::inBipodPosition,
            ClientboundRunFiringLogicPacket::new);

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.handleAutomaticFire(this));
    }

}
