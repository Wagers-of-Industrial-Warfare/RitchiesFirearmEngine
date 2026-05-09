package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.NoOpProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEClientContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.Map;

public class RFEProjectileRendererPacksHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, RFEProjectileRenderer> PROJECTILE_RENDERERS_BY_ID = new Object2ObjectOpenHashMap<>();

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/projectile_renderers"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            PROJECTILE_RENDERERS_BY_ID.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object for RFE projectile renderer");
                    JsonObject obj = el.getAsJsonObject();
                    ResourceLocation rendererTypeId = RFEUtils.location(GsonHelper.getAsString(obj, "renderer"));
                    RFEProjectileRenderer.Serializer ser = RFEClientContentBuilderRegistry.getProjectileRendererType(rendererTypeId);
                    PROJECTILE_RENDERERS_BY_ID.put(id, ser.apply(obj));
                } catch (Exception e) {
                    LOGGER.error("Error loading RFE projectile renderer for projectile type {}, defaulting to no-op renderer: {}", id, e);
                }
            }
        }
    }

    public static RFEProjectileRenderer getProjectileRenderer(RFEProjectileType type) {
        return PROJECTILE_RENDERERS_BY_ID.getOrDefault(RFEProjectileTypeHandler.getProjectileTypeId(type), NoOpProjectileRenderer.INSTANCE);
    }

    public static RFEProjectileRenderer getProjectileRenderer(RFEProjectileInstance instance) {
        return getProjectileRenderer(instance.projectileType());
    }

}
