package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

public class RFEItemAttachmentsMenuSlotsHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, ImmutableMap<MenuType<?>, MenuTypeSlotsConfig>> MENU_SLOTS = new Reference2ObjectOpenHashMap<>();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener(GSON, RitchiesFirearmEngine.MOD_ID + "/item_attachments_menu");

        private ReloadListener(Gson gson, String directory) { super(gson, directory); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
            MENU_SLOTS.clear();

            Map<Item, ImmutableMap.Builder<MenuType<?>, MenuTypeSlotsConfig>> builders = new Reference2ObjectOpenHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entries()) {
                ResourceLocation fullId = entry.getKey();
                try {
                    String[] components = fullId.getPath().split("/", 3);
                    ResourceLocation parentItemId = ResourceLocation.fromNamespaceAndPath(fullId.getNamespace(), components[0]);
                    ResourceLocation menuTypeId = ResourceLocation.fromNamespaceAndPath(components[1], components[2]);
                    Item parentItem = BuiltInRegistries.ITEM.getOptional(parentItemId)
                            .orElseThrow(() -> new IllegalStateException("Item " + parentItemId + " does not exist"));
                    MenuType<?> menuType = BuiltInRegistries.MENU.getOptional(menuTypeId)
                            .orElseThrow(() -> new IllegalStateException("Menu " + menuTypeId + " does not exist"));
                    MenuTypeSlotsConfig config = MenuTypeSlotsConfig.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    builders.computeIfAbsent(parentItem, k -> ImmutableMap.builder()).put(menuType, config);
                } catch (Exception e) {
                    LOGGER.error("Error loading item attachments data for {}: {}", fullId, e);
                }
            }

            for (Map.Entry<Item, ImmutableMap.Builder<MenuType<?>, MenuTypeSlotsConfig>> entry : builders.entrySet())
                MENU_SLOTS.put(entry.getKey(), entry.getValue().build());
        }
    }

    public static MenuTypeSlotsConfig getConfig(ItemStack parent, MenuType<?> menu) {
        if (parent.isEmpty() || !MENU_SLOTS.containsKey(parent.getItem()))
            return MenuTypeSlotsConfig.EMPTY;
        return MENU_SLOTS.get(parent.getItem()).getOrDefault(menu, MenuTypeSlotsConfig.EMPTY);
    }

    public record MenuTypeSlotsConfig(ImmutableMap<ResourceLocation, SlotConfig> slotConfig) {
        private static final MenuTypeSlotsConfig EMPTY = new MenuTypeSlotsConfig(ImmutableMap.of());

        public static final Codec<MenuTypeSlotsConfig> CODEC = RecordCodecBuilder.create(o -> o.group(
                Codec.unboundedMap(ResourceLocation.CODEC, SlotConfig.CODEC).xmap(ImmutableMap::copyOf, LinkedHashMap::new)
                        .fieldOf("slots").forGetter(MenuTypeSlotsConfig::slotConfig)
        ).apply(o, MenuTypeSlotsConfig::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MenuTypeSlotsConfig> STREAM_CODEC =
                ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, SlotConfig.STREAM_CODEC)
                        .map(ImmutableMap::copyOf, HashMap::new)
                        .map(MenuTypeSlotsConfig::new, MenuTypeSlotsConfig::slotConfig);
    }

    public record SlotConfig(@Nullable ResourceLocation emptyIcon, @Nullable String emptyText, boolean disabled) {
        public static final Codec<SlotConfig> CODEC = RecordCodecBuilder.create(o -> o.group(
                ResourceLocation.CODEC.optionalFieldOf("empty_icon").forGetter(p -> Optional.ofNullable(p.emptyIcon)),
                Codec.STRING.optionalFieldOf("empty_text").forGetter(p -> Optional.ofNullable(p.emptyText)),
                Codec.BOOL.optionalFieldOf("disabled", false).forGetter(SlotConfig::disabled)
        ).apply(o, (opBg, opTxt, disabled) -> new SlotConfig(opBg.orElse(null), opTxt.orElse(null), disabled)));

        public static final StreamCodec<RegistryFriendlyByteBuf, SlotConfig> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).map(op -> op.orElse(null), Optional::ofNullable), SlotConfig::emptyIcon,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).map(op -> op.orElse(null), Optional::ofNullable), SlotConfig::emptyText,
                ByteBufCodecs.BOOL, SlotConfig::disabled,
                SlotConfig::new);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncItemAttachmentsMenuSlotsPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncItemAttachmentsMenuSlotsPacket(), player);
    }

    public record ClientboundSyncItemAttachmentsMenuSlotsPacket(Reference2ObjectOpenHashMap<Item, ImmutableMap<MenuType<?>, MenuTypeSlotsConfig>> menuSlots) implements RFEPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncItemAttachmentsMenuSlotsPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM),
                        ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.MENU), MenuTypeSlotsConfig.STREAM_CODEC)
                                .map(ImmutableMap::copyOf, Reference2ObjectOpenHashMap::new))
                        .map(ClientboundSyncItemAttachmentsMenuSlotsPacket::new, ClientboundSyncItemAttachmentsMenuSlotsPacket::menuSlots);

        public ClientboundSyncItemAttachmentsMenuSlotsPacket() { this(new Reference2ObjectOpenHashMap<>(MENU_SLOTS)); }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            MENU_SLOTS.clear();
            MENU_SLOTS.putAll(this.menuSlots);
        }
    }

    private RFEItemAttachmentsMenuSlotsHandler() {}

}
