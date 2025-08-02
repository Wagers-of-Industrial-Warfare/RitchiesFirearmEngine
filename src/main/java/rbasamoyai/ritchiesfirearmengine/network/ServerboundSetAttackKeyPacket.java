package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public record ServerboundSetAttackKeyPacket(boolean down) implements RFEPacket {

    public ServerboundSetAttackKeyPacket(FriendlyByteBuf buf) { this(buf.readBoolean()); }

    @Override
    public void rootEncode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.down);
    }

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        if (sender == null)
            return;
        ItemStack mainhandItem = sender.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction interactable) {
            if (this.down) {
                interactable.onPressAttackKey(mainhandItem, sender);
            } else {
                interactable.onReleaseAttackKey(mainhandItem, sender);
            }
        }
    }
}
