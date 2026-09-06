package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.SlotConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;

import javax.annotation.Nullable;
import java.util.Set;

public class RFEItemAttachmentSlot extends Slot {

    private final Slot parentSlot;
    private final ResourceLocation slotId;
    private final SlotConfig slotConfig;
    private ItemStack storedStack;
    @Nullable
    private final LivingEntity owner;

    public RFEItemAttachmentSlot(Slot parentSlot, ResourceLocation slotId, int x, int y, SlotConfig slotConfig, @Nullable LivingEntity owner) {
        super(new SimpleContainer(0), 0, x, y);
        this.parentSlot = parentSlot;
        this.slotId = slotId;
        this.slotConfig = slotConfig;
        ItemStack targetStack = this.parentSlot.getItem();
        this.storedStack = targetStack.getItem() instanceof IHasRFEItemAttachments attachments
                ? attachments.getAttachmentInSlot(targetStack, this.slotId) : ItemStack.EMPTY;
        this.owner = owner;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        ItemStack targetStack = this.parentSlot.getItem();
        if (this.slotConfig.disabled() || !(targetStack.getItem() instanceof IHasRFEItemAttachments) || !this.getBlockingSlots().isEmpty())
            return false;
        return RFEItemAttachmentsPropertiesHandler.getData(targetStack, stack, this.slotId) != null;
    }

    @Override public boolean mayPickup(Player player) { return !this.slotConfig.disabled(); }

    @Override public ItemStack getItem() { return this.storedStack; }

    @Override
    public void set(ItemStack stack) {
        this.storedStack = stack;
        this.setChanged();
    }

    @Override
    public void setChanged() {
        ItemStack targetStack = this.parentSlot.getItem();
        if (targetStack.getItem() instanceof IHasRFEItemAttachments attachments)
            attachments.setAttachment(targetStack, this.slotId, this.storedStack);
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        super.setByPlayer(newStack, oldStack);
        if (this.owner == null || !this.owner.level().isClientSide)
            return;
        if (newStack.isEmpty()) {
            if (this.slotConfig.soundOnRemove() != null)
                this.owner.playSound(this.slotConfig.soundOnRemove());
        } else if (this.slotConfig.soundOnAdd() != null) {
            this.owner.playSound(this.slotConfig.soundOnAdd());
        }
    }

    @Override public int getMaxStackSize() { return 1; } // This may change if it's something like internal magazines.

    @Override
    public ItemStack remove(int amount) {
        ItemStack ret = ItemStack.EMPTY;
        if (amount > 0) {
            ret = this.storedStack;
            this.storedStack = ItemStack.EMPTY;
        }
        return ret;
    }

    @Override public int getSlotIndex() { return 0; }

    @Override
    public boolean isSameInventory(Slot other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof RFEItemAttachmentSlot otherAttachmentSlot)) {
            return false;
        } else {
            return otherAttachmentSlot.parentSlot == this.parentSlot;
        }
    }

    @Override public int getContainerSlot() { return 0; }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.slotConfig.emptyIcon() == null ? null : Pair.of(InventoryMenu.BLOCK_ATLAS, this.slotConfig.emptyIcon());
    }

    @Nullable public String getEmptyTextKey() { return this.slotConfig.emptyText(); }

    public ResourceLocation getSlotId() { return this.slotId; }

    public Set<ResourceLocation> getBlockingSlots() {
        ItemStack targetStack = this.parentSlot.getItem();
        return targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments ?
                hasAttachments.getBlockingSlots(targetStack, this.getSlotId()) : Set.of();
    }

}
