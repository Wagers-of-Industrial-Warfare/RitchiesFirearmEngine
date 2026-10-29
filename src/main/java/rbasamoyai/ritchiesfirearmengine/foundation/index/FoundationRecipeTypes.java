package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

public class FoundationRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RitchiesFirearmEngine.MOD_ID);



    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> register(String id) {
        final String toString = RitchiesFirearmEngine.resource(id).toString();
        return RECIPE_TYPES.register(id, () -> new RecipeType<>() {
            @Override public String toString() { return toString; }
        });
    }

}
