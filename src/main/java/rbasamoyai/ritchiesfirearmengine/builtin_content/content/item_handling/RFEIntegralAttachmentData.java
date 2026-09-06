package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RFEIntegralAttachmentData {

    private static final int MAX_SIZE = 256;

    public static final RFEIntegralAttachmentData EMPTY = new RFEIntegralAttachmentData(new Object2ObjectOpenHashMap<>());

    public static final Codec<RFEIntegralAttachmentData> CODEC = RFEIntegralAttachmentData.Slot.CODEC
            .sizeLimitedListOf(MAX_SIZE)
            .xmap(RFEIntegralAttachmentData::fromSlots, RFEIntegralAttachmentData::asSlots);

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEIntegralAttachmentData> STREAM_CODEC = RFEIntegralAttachmentData.Slot.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SIZE))
            .map(RFEIntegralAttachmentData::fromSlots, RFEIntegralAttachmentData::asSlots);

    private final Object2ObjectOpenHashMap<ResourceLocation, DataComponentPatch> data;
    private final int hashCode;

    private RFEIntegralAttachmentData(Object2ObjectOpenHashMap<ResourceLocation, DataComponentPatch> data) {
        if (data.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Got " + data.size() + " attachment data, but maximum is " + MAX_SIZE);
        } else {
            this.data = data;
            this.hashCode = hashDataList(Lists.newArrayList(data.values()));
        }
    }

    private static int hashDataList(List<DataComponentPatch> list) {
        int hash = 0;
        for (DataComponentPatch data : list)
            hash = hash * 31 + data.hashCode();
        return hash;
    }

    private RFEIntegralAttachmentData(int size) {
        this(new Object2ObjectOpenHashMap<>(size));
    }

    private static RFEIntegralAttachmentData fromSlots(List<RFEIntegralAttachmentData.Slot> slots) {
        RFEIntegralAttachmentData itemAttachmentContents = new RFEIntegralAttachmentData(slots.size());
        for (RFEIntegralAttachmentData.Slot slot : slots)
            itemAttachmentContents.data.put(slot.id, slot.data);
        return itemAttachmentContents;
    }

    public static RFEIntegralAttachmentData fromDataMap(Map<ResourceLocation, DataComponentPatch> data) {
        int sz = data.size();
        RFEIntegralAttachmentData itemAttachmentContents = new RFEIntegralAttachmentData(sz);
        itemAttachmentContents.data.putAll(data);
        return itemAttachmentContents;
    }

    private List<RFEIntegralAttachmentData.Slot> asSlots() {
        List<RFEIntegralAttachmentData.Slot> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, DataComponentPatch> e : this.data.entrySet())
            list.add(new RFEIntegralAttachmentData.Slot(e.getKey(), e.getValue()));
        return list;
    }

    public void copyInto(Map<ResourceLocation, DataComponentPatch> map) {
        map.putAll(this.data);
    }

    public DataComponentPatch getSlot(ResourceLocation slot) {
        return this.data.getOrDefault(slot, DataComponentPatch.EMPTY);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof RFEIntegralAttachmentData itemAttachmentContents))
            return false;
        if (!this.data.keySet().equals(itemAttachmentContents.data.keySet()))
            return false;
        for (Map.Entry<ResourceLocation, DataComponentPatch> e : this.data.entrySet()) {
            if (!e.getValue().equals(itemAttachmentContents.data.get(e.getKey())))
                return false;
        }
        return true;
    }

    @Override public int hashCode() { return this.hashCode; }

    /**
     * Neo:
     * {@return the number of slots in this container}
     */
    public int getSlots() { return this.data.size(); }

    record Slot(ResourceLocation id, DataComponentPatch data) {
        public static final Codec<RFEIntegralAttachmentData.Slot> CODEC = RecordCodecBuilder.create(o -> o.group(
                        ResourceLocation.CODEC.fieldOf("slot").forGetter(RFEIntegralAttachmentData.Slot::id),
                        DataComponentPatch.CODEC.fieldOf("data").forGetter(RFEIntegralAttachmentData.Slot::data))
                .apply(o, RFEIntegralAttachmentData.Slot::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, RFEIntegralAttachmentData.Slot> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, RFEIntegralAttachmentData.Slot::id,
                DataComponentPatch.STREAM_CODEC, RFEIntegralAttachmentData.Slot::data,
                RFEIntegralAttachmentData.Slot::new);
    }

}
