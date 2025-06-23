package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AmmoPredicate implements Predicate<ItemStack> {

    private static final Map<ResourceLocation, ItemAmmoPredicate> CACHED_ITEM_AMMO_PREDICATES = new Object2ObjectOpenHashMap<>();

    public static class ItemAmmoPredicate extends AmmoPredicate {
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
    }

    public static class TagAmmoPredicate extends AmmoPredicate {
        private final TagKey<Item> tag;

        public TagAmmoPredicate(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override public boolean test(ItemStack itemStack) { return itemStack.is(this.tag); }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof TagAmmoPredicate other && this.tag.equals(other.tag);
        }
    }

    @Nullable
    public static AmmoPredicate fromString(String string) {
        if (string.isEmpty())
            return null;
        boolean isTag = string.charAt(0) == '#';
        if (isTag)
            string = string.substring(1);
        if (string.isEmpty())
            return null;
        ResourceLocation loc = RFEUtils.location(string);
        if (isTag)
            return new TagAmmoPredicate(TagKey.create(Registries.ITEM, loc));
        return CACHED_ITEM_AMMO_PREDICATES.computeIfAbsent(loc, ItemAmmoPredicate::new);
    }

}
