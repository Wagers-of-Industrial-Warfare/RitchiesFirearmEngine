package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackMetadata;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class RFEPluginManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, RFEPlugin> PLUGINS = new LinkedHashMap<>();

    public static void registerAndInitPlugins(String packId, RFEPackMetadata metadata) throws Exception {
        Map<String, RFEPlugin> foundPlugins = new LinkedHashMap<>();
        for (RFEPlugin.Info info : metadata.pluginInfo()) {
            if (!PLUGINS.containsKey(info.classPath()))
                foundPlugins.put(info.classPath(), loadPlugin(packId, info, metadata));
        }
        for (RFEPlugin plugin : foundPlugins.values())
            plugin.register();
        PLUGINS.putAll(foundPlugins);

        EnvExecute.runOnClient(() -> () -> {
            try {
                RFEClientPluginManager.registerAndInitPlugins(packId, metadata);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static RFEPlugin loadPlugin(String packId, RFEPlugin.Info info, RFEPackMetadata metadata) throws Exception {
        try {
            if (!RFEModUtils.isModPresent(info.modId()))
                throw new IllegalStateException("Plugin requires mod " + info.modId());
            boolean dependenciesMatch = false;
            for (RFEPackMetadata.DependencyInfo dependency : metadata.dependencies()) {
                if (dependency.dependencyId().equals(info.modId())) {
                    dependenciesMatch = true;
                    break;
                }
            }
            if (!dependenciesMatch)
                throw new IllegalStateException("Plugin's mod requirement is not present in pack metadata dependencies section");
            Class<?> clazz = Class.forName(info.classPath());
            if (!RFEPlugin.class.isAssignableFrom(clazz))
                throw new IllegalStateException("Plugin " + info.classPath() + " must implement RFEPlugin");
            return (RFEPlugin) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception t) {
            RitchiesFirearmEngine.LOGGER.error("Error loading plugin class {} of mod {} from source {}", info.classPath(), info.modId(), packId);
            throw t;
        }
    }

    public static void afterPackLoading() {
        for (RFEPlugin plugin : PLUGINS.values())
            plugin.afterPackLoading();
    }

    public static void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        for (RFEPlugin plugin : PLUGINS.values())
            plugin.registerResourceListeners(registry);
    }

    public static void loadItems(BiConsumer<ResourceLocation, Item> registry) {
        LOGGER.info("Registering RFE plugin items");
        for (RFEPlugin plugin : PLUGINS.values())
            plugin.registerPluginItems(registry);
    }

    public static void loadCreativeModeTabs(BiConsumer<ResourceLocation, CreativeModeTab> registry) {
        LOGGER.info("Registering RFE plugin creative mode tabs");
        for (RFEPlugin plugin : PLUGINS.values())
            plugin.registerPluginCreativeModeTabs(registry);
    }

    public static void loadParticleTypes(BiConsumer<ResourceLocation, ParticleType<?>> registry) {
        LOGGER.info("Registering RFE plugin particle types");
        for (RFEPlugin plugin : PLUGINS.values())
            plugin.registerPluginParticleTypes(registry);
    }

    private RFEPluginManager() {}

}
