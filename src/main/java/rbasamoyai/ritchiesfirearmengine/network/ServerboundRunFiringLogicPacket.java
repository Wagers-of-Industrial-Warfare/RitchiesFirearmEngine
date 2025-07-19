package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.IFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFiringInput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public record ServerboundRunFiringLogicPacket(List<RFEFiringInput> firingInputs, boolean jam, InteractionHand hand) implements RFEPacket {

    public static ServerboundRunFiringLogicPacket decode(FriendlyByteBuf buf) {
        int sz = buf.readVarInt();
        List<RFEFiringInput> firingInputs = new ArrayList<>();
        for (int i = 0; i < sz; ++i)
            firingInputs.add(RFEFiringInput.fromNetwork(buf));
        boolean jam = buf.readBoolean();
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new ServerboundRunFiringLogicPacket(firingInputs, jam, hand);
    }

    @Override
    public void rootEncode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.firingInputs.size());
        for (RFEFiringInput input : this.firingInputs)
            RFEFiringInput.toNetwork(buf, input);
        buf.writeBoolean(this.jam);
        buf.writeEnum(this.hand);
    }

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        if (sender == null)
            return;
        ItemStack itemStack = sender.getItemInHand(this.hand);
        if (itemStack.getItem() instanceof IFirearmItem firearm)
            firearm.handleClientFireInputOnServer(itemStack, sender, this.firingInputs, this.jam);
    }

}
