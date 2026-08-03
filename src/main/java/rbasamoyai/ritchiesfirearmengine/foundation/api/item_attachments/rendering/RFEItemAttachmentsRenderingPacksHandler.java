package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentsRenderData.SlotAttachmentRenderData;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;

import java.util.Map;
import java.util.Optional;

public class RFEItemAttachmentsRenderingPacksHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, RFEItemAttachmentsRenderData> ATTACHMENTS = new Reference2ObjectOpenHashMap<>();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener(GSON, RitchiesFirearmEngine.MOD_ID + "/item_attachments_rendering");

        private ReloadListener(Gson gson, String directory) { super(gson, directory); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
            ATTACHMENTS.clear();

            Map<Item, RendererBuilder> builders = new Reference2ObjectOpenHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entries()) {
                ResourceLocation fullId = entry.getKey();
                try {
                    String[] components = fullId.getPath().split("/", 3);
                    ResourceLocation parentItemId = ResourceLocation.fromNamespaceAndPath(fullId.getNamespace(), components[0]);
                    ResourceLocation attachmentItemId = ResourceLocation.fromNamespaceAndPath(components[1], components[2]);
                    Item parentItem = BuiltInRegistries.ITEM.getOptional(parentItemId)
                            .orElseThrow(() -> new IllegalStateException("Item " + parentItemId + " does not exist"));
                    Item attachmentItem = BuiltInRegistries.ITEM.getOptional(attachmentItemId)
                            .orElseThrow(() -> new IllegalStateException("Item " + attachmentItemId + " does not exist"));
                    RendererItemLayer layer = RendererItemLayer.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    builders.computeIfAbsent(parentItem, k -> new RendererBuilder()).applyLayer(attachmentItem, layer);
                } catch (Exception e) {
                    LOGGER.error("Error loading item attachments rendering data for {}: {}", fullId, e);
                }
            }

            for (Map.Entry<Item, RendererBuilder> entry : builders.entrySet())
                ATTACHMENTS.put(entry.getKey(), entry.getValue().build());
        }
    }

    public static Optional<SlotAttachmentRenderData> getRenderData(ItemStack parent, ItemStack attachment, ResourceLocation slot) {
        if (parent.isEmpty() || attachment.isEmpty() || !ATTACHMENTS.containsKey(parent.getItem()))
            return Optional.empty();
        RFEItemAttachmentsRenderData renderData = ATTACHMENTS.get(parent.getItem());
        Map<Item, Map<ResourceLocation, SlotAttachmentRenderData>> renderDataByItemAndSlot = renderData.renderDataByItemAndSlot();
        if (!renderDataByItemAndSlot.containsKey(attachment.getItem()))
            return Optional.empty();
        Map<ResourceLocation, SlotAttachmentRenderData> renderDataBySlot = renderDataByItemAndSlot.get(attachment.getItem());
        return Optional.ofNullable(renderDataBySlot.get(slot));
    }

    private static class RendererBuilder {
        public final Map<Item, Map<ResourceLocation, SlotAttachmentRenderData>> renderDataByItemAndSlot = new Reference2ObjectOpenHashMap<>();

        public void applyLayer(Item attachmentItem, RendererItemLayer layer) {
            Map<ResourceLocation, SlotAttachmentRenderData> itemRenderDataBySlot = this.renderDataByItemAndSlot
                    .computeIfAbsent(attachmentItem, k -> new Object2ObjectOpenHashMap<>());
            itemRenderDataBySlot.putAll(layer.itemRenderDataBySlot);
        }

        public RFEItemAttachmentsRenderData build() {
            return new RFEItemAttachmentsRenderData(this.renderDataByItemAndSlot);
        }
    }

    private record RendererItemLayer(Map<ResourceLocation, SlotAttachmentRenderData> itemRenderDataBySlot) {
        public static final Codec<RendererItemLayer> CODEC = RecordCodecBuilder.create(o -> o.group(
                Codec.unboundedMap(ResourceLocation.CODEC, SlotAttachmentRenderData.CODEC.codec())
                        .fieldOf("slot_rendering").forGetter(RendererItemLayer::itemRenderDataBySlot)
        ).apply(o, RendererItemLayer::new));
    }

    private RFEItemAttachmentsRenderingPacksHandler() {}

}
