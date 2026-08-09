package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.MenuTypeSlotsConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.SlotConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationMenus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RFEItemAttachmentsMenu extends AbstractContainerMenu {

    private final int selected;
    private final List<RFEItemAttachmentSlot> attachmentSlots = new ArrayList<>();

    public static RFEItemAttachmentsMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new RFEItemAttachmentsMenu(FoundationMenus.FIELD_ATTACHMENTS_MENU.get(), containerId, inventory, buf.readVarInt());
    }

    public static RFEItemAttachmentsMenu server(int containerId, Inventory inventory, Player player) {
        return new RFEItemAttachmentsMenu(FoundationMenus.FIELD_ATTACHMENTS_MENU.get(), containerId, inventory, inventory.selected);
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

        ItemStack targetStack = this.getFocusedAttachmentsItem(inventory);
        MenuTypeSlotsConfig menuConfig = RFEItemAttachmentsMenuSlotsHandler.getConfig(targetStack, this.getType());
        ImmutableMap<ResourceLocation, SlotConfig> slotConfigById = menuConfig.slotConfig();
        int i = 0;
        for (Map.Entry<ResourceLocation, SlotConfig> entry : slotConfigById.entrySet()) {
            int x = i / 6 * -18 - 17;
            int y = i % 6 * 18 + 1;
            this.addAttachmentSlot(new RFEItemAttachmentSlot(targetStack, entry.getKey(), x, y, entry.getValue()));
            ++i;
        }
    }

    private RFEItemAttachmentSlot addAttachmentSlot(RFEItemAttachmentSlot attachmentSlot) {
        this.attachmentSlots.add(attachmentSlot);
        this.addSlot(attachmentSlot);
        return attachmentSlot;
    }

    public void iterateAttachmentSlots(Consumer<RFEItemAttachmentSlot> cons) { this.attachmentSlots.forEach(cons); }

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
