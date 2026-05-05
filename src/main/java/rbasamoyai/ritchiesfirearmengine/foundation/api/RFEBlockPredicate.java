package rbasamoyai.ritchiesfirearmengine.foundation.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.Predicate;

public sealed abstract class RFEBlockPredicate implements Predicate<Block> {

    public static final Codec<RFEBlockPredicate> CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(fromString(s));
                } catch (Exception e) {
                    return DataResult.error(() -> "Error decoding block predicate string: " + e.getMessage());
                }
            }, RFEBlockPredicate::toString);

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEBlockPredicate> STREAM_CODEC =
            PredicateType.STREAM_CODEC.dispatch(RFEBlockPredicate::type, PredicateType::streamCodec);

    public static RFEBlockPredicate fromString(String entry) {
        return entry.charAt(0) == '#' ? TagPredicate.fromLocation(RFEUtils.location(entry.substring(1)))
                : TypePredicate.fromLocation(RFEUtils.location(entry));
    }

    protected abstract PredicateType type();

    public static final class TypePredicate extends RFEBlockPredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, TypePredicate> STREAM_CODEC =
                ByteBufCodecs.registry(Registries.BLOCK).map(TypePredicate::new, t -> t.block);

        private final Block block;

        TypePredicate(Block block) { this.block = block; }

        static TypePredicate fromLocation(ResourceLocation loc) {
            return new TypePredicate(BuiltInRegistries.BLOCK.getOptional(loc)
                    .orElseThrow(() -> new IllegalStateException("Unknown block '" + loc + "'")));
        }

        @Override public boolean test(Block block) { return block == this.block; }
        @Override protected PredicateType type() { return PredicateType.TYPE; }
        @Override public String toString() { return BuiltInRegistries.BLOCK.getKey(this.block).toString(); }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof TypePredicate other && other.block == this.block;
        }
    }

    public static final class TagPredicate extends RFEBlockPredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, TagPredicate> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(rl -> TagKey.create(Registries.BLOCK, rl), TagKey::location)
                        .map(TagPredicate::new, t -> t.tag).cast();

        private final TagKey<Block> tag;

        TagPredicate(TagKey<Block> tag) { this.tag = tag; }

        static TagPredicate fromLocation(ResourceLocation loc) { return new TagPredicate(TagKey.create(Registries.BLOCK, loc)); }

        @Override public boolean test(Block block) { return block.builtInRegistryHolder().is(this.tag); }
        @Override protected PredicateType type() { return PredicateType.TAG; }
        @Override public String toString() { return "#" + this.tag.location(); }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof TagPredicate other && other.tag.equals(this.tag);
        }
    }

    protected enum PredicateType {
        TYPE(TypePredicate.STREAM_CODEC),
        TAG(TagPredicate.STREAM_CODEC);

        private static final StreamCodec<RegistryFriendlyByteBuf, PredicateType> STREAM_CODEC =
                NeoForgeStreamCodecs.enumCodec(PredicateType.class);

        private final StreamCodec<RegistryFriendlyByteBuf, ? extends RFEBlockPredicate> streamCodec;

        PredicateType(StreamCodec<RegistryFriendlyByteBuf, ? extends RFEBlockPredicate> streamCodec) {
            this.streamCodec = streamCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends RFEBlockPredicate> streamCodec() { return this.streamCodec; }
    }

}
