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
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.Predicate;

public sealed abstract class RFEEntityTypePredicate implements Predicate<EntityType<?>> {

    public static final Codec<RFEEntityTypePredicate> CODEC = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(fromString(s));
                } catch (Exception e) {
                    return DataResult.error(() -> "Error decoding entity type predicate string: " + e.getMessage());
                }
            }, RFEEntityTypePredicate::toString);

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEEntityTypePredicate> STREAM_CODEC =
            PredicateType.STREAM_CODEC.dispatch(RFEEntityTypePredicate::type, PredicateType::streamCodec);

    protected abstract PredicateType type();

    public static RFEEntityTypePredicate fromString(String entry) {
        return entry.charAt(0) == '#' ? TagPredicate.fromLocation(RFEUtils.location(entry.substring(1)))
                : TypePredicate.fromLocation(RFEUtils.location(entry));
    }

    public static final class TypePredicate extends RFEEntityTypePredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, TypePredicate> STREAM_CODEC =
                ByteBufCodecs.registry(Registries.ENTITY_TYPE).map(TypePredicate::new, t -> t.entityType);

        private final EntityType<?> entityType;

        TypePredicate(EntityType<?> entityType) { this.entityType = entityType; }

        static TypePredicate fromLocation(ResourceLocation loc) {
            return new TypePredicate(BuiltInRegistries.ENTITY_TYPE.getOptional(loc)
                    .orElseThrow(() -> new IllegalStateException("Unknown entity type '" + loc + "'")));
        }

        @Override public boolean test(EntityType<?> entityType) { return entityType == this.entityType; }
        @Override protected PredicateType type() { return PredicateType.TYPE; }
        @Override public String toString() { return BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString(); }
    }

    public static final class TagPredicate extends RFEEntityTypePredicate {
        private static final StreamCodec<RegistryFriendlyByteBuf, TagPredicate> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(rl -> TagKey.create(Registries.ENTITY_TYPE, rl), TagKey::location)
                        .map(TagPredicate::new, t -> t.tag).cast();

        private final TagKey<EntityType<?>> tag;

        TagPredicate(TagKey<EntityType<?>> tag) { this.tag = tag; }

        static TagPredicate fromLocation(ResourceLocation loc) { return new TagPredicate(TagKey.create(Registries.ENTITY_TYPE, loc)); }

        @Override public boolean test(EntityType<?> entityType) { return entityType.is(this.tag); }
        @Override protected PredicateType type() { return PredicateType.TAG; }
        @Override public String toString() { return "#" + this.tag.location(); }
    }

    protected enum PredicateType {
        TYPE(TypePredicate.STREAM_CODEC),
        TAG(TagPredicate.STREAM_CODEC);

        private static final StreamCodec<RegistryFriendlyByteBuf, PredicateType> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(PredicateType.class);

        private final StreamCodec<RegistryFriendlyByteBuf, ? extends RFEEntityTypePredicate> streamCodec;

        PredicateType(StreamCodec<RegistryFriendlyByteBuf, ? extends RFEEntityTypePredicate> streamCodec) {
            this.streamCodec = streamCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends RFEEntityTypePredicate> streamCodec() { return this.streamCodec; }
    }

}
