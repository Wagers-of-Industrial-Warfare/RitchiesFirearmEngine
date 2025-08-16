package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.Executor;

public record ClientboundRunFiringLogicPacket(InteractionHand hand, RFERecoilClientImpulse recoil, @Nullable UUID recoilUUID) implements RFEPacket {

    public static ClientboundRunFiringLogicPacket decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        RFEAimAngles aimRecoil = new RFEAimAngles(buf.readFloat(), buf.readFloat());
        RFEAimAngles cameraRecoil = new RFEAimAngles(buf.readFloat(), buf.readFloat());
        float roll = buf.readFloat();
        UUID uuid = buf.readBoolean() ? buf.readUUID() : null;
        return new ClientboundRunFiringLogicPacket(hand, new RFERecoilClientImpulse(aimRecoil, cameraRecoil, roll), uuid);
    }

    @Override
    public void rootEncode(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand)
                .writeFloat(this.recoil.aimRecoil().pitch())
                .writeFloat(this.recoil.aimRecoil().yaw())
                .writeFloat(this.recoil.cameraRecoil().pitch())
                .writeFloat(this.recoil.cameraRecoil().yaw())
                .writeFloat(this.recoil.cameraRoll());
        buf.writeBoolean(this.recoilUUID != null);
        if (this.recoilUUID != null)
            buf.writeUUID(this.recoilUUID);
    }

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.handleAutomaticFire(this));
    }

}
