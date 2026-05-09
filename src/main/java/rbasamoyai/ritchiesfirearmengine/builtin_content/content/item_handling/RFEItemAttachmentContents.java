package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modification of {@link net.minecraft.world.item.component.ItemContainerContents} for RFE item attachment items.
 * Includes NeoForge comments.
 */
public final class RFEItemAttachmentContents {
    private static final int MAX_SIZE = 256;

    public static final RFEItemAttachmentContents EMPTY = new RFEItemAttachmentContents(new Object2ObjectOpenHashMap<>());

    public static final Codec<RFEItemAttachmentContents> CODEC = Slot.CODEC
            .sizeLimitedListOf(MAX_SIZE)
            .xmap(RFEItemAttachmentContents::fromSlots, RFEItemAttachmentContents::asSlots);

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEItemAttachmentContents> STREAM_CODEC = Slot.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SIZE))
            .map(RFEItemAttachmentContents::fromSlots, RFEItemAttachmentContents::asSlots);

    private final Object2ObjectOpenHashMap<ResourceLocation, ItemStack> items;
    private final int hashCode;

    private RFEItemAttachmentContents(Object2ObjectOpenHashMap<ResourceLocation, ItemStack> items) {
        if (items.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is " + MAX_SIZE);
        } else {
            this.items = items;
            this.hashCode = ItemStack.hashStackList(Lists.newArrayList(items.values()));
        }
    }

    private RFEItemAttachmentContents(int size) {
        this(new Object2ObjectOpenHashMap<>(size));
    }

    private static RFEItemAttachmentContents fromSlots(List<Slot> slots) {
        RFEItemAttachmentContents itemAttachmentContents = new RFEItemAttachmentContents(slots.size());
        for (Slot slot : slots)
            itemAttachmentContents.items.put(slot.id, slot.item);
        return itemAttachmentContents;
    }

    public static RFEItemAttachmentContents fromItems(Map<ResourceLocation, ItemStack> items) {
        int sz = items.size();
        RFEItemAttachmentContents itemAttachmentContents = new RFEItemAttachmentContents(sz);
        for (Map.Entry<ResourceLocation, ItemStack> e : items.entrySet())
            itemAttachmentContents.items.put(e.getKey(), e.getValue().copy());
        return itemAttachmentContents;
    }

    private List<RFEItemAttachmentContents.Slot> asSlots() {
        List<RFEItemAttachmentContents.Slot> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ItemStack> e : this.items.entrySet())
            list.add(new Slot(e.getKey(), e.getValue()));
        return list;
    }

    public void copyInto(Map<ResourceLocation, ItemStack> map) {
        for (Map.Entry<ResourceLocation, ItemStack> e : this.items.entrySet())
            map.put(e.getKey(), e.getValue().copy());
    }

    public ItemStack copySlot(ResourceLocation slot) {
        return this.items.containsKey(slot) ? this.items.get(slot).copy() : ItemStack.EMPTY;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof RFEItemAttachmentContents itemAttachmentContents))
            return false;
        if (!this.items.keySet().equals(itemAttachmentContents.items.keySet()))
            return false;
        for (Map.Entry<ResourceLocation, ItemStack> e : this.items.entrySet()) {
            if (!ItemStack.matches(e.getValue(), itemAttachmentContents.items.get(e.getKey())))
                return false;
        }
        return true;
    }

    @Override public int hashCode() { return this.hashCode; }

    /**
     * Neo:
     * {@return the number of slots in this container}
     */
    public int getSlots() { return this.items.size(); }

    record Slot(ResourceLocation id, ItemStack item) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(o -> o.group(
                ResourceLocation.CODEC.fieldOf("slot").forGetter(Slot::id),
                ItemStack.CODEC.fieldOf("item").forGetter(Slot::item))
                        .apply(o, Slot::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Slot::id,
                ItemStack.STREAM_CODEC, Slot::item,
                Slot::new);
    }
}
