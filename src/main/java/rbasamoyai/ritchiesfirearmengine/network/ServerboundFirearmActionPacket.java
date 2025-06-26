package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

public record ServerboundFirearmActionPacket(RFEFirearmItem.Action action) implements RFEPacket {

    public ServerboundFirearmActionPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(RFEFirearmItem.Action.class));
    }

    @Override
    public void rootEncode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
    }

    @Override
    public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
        if (sender == null)
            return;
        ItemStack mainhandItem = sender.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem mainFirearm) {
            switch (this.action) {
                case RELOAD -> mainFirearm.onReload(mainhandItem, sender);
                case UNLOAD -> mainFirearm.onUnload(mainhandItem, sender);
                case SWITCH_MODE -> mainFirearm.onSwitchMode(mainhandItem, sender);
            }
        }
        if (mainhandItem.getItem() instanceof MagazineItem magazine) {
            switch (this.action) {
                case RELOAD -> magazine.tryReloadingOutsideOfMenu(mainhandItem, sender);
                case UNLOAD -> magazine.tryUnloadingOutsideOfMenu(mainhandItem, sender);
            }
        }
        if (mainhandItem.getItem() instanceof AmmoPacketItem ammoPacket) {
            switch (this.action) {
                case RELOAD -> ammoPacket.tryReloadingOutsideOfMenu(mainhandItem, sender);
            }
        }
    }

}
