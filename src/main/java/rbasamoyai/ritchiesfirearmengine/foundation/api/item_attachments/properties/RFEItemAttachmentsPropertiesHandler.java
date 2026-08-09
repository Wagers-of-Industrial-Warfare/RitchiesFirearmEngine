package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

public class RFEItemAttachmentsPropertiesHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, ItemAttachmentsPropertiesHolder> ATTACHMENTS = new Reference2ObjectOpenHashMap<>();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener(GSON, RitchiesFirearmEngine.MOD_ID + "/item_attachments");

        private ReloadListener(Gson gson, String directory) { super(gson, directory); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
            ATTACHMENTS.clear();

            Map<Item, AttachmentsBuilder> builders = new Reference2ObjectOpenHashMap<>();

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
                    AttachmentItemDataLayer layer = AttachmentItemDataLayer.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    builders.computeIfAbsent(parentItem, k -> new AttachmentsBuilder()).applyLayer(attachmentItem, layer);
                } catch (Exception e) {
                    LOGGER.error("Error loading item attachments data for {}: {}", fullId, e);
                }
            }

            for (Map.Entry<Item, AttachmentsBuilder> entry : builders.entrySet())
                ATTACHMENTS.put(entry.getKey(), entry.getValue().build());
        }
    }

    public static Optional<RFEItemAttachmentProperties> getData(ItemStack parent, ItemStack attachment, ResourceLocation slot) {
        if (parent.isEmpty() || attachment.isEmpty() || !ATTACHMENTS.containsKey(parent.getItem()))
            return Optional.empty();
        ItemAttachmentsPropertiesHolder attachmentData = ATTACHMENTS.get(parent.getItem());
        return Optional.ofNullable(attachmentData.getAttachmentProperties(attachment, slot));
    }

    private static class AttachmentsBuilder {
        public final Reference2ObjectOpenHashMap<Item, Object2ObjectOpenHashMap<ResourceLocation, RFEItemAttachmentProperties>> renderDataByItemAndSlot = new Reference2ObjectOpenHashMap<>();

        public void applyLayer(Item attachmentItem, AttachmentItemDataLayer layer) {
            Map<ResourceLocation, RFEItemAttachmentProperties> itemDataBySlot = this.renderDataByItemAndSlot
                    .computeIfAbsent(attachmentItem, k -> new Object2ObjectOpenHashMap<>());
            itemDataBySlot.putAll(layer.itemDataBySlot);
        }

        public ItemAttachmentsPropertiesHolder build() {
            return new ItemAttachmentsPropertiesHolder(this.renderDataByItemAndSlot);
        }
    }

    private record AttachmentItemDataLayer(Map<ResourceLocation, RFEItemAttachmentProperties> itemDataBySlot) {
        public static final Codec<AttachmentItemDataLayer> CODEC = RecordCodecBuilder.create(o -> o.group(
                Codec.unboundedMap(ResourceLocation.CODEC, RFEItemAttachmentProperties.CODEC.codec())
                        .fieldOf("slot_attachments").forGetter(AttachmentItemDataLayer::itemDataBySlot)
        ).apply(o, AttachmentItemDataLayer::new));
    }

    public record ItemAttachmentsPropertiesHolder(Reference2ObjectOpenHashMap<Item, Object2ObjectOpenHashMap<ResourceLocation, RFEItemAttachmentProperties>> attachmentPropertiesByItemAndSlot) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttachmentsPropertiesHolder> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM),
                                ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ResourceLocation.STREAM_CODEC, RFEItemAttachmentProperties.STREAM_CODEC))
                        .map(ItemAttachmentsPropertiesHolder::new, ItemAttachmentsPropertiesHolder::attachmentPropertiesByItemAndSlot);

        @Nullable
        public RFEItemAttachmentProperties getAttachmentProperties(Item item, ResourceLocation slot) {
            if (!this.attachmentPropertiesByItemAndSlot.containsKey(item))
                return null;
            return this.attachmentPropertiesByItemAndSlot.get(item).get(slot);
        }

        @Nullable
        public RFEItemAttachmentProperties getAttachmentProperties(ItemStack itemStack, ResourceLocation slot) {
            return this.getAttachmentProperties(itemStack.getItem(), slot);
        }
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncItemAttachmentsPropertiesPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncItemAttachmentsPropertiesPacket(), player);
    }

    public record ClientboundSyncItemAttachmentsPropertiesPacket(Reference2ObjectOpenHashMap<Item, ItemAttachmentsPropertiesHolder> attachments) implements RFEPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncItemAttachmentsPropertiesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM), ItemAttachmentsPropertiesHolder.STREAM_CODEC)
                        .map(ClientboundSyncItemAttachmentsPropertiesPacket::new, ClientboundSyncItemAttachmentsPropertiesPacket::attachments);

        public ClientboundSyncItemAttachmentsPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(ATTACHMENTS)); }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            ATTACHMENTS.clear();
            ATTACHMENTS.putAll(this.attachments);
        }
    }

    private RFEItemAttachmentsPropertiesHandler() {}

}
