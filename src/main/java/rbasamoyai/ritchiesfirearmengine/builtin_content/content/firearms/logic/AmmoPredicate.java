package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;
import java.util.function.Predicate;

public abstract sealed class AmmoPredicate implements Predicate<ItemStack> {

    private static final Map<ResourceLocation, ItemAmmoPredicate> CACHED_ITEM_AMMO_PREDICATES = new Object2ObjectOpenHashMap<>();

    public static final Codec<AmmoPredicate> CODEC = Codec.STRING.comapFlatMap(AmmoPredicate::decodeFromString, AmmoPredicate::toString);

    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoPredicate> STREAM_CODEC = PredicateType.STREAM_CODEC
            .dispatch(AmmoPredicate::type, PredicateType::streamCodec);

    protected abstract PredicateType type();

    public static final class ItemAmmoPredicate extends AmmoPredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, ItemAmmoPredicate> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(ItemAmmoPredicate::new, ItemAmmoPredicate::location).cast();

        private final ResourceLocation location;
        private Item item = null;
        private boolean resolved = false;

        public ItemAmmoPredicate(ResourceLocation location) {
            this.location = location;
        }

        private ResourceLocation location() { return this.location; }

        @Override protected PredicateType type() { return PredicateType.ITEM; }

        @Override
        public boolean test(ItemStack itemStack) {
            if (this.item == null) {
                if (this.resolved)
                    return false;
                this.item = BuiltInRegistries.ITEM.getOptional(this.location).orElse(null);
                this.resolved = true;
                if (this.item == null)
                    return false;
            }
            return itemStack.is(this.item);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ItemAmmoPredicate other
                    && (this.item == other.item && this.item != null || this.location.equals(other.location));
        }

        @Override public String toString() { return this.location.toString(); }
    }

    public static final class TagAmmoPredicate extends AmmoPredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, TagAmmoPredicate> STREAM_CODEC =
                ByteBufCodecs.fromCodec(TagKey.codec(Registries.ITEM)).map(TagAmmoPredicate::new, TagAmmoPredicate::tag).cast();

        private final TagKey<Item> tag;

        public TagAmmoPredicate(TagKey<Item> tag) {
            this.tag = tag;
        }

        private TagKey<Item> tag() { return this.tag; }

        @Override protected PredicateType type() { return PredicateType.TAG; }

        @Override public boolean test(ItemStack itemStack) { return itemStack.is(this.tag); }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof TagAmmoPredicate other && this.tag.equals(other.tag);
        }

        @Override public String toString() { return "#" + this.tag.location(); }
    }

    public static AmmoPredicate fromString(String string) throws IllegalStateException {
        if (string.isEmpty())
            throw new IllegalStateException("Cannot make ammo predicate from empty string");
        boolean isTag = string.charAt(0) == '#';
        if (isTag)
            string = string.substring(1);
        if (string.isEmpty())
            throw new IllegalStateException("Cannot make ammo predicate from empty string");
        ResourceLocation loc = RFEUtils.location(string);
        if (isTag)
            return new TagAmmoPredicate(TagKey.create(Registries.ITEM, loc));
        return CACHED_ITEM_AMMO_PREDICATES.computeIfAbsent(loc, ItemAmmoPredicate::new);
    }

    public static DataResult<AmmoPredicate> decodeFromString(String string) {
        try {
            return DataResult.success(fromString(string));
        } catch (IllegalStateException e) {
            return DataResult.error(() -> "Error encountered while decoding ammo predicate: " + e.getMessage());
        }
    }

    protected enum PredicateType {
        ITEM(ItemAmmoPredicate.STREAM_CODEC),
        TAG(TagAmmoPredicate.STREAM_CODEC);

        private static final StreamCodec<RegistryFriendlyByteBuf, PredicateType> STREAM_CODEC =
                NeoForgeStreamCodecs.enumCodec(PredicateType.class).cast();

        private final StreamCodec<RegistryFriendlyByteBuf, ? extends AmmoPredicate> streamCodec;

        PredicateType(StreamCodec<RegistryFriendlyByteBuf, ? extends AmmoPredicate> streamCodec) {
            this.streamCodec = streamCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends AmmoPredicate> streamCodec() { return this.streamCodec; }
    }

}
