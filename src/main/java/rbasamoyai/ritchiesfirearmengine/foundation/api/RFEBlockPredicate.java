package rbasamoyai.ritchiesfirearmengine.foundation.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.Predicate;

public sealed abstract class RFEBlockPredicate implements Predicate<Block> {

    protected abstract void toNetwork(FriendlyByteBuf buf);

    public static RFEBlockPredicate of(String entry) {
        return entry.charAt(0) == '#' ? TagPredicate.fromLocation(RFEUtils.location(entry.substring(1)))
                : TypePredicate.fromLocation(RFEUtils.location(entry));
    }

    public static void toNetwork(FriendlyByteBuf buf, RFEBlockPredicate pred) {
        buf.writeBoolean(pred instanceof TypePredicate);
        pred.toNetwork(buf);
    }

    public static RFEBlockPredicate fromNetwork(FriendlyByteBuf buf) {
        boolean type = buf.readBoolean();
        ResourceLocation id = buf.readResourceLocation();
        return type ? TypePredicate.fromLocation(id) : TagPredicate.fromLocation(id);
    }

    public static final class TypePredicate extends RFEBlockPredicate {
        private final Block block;

        TypePredicate(Block block) { this.block = block; }

        static TypePredicate fromLocation(ResourceLocation loc) {
            return new TypePredicate(BuiltInRegistries.BLOCK.getOptional(loc)
                    .orElseThrow(() -> new IllegalStateException("Unknown block '" + loc + "'")));
        }

        @Override public boolean test(Block block) { return block == this.block; }
        @Override protected void toNetwork(FriendlyByteBuf buf) { buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(this.block)); }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof TypePredicate other && other.block == this.block;
        }
    }

    public static final class TagPredicate extends RFEBlockPredicate {
        private final TagKey<Block> tag;

        TagPredicate(TagKey<Block> tag) { this.tag = tag; }

        static TagPredicate fromLocation(ResourceLocation loc) { return new TagPredicate(TagKey.create(Registries.BLOCK, loc)); }

        @Override public boolean test(Block block) { return block.builtInRegistryHolder().is(this.tag); }
        @Override protected void toNetwork(FriendlyByteBuf buf) { buf.writeResourceLocation(this.tag.location()); }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof TagPredicate other && other.tag.equals(this.tag);
        }
    }

}
