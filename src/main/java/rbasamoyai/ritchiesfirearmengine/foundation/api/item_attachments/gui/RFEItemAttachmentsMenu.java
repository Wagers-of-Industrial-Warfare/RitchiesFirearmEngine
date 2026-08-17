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
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationMenus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RFEItemAttachmentsMenu extends AbstractContainerMenu implements IRFEItemAttachmentsMenu {

    private final int selected;
    private final List<RFEItemAttachmentSlot> attachmentSlots = new ArrayList<>();
    private final Slot targetSlot;

    public static RFEItemAttachmentsMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new RFEItemAttachmentsMenu(FoundationMenus.FIELD_ATTACHMENTS_MENU.get(), containerId, inventory, buf.readVarInt());
    }

    public static RFEItemAttachmentsMenu server(int containerId, Inventory inventory, Player player, int selected) {
        return new RFEItemAttachmentsMenu(FoundationMenus.FIELD_ATTACHMENTS_MENU.get(), containerId, inventory, selected);
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
        int selectedX;
        int selectedY;
        if (this.selected < 9) {
            selectedX = 8 + this.selected * 18;
            selectedY = 176;
        } else {
            int row = Math.floorDiv(this.selected, 9) - 1;
            int col = this.selected % 9;
            selectedX = 8 + col * 18;
            selectedY = 118 + row * 18;
        }
        this.targetSlot = this.addSlot(new Slot(inventory, this.selected, selectedX, selectedY) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });

        ItemStack targetStack = this.getFocusedAttachmentsItem();
        MenuTypeSlotsConfig menuConfig = RFEItemAttachmentsMenuSlotsHandler.getConfig(targetStack, this.getType());
        ImmutableMap<ResourceLocation, SlotConfig> slotConfigById = menuConfig.slotConfig();
        int i = 0;
        for (Map.Entry<ResourceLocation, SlotConfig> entry : slotConfigById.entrySet()) {
            int x = i / 6 * -18 - 17;
            int y = i % 6 * 18 + 1;
            this.addAttachmentSlot(new RFEItemAttachmentSlot(targetStack, entry.getKey(), x, y, entry.getValue(), inventory.player));
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
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack moveStack = slot.getItem();
            returnStack = moveStack.copy();
            if (index < 36) {
                boolean placed = false;
                for (int i = 36; i < this.slots.size(); ++i) {
                    if (!(this.slots.get(i) instanceof RFEItemAttachmentSlot attachmentSlot))
                        continue;
                    if (attachmentSlot.mayPlace(moveStack) && this.moveItemStackTo(moveStack, i, i + 1, false))
                        placed = true;
                }
                if (!placed)
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(moveStack, this.selected + 1, 36, true)
                        && !this.moveItemStackTo(moveStack, 0, this.selected, true))
                    return ItemStack.EMPTY;
            }

            if (moveStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (moveStack.getCount() == returnStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, returnStack);
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.targetSlot.getItem().getItem() instanceof IHasRFEItemAttachments;
    }

    public ItemStack getFocusedAttachmentsItem() { return this.targetSlot.getItem(); }

    public int getSelectedIndex() { return this.selected; }

    public Slot getTargetSlot() { return this.targetSlot; }

    @Override
    public void modifyAttachmentOption(Player player, ResourceLocation slotId, int option) {
        ItemStack itemStack = this.targetSlot.getItem();
        if (!(itemStack.getItem() instanceof IHasRFEItemAttachments hasAttachments))
            return;
        ItemStack attachmentStack = hasAttachments.getAttachmentInSlot(itemStack, slotId);
        RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(itemStack, attachmentStack, slotId);
        if (attachmentProperties == null)
            return;
        attachmentProperties.acceptAttachmentConfigOption(attachmentStack, option);
        hasAttachments.setAttachment(itemStack, slotId, attachmentStack);
        this.targetSlot.set(itemStack);
    }

}
