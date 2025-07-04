package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public final class ServerboundReleaseAttackKeyPacket implements RFEPacket {

    public ServerboundReleaseAttackKeyPacket() {}
    public ServerboundReleaseAttackKeyPacket(FriendlyByteBuf buf) {}

    @Override public void rootEncode(FriendlyByteBuf buf) {}

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        if (sender == null)
            return;
        ItemStack mainhandItem = sender.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction interactable)
            interactable.onReleaseAttackKey(mainhandItem, sender);
    }
}
