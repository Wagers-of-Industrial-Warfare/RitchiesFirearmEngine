package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.reloading;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationBlocks;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationMenus;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

public class ReloadingStationMenu extends AbstractContainerMenu {

    public static ReloadingStationMenu client(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new ReloadingStationMenu(FoundationMenus.RELOADING_STATION.get(), containerId, inventory, inventory.player, ContainerLevelAccess.NULL);
    }

    public static ReloadingStationMenu server(int containerId, Inventory inventory, ContainerLevelAccess access) {
        return new ReloadingStationMenu(FoundationMenus.RELOADING_STATION.get(), containerId, inventory, inventory.player, access);
    }

    private final ContainerLevelAccess access;
    private final CraftingContainer ammoSlots = new TransientCraftingContainer(this, 5, 1);
    private final CraftingContainer magazineSlot = new TransientCraftingContainer(this, 1, 1);
    private final ResultContainer resultSlot = new ResultContainer();
    private final Player player;

    protected ReloadingStationMenu(MenuType<? extends ReloadingStationMenu> menuType, int containerId, Inventory inventory,
                                   Player player, ContainerLevelAccess access) {
        super(menuType, containerId);
        this.player = player;
        this.access = access;

        this.addSlot(new ReloadingResultSlot(this, this.resultSlot, 0, 107, 59));

        for (int i = 0; i < 5; i++)
            this.addSlot(new ReloadingIngredientSlot(this, this.ammoSlots, i, 44 + i * 18, 23, ReloadingIngredientSlot.Type.AMMO));
        this.addSlot(new ReloadingIngredientSlot(this, this.magazineSlot, 0, 53, 59, ReloadingIngredientSlot.Type.MAGAZINE));

        for (int rowIdx = 0; rowIdx < 3; rowIdx++) {
            for (int colIdx = 0; colIdx < 9; colIdx++)
                this.addSlot(new Slot(inventory, colIdx + rowIdx * 9 + 9, 8 + colIdx * 18, 91 + rowIdx * 18));
        }
        for (int hotbarIdx = 0; hotbarIdx < 9; hotbarIdx++)
            this.addSlot(new Slot(inventory, hotbarIdx, 8 + hotbarIdx * 18, 149));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack moveStack = slot.getItem();
            returnStack = moveStack.copy();
            if (index < 7) {
                if (!this.moveItemStackTo(moveStack, 7, 43, true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(moveStack, 1, 7, false))
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
        return stillValid(this.access, player, FoundationBlocks.RELOADING_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.ammoSlots);
            this.clearContainer(player, this.magazineSlot);
        });
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.ammoSlots || container == this.magazineSlot)
            this.access.execute((level, pos) -> this.onChangeIngredients(level));
    }

    private void onChangeIngredients(Level level) {
        if (level.isClientSide)
            return;
        ItemStack result = this.tryReloadingMagazine(true);
        this.resultSlot.setItem(0, result);
        ((ServerPlayer) this.player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, result));
    }

    protected ItemStack tryReloadingMagazine(boolean simulate) {
        ItemStack magazineStack = this.magazineSlot.getItem(0).copyWithCount(1);
        return magazineStack.getItem() instanceof MagazineItem magazineItem
                && magazineItem.tryReloadingInMenu(magazineStack, this.ammoSlots.getItems(), simulate, this::handleExtraItem)
                ? magazineStack : ItemStack.EMPTY;
    }

    protected void handleExtraItem(ItemStack itemStack) {
        if (!this.player.level().isClientSide)
            RFEItemUtils.addItemToEntity(itemStack, this.player);
    }

    protected void decrementMagazineSlot() { this.magazineSlot.removeItem(0, 1); }

}
