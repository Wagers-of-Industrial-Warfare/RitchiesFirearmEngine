package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.reloading;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReloadingResultSlot extends Slot {

    private final ReloadingStationMenu menu;

    public ReloadingResultSlot(ReloadingStationMenu menu, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    @Override public boolean mayPlace(ItemStack stack) { return false; }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.menu.tryReloadingMagazine(false);
        this.menu.decrementMagazineSlot();
    }

    @Override public boolean isFake() { return true; }

}
