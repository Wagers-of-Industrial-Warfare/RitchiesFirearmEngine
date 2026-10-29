package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class AbstractWorkstationRecipe implements Recipe<WorkstationInput> {

    protected final RecipeType<?> type;
    protected final List<WorkstationIngredient> ingredients;
    protected final ItemStack result;
    protected final int craftingTime;

    protected AbstractWorkstationRecipe(RecipeType<?> type, List<WorkstationIngredient> ingredients, ItemStack result, int craftingTime) {
        this.type = type;
        this.ingredients = ingredients;
        this.result = result;
        this.craftingTime = craftingTime;
    }

    @Override
    public boolean matches(WorkstationInput input, Level level) {
        if (input.size() != this.ingredients.size())
            return false;
        for (int i = 0; i < this.ingredients.size(); ++i) {
            if (!this.ingredients.get(i).test(input.getItem(i)))
                return false;
        }
        return true;
    }

    @Override public ItemStack assemble(WorkstationInput input, HolderLookup.Provider registries) { return this.result.copy(); }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return this.result; }

    @Override public RecipeType<?> getType() { return this.type; }

    public List<WorkstationIngredient> getWorkstationIngredients() { return this.ingredients; }
    public ItemStack getResultItemRaw() { return this.result; }
    public int getCraftingTime() { return this.craftingTime; }

    public static class SimpleSerializer<T extends AbstractWorkstationRecipe> implements RecipeSerializer<T> {
        private final MapCodec<T> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        public SimpleSerializer(AbstractWorkstationRecipe.Factory<T> factory) {
            this.codec = RecordCodecBuilder.mapCodec(o -> o.group(
                    WorkstationIngredient.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("ingredients").forGetter(T::getWorkstationIngredients),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(T::getResultItemRaw),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("crafting_time").forGetter(T::getCraftingTime)
            ).apply(o, factory::create));

            this.streamCodec = StreamCodec.composite(
                    WorkstationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), T::getWorkstationIngredients,
                    ItemStack.STREAM_CODEC, T::getResultItemRaw,
                    ByteBufCodecs.VAR_INT, T::getCraftingTime,
                    factory::create);
        }

        @Override public MapCodec<T> codec() { return this.codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() { return this.streamCodec; }
    }

    @FunctionalInterface
    public interface Factory<T extends AbstractWorkstationRecipe> {
        T create(List<WorkstationIngredient> ingredients, ItemStack result, int craftingTime);
    }

}
