package rbasamoyai.ritchiesfirearmengine.foundation.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.Predicate;

public sealed abstract class RFEEntityTypePredicate implements Predicate<EntityType<?>> {

    protected abstract void toNetwork(FriendlyByteBuf buf);

    public static RFEEntityTypePredicate of(String entry) {
        return entry.charAt(0) == '#' ? TagPredicate.fromLocation(RFEUtils.location(entry.substring(1)))
                : TypePredicate.fromLocation(RFEUtils.location(entry));
    }

    public static void toNetwork(FriendlyByteBuf buf, RFEEntityTypePredicate pred) {
        buf.writeBoolean(pred instanceof TypePredicate);
        pred.toNetwork(buf);
    }

    public static RFEEntityTypePredicate fromNetwork(FriendlyByteBuf buf) {
        boolean type = buf.readBoolean();
        ResourceLocation id = buf.readResourceLocation();
        return type ? TypePredicate.fromLocation(id) : TagPredicate.fromLocation(id);
    }

    public static final class TypePredicate extends RFEEntityTypePredicate {
        private final EntityType<?> type;

        TypePredicate(EntityType<?> type) { this.type = type; }

        static TypePredicate fromLocation(ResourceLocation loc) {
            return new TypePredicate(BuiltInRegistries.ENTITY_TYPE.getOptional(loc)
                    .orElseThrow(() -> new IllegalStateException("Unknown entity type '" + loc + "'")));
        }

        @Override public boolean test(EntityType<?> entityType) { return entityType == this.type; }
        @Override protected void toNetwork(FriendlyByteBuf buf) { buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(this.type)); }
    }

    public static final class TagPredicate extends RFEEntityTypePredicate {
        private final TagKey<EntityType<?>> tag;

        TagPredicate(TagKey<EntityType<?>> tag) { this.tag = tag; }

        static TagPredicate fromLocation(ResourceLocation loc) { return new TagPredicate(TagKey.create(Registries.ENTITY_TYPE, loc)); }

        @Override public boolean test(EntityType<?> entityType) { return entityType.is(this.tag); }
        @Override protected void toNetwork(FriendlyByteBuf buf) { buf.writeResourceLocation(this.tag.location()); }
    }

}
