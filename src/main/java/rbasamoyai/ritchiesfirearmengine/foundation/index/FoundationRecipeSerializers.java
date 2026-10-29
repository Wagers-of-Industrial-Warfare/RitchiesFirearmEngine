package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting.AbstractWorkstationRecipe;

public class FoundationRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RitchiesFirearmEngine.MOD_ID);



    private static <T extends AbstractWorkstationRecipe> DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> simpleWorkstation(String id, AbstractWorkstationRecipe.Factory<T> factory) {
        return RECIPE_SERIALIZERS.register(id, () -> new AbstractWorkstationRecipe.SimpleSerializer<>(factory));
    }

}
