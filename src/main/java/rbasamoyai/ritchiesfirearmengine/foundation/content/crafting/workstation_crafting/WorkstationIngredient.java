package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;
import java.util.function.Predicate;

public record WorkstationIngredient(Ingredient ingredient, int displayX, int displayY,
                                    Optional<ResourceLocation> icon, Optional<String> emptyTextKey)
        implements Predicate<ItemStack> {

    public static final Codec<WorkstationIngredient> CODEC = RecordCodecBuilder.create(o -> o.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(WorkstationIngredient::ingredient),
            Codec.intRange(0, GenericWorkstationMenu.MAX_SLOT_X).fieldOf("display_x").forGetter(WorkstationIngredient::displayX),
            Codec.intRange(0, GenericWorkstationMenu.MAX_SLOT_Y).fieldOf("display_y").forGetter(WorkstationIngredient::displayY),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(WorkstationIngredient::icon),
            Codec.STRING.optionalFieldOf("slot_text").forGetter(WorkstationIngredient::emptyTextKey)
    ).apply(o, WorkstationIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkstationIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WorkstationIngredient::ingredient,
            ByteBufCodecs.VAR_INT, WorkstationIngredient::displayX,
            ByteBufCodecs.VAR_INT, WorkstationIngredient::displayY,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), WorkstationIngredient::icon,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), WorkstationIngredient::emptyTextKey,
            WorkstationIngredient::new);

    @Override public boolean test(ItemStack itemStack) { return this.ingredient.test(itemStack); }

}
