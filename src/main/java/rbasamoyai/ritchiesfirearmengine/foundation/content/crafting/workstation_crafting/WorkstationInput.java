package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record WorkstationInput(List<ItemStack> items) implements RecipeInput {

    public static final WorkstationInput EMPTY = new WorkstationInput(NonNullList.create());

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || this.items.size() <= index)
            throw new IllegalArgumentException("No item for index " + index);
        return this.items.get(index);
    }

    @Override public int size() { return this.items.size(); }

}
