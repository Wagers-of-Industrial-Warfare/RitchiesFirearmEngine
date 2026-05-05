package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * Modification of {@link net.minecraft.world.item.component.ItemContainerContents} for RFE purposes, mainly for more
 * ammo and empty spacers. Includes NeoForge comments.
 */
public final class RFEItemContainerContents {
    private static final int MAX_SIZE = 1024;

    public static final RFEItemContainerContents EMPTY = new RFEItemContainerContents(NonNullList.create());

    public static final Codec<RFEItemContainerContents> CODEC = Slot.CODEC
            .sizeLimitedListOf(MAX_SIZE)
            .xmap(RFEItemContainerContents::fromSlots, RFEItemContainerContents::asSlots);

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEItemContainerContents> STREAM_CODEC = Slot.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SIZE))
            .map(RFEItemContainerContents::fromSlots, RFEItemContainerContents::asSlots);

    private final NonNullList<ItemStack> items;
    private final int hashCode;

    private RFEItemContainerContents(NonNullList<ItemStack> items) {
        if (items.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is " + MAX_SIZE);
        } else {
            this.items = items;
            this.hashCode = ItemStack.hashStackList(items);
        }
    }

    private RFEItemContainerContents(int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    private RFEItemContainerContents(List<ItemStack> items) {
        this(items.size());

        for (int i = 0; i < items.size(); i++) {
            this.items.set(i, items.get(i));
        }
    }

    private static RFEItemContainerContents fromSlots(List<Slot> slots) {
        OptionalInt optionalint = slots.stream().mapToInt(Slot::index).max();
        if (optionalint.isEmpty())
            return EMPTY;
        RFEItemContainerContents itemcontainercontents = new RFEItemContainerContents(optionalint.getAsInt() + 1);
        for (Slot slot : slots) {
            if (slot.item.isEmpty()) {
                for (int i = slot.index; i < slot.index + slot.spacingCount; ++i)
                    itemcontainercontents.items.set(slot.index, ItemStack.EMPTY);
            } else {
                itemcontainercontents.items.set(slot.index, slot.item);
            }
        }
        return itemcontainercontents;
    }

    public static RFEItemContainerContents fromItems(List<ItemStack> items) {
        int sz = items.size();
        RFEItemContainerContents itemcontainercontents = new RFEItemContainerContents(sz);
        for (int i = 0; i < sz; ++i)
            itemcontainercontents.items.set(i, items.get(i).copy());
        return itemcontainercontents;
    }

    private List<RFEItemContainerContents.Slot> asSlots() {
        List<RFEItemContainerContents.Slot> list = new ArrayList<>();
        int emptyCount = 0;
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (itemstack.isEmpty()) {
                ++emptyCount;
            } else {
                if (emptyCount > 0) {
                    list.add(new RFEItemContainerContents.Slot(i - emptyCount, ItemStack.EMPTY, emptyCount));
                    emptyCount = 0;
                }
                list.add(new RFEItemContainerContents.Slot(i, itemstack, 0));
            }
        }
        if (emptyCount > 0)
            list.add(new RFEItemContainerContents.Slot(this.items.size() - emptyCount, ItemStack.EMPTY, emptyCount));
        return list;
    }

    public void copyInto(NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack itemstack = i < this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
            list.set(i, itemstack.copy());
        }
    }

    public ItemStack copyOne() {
        return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(0).copy();
    }

    public Stream<ItemStack> stream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return this.items.stream().filter(p_331322_ -> !p_331322_.isEmpty()).map(ItemStack::copy);
    }

    public Iterable<ItemStack> nonEmptyItems() {
        return Iterables.filter(this.items, p_331420_ -> !p_331420_.isEmpty());
    }

    public Iterable<ItemStack> nonEmptyItemsCopy() {
        return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RFEItemContainerContents itemcontainercontents && ItemStack.listMatches(this.items, itemcontainercontents.items);
    }

    @Override public int hashCode() { return this.hashCode; }

    /**
     * Neo:
     * {@return the number of slots in this container}
     */
    public int getSlots() { return this.items.size(); }

    /**
     * Neo: Gets a copy of the stack at a particular slot.
     *
     * @param slot The slot to check. Must be within [0, {@link #getSlots()}]
     * @return A copy of the stack in that slot
     * @throws UnsupportedOperationException if the provided slot index is out-of-bounds.
     */
    public ItemStack getStackInSlot(int slot) {
        this.validateSlotIndex(slot);
        return this.items.get(slot).copy();
    }

    /**
     * Neo: Throws {@link UnsupportedOperationException} if the provided slot index is invalid.
     */
    private void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            throw new UnsupportedOperationException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
        }
    }

    record Slot(int index, ItemStack item, int spacingCount) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(o -> o.group(
                Codec.intRange(0, MAX_SIZE - 1).fieldOf("slot").forGetter(Slot::index),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(Slot::item),
                Codec.intRange(0, 100).optionalFieldOf("spacing", 0).forGetter(Slot::spacingCount))
                        .apply(o, Slot::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::index,
                ItemStack.OPTIONAL_STREAM_CODEC, Slot::item,
                ByteBufCodecs.VAR_INT, Slot::spacingCount,
                Slot::new);
    }
}
