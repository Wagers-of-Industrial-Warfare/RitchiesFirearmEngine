package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationMenus;

public class RFEItemAttachmentsMenu extends AbstractContainerMenu {

    private final int selected;

    public static RFEItemAttachmentsMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new RFEItemAttachmentsMenu(FoundationMenus.ATTACHMENTS_MENU.get(), containerId, inventory, buf.readVarInt());
    }

    public static RFEItemAttachmentsMenu server(int containerId, Inventory inventory, Player player) {
        return new RFEItemAttachmentsMenu(FoundationMenus.ATTACHMENTS_MENU.get(), containerId, inventory, inventory.selected);
    }

    protected RFEItemAttachmentsMenu(MenuType<? extends RFEItemAttachmentsMenu> menuType, int containerId,
                                     Inventory inventory, int selected) {
        super(menuType, containerId);
        this.selected = selected;

        for (int rowInv = 0; rowInv < 3; rowInv++) {
            for (int colInv = 0; colInv < 9; colInv++) {
                this.addSlot(new Slot(inventory, colInv + (rowInv + 1) * 9, 8 + colInv * 18, 118 + rowInv * 18));
            }
        }

        for (int hotbarCol = 0; hotbarCol < 9; hotbarCol++) {
            if (hotbarCol != this.selected)
                this.addSlot(new Slot(inventory, hotbarCol, 8 + hotbarCol * 18, 176));
        }
        this.addSlot(new Slot(inventory, this.selected, 8 + this.selected * 18, 176) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });

        // TODO add attachment slots (both active and inactive)
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.getFocusedAttachmentsItem(player.getInventory()).getItem() instanceof IHasRFEItemAttachments;
    }

    public ItemStack getFocusedAttachmentsItem(Inventory inventory) {
        return inventory.getItem(this.selected);
    }

    public int getSelectedIndex() { return this.selected; }

}
