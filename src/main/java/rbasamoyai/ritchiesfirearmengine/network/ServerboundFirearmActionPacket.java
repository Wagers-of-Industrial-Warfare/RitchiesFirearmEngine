package rbasamoyai.ritchiesfirearmengine.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.AmmoPacketItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

import java.util.concurrent.Executor;

public record ServerboundFirearmActionPacket(RFEFirearmItem.Action action) implements RFEPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundFirearmActionPacket> STREAM_CODEC =
            NeoForgeStreamCodecs.enumCodec(RFEFirearmItem.Action.class).map(ServerboundFirearmActionPacket::new, ServerboundFirearmActionPacket::action).cast();

    @Override
    public void handle(Executor exec, PacketListener listener, Player player) {
        if (player == null)
            return;
        // TODO offhand
        ItemStack mainhandItem = player.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem mainFirearm) {
            switch (this.action) {
                case RELOAD -> mainFirearm.onReload(mainhandItem, player);
                case UNLOAD -> mainFirearm.onUnload(mainhandItem, player);
                case SWITCH_MODE -> mainFirearm.onSwitchMode(mainhandItem, player);
            }
        }
        if (mainhandItem.getItem() instanceof MagazineItem magazine) {
            switch (this.action) {
                case RELOAD -> magazine.tryReloadingOutsideOfMenu(mainhandItem, player);
                case UNLOAD -> magazine.tryUnloadingOutsideOfMenu(mainhandItem, player);
            }
        }
        if (mainhandItem.getItem() instanceof AmmoPacketItem ammoPacket) {
            switch (this.action) {
                case RELOAD -> ammoPacket.tryReloadingOutsideOfMenu(mainhandItem, player);
            }
        }
    }

}
