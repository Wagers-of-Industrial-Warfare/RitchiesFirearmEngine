package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;
import java.util.function.Predicate;

public abstract sealed class AmmoPredicate implements Predicate<ItemStack> {

    private static final Map<ResourceLocation, ItemAmmoPredicate> CACHED_ITEM_AMMO_PREDICATES = new Object2ObjectOpenHashMap<>();

    public static final class ItemAmmoPredicate extends AmmoPredicate {
        private final ResourceLocation location;
        private Item item = null;
        private boolean resolved = false;

        public ItemAmmoPredicate(ResourceLocation location) {
            this.location = location;
        }

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
        private final TagKey<Item> tag;

        public TagAmmoPredicate(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override public boolean test(ItemStack itemStack) { return itemStack.is(this.tag); }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof TagAmmoPredicate other && this.tag.equals(other.tag);
        }

        @Override public String toString() { return "#" + this.tag.location(); }
    }

    public static AmmoPredicate fromString(String string) {
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

    public static void writeToNetwork(AmmoPredicate pred, FriendlyByteBuf buf) {
        buf.writeUtf(pred.toString());
    }

    public static AmmoPredicate fromNetwork(FriendlyByteBuf buf) {
        return AmmoPredicate.fromString(buf.readUtf());
    }

}
