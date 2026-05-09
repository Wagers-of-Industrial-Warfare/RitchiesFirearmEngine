package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud.NoHUDOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEClientContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;

public class RFEHudOverlayRendererPacksHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, RFEHudOverlayRenderer> HUD_OVERLAY_RENDERERS_BY_ID = new Reference2ObjectOpenHashMap<>();

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/hud_overlay_renderers"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            HUD_OVERLAY_RENDERERS_BY_ID.clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("No item '" + id + "' exists"));
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object for RFE HUD overlay renderer");
                    JsonObject obj = el.getAsJsonObject();
                    ResourceLocation rendererTypeId = RFEUtils.location(GsonHelper.getAsString(obj, "style"));
                    RFEHudOverlayRenderer.Serializer ser = RFEClientContentBuilderRegistry.getHUDOverlayRendererType(rendererTypeId);
                    HUD_OVERLAY_RENDERERS_BY_ID.put(item, ser.apply(obj));
                } catch (Exception e) {
                    LOGGER.error("Error loading RFE HUD overlay renderer for item {}, defaulting to no HUD renderer: {}", id, e);
                }
            }
        }
    }

    public static RFEHudOverlayRenderer getHudOverlayRenderer(Item item) {
        return HUD_OVERLAY_RENDERERS_BY_ID.getOrDefault(item, NoHUDOverlayRenderer.INSTANCE);
    }

    public static RFEHudOverlayRenderer getHudOverlayRenderer(ItemStack itemStack) {
        return getHudOverlayRenderer(itemStack.getItem());
    }

}
