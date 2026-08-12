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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;

import java.util.LinkedHashMap;
import java.util.Map;

public class RFEItemAttachmentsScreenDisplayHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, ImmutableMap<MenuType<?>, MenuTypeDisplayConfig>> MENU_SLOTS = new Reference2ObjectOpenHashMap<>();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener(GSON, RitchiesFirearmEngine.MOD_ID + "/item_attachments_screen");

        private ReloadListener(Gson gson, String directory) { super(gson, directory); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
            MENU_SLOTS.clear();

            Map<Item, ImmutableMap.Builder<MenuType<?>, MenuTypeDisplayConfig>> builders = new Reference2ObjectOpenHashMap<>();

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
                    MenuTypeDisplayConfig config = MenuTypeDisplayConfig.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    builders.computeIfAbsent(parentItem, k -> ImmutableMap.builder()).put(menuType, config);
                } catch (Exception e) {
                    LOGGER.error("Error loading item attachments screen display data for {}: {}", fullId, e);
                }
            }

            for (Map.Entry<Item, ImmutableMap.Builder<MenuType<?>, MenuTypeDisplayConfig>> entry : builders.entrySet())
                MENU_SLOTS.put(entry.getKey(), entry.getValue().build());
        }
    }

    public static MenuTypeDisplayConfig getConfig(ItemStack parent, MenuType<?> menu) {
        if (parent.isEmpty() || !MENU_SLOTS.containsKey(parent.getItem()))
            return MenuTypeDisplayConfig.EMPTY;
        return MENU_SLOTS.get(parent.getItem()).getOrDefault(menu, MenuTypeDisplayConfig.EMPTY);
    }

    public record MenuTypeDisplayConfig(float modelScale, ImmutableMap<ResourceLocation, SlotDisplayConfig> slotDisplayConfig) {
        private static final MenuTypeDisplayConfig EMPTY = new MenuTypeDisplayConfig(64f, ImmutableMap.of());

        public static final Codec<MenuTypeDisplayConfig> CODEC = RecordCodecBuilder.create(o -> o.group(
                Codec.floatRange(1f, 256f).optionalFieldOf("model_scale", 64f).forGetter(MenuTypeDisplayConfig::modelScale),
                Codec.unboundedMap(ResourceLocation.CODEC, SlotDisplayConfig.CODEC).xmap(ImmutableMap::copyOf, LinkedHashMap::new)
                        .fieldOf("slots").forGetter(MenuTypeDisplayConfig::slotDisplayConfig)
        ).apply(o, MenuTypeDisplayConfig::new));
    }

    public record SlotDisplayConfig(Vector3f modelPos) {
        public static final Codec<SlotDisplayConfig> CODEC = RecordCodecBuilder.create(o -> o.group(
                ExtraCodecs.VECTOR3F.fieldOf("model_pos").forGetter(SlotDisplayConfig::modelPos)
        ).apply(o, SlotDisplayConfig::new));
    }

    private RFEItemAttachmentsScreenDisplayHandler() {}

}
