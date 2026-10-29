package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.reloading;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

public class ReloadingIngredientSlot extends Slot {

    private static final ResourceLocation AMMO_ICON = RitchiesFirearmEngine.resource("item/ammo_icon");
    private static final ResourceLocation MAGAZINE_ICON = RitchiesFirearmEngine.resource("item/magazine_icon");

    private final Type type;

    public ReloadingIngredientSlot(Container container, int slot, int x, int y, Type type) {
        super(container, slot, x, y);
        this.type = type;
        this.setBackground(InventoryMenu.BLOCK_ATLAS, this.type == Type.AMMO ? AMMO_ICON : MAGAZINE_ICON);
    }

    public Component getEmptyText() {
        return this.type == Type.AMMO
                ? Component.translatable("container.ritchiesfirearmengine.reloading_station.slot.ammunition")
                : Component.translatable("container.ritchiesfirearmengine.reloading_station.slot.magazine");
    }

    public enum Type {
        AMMO,
        MAGAZINE
    }

}
