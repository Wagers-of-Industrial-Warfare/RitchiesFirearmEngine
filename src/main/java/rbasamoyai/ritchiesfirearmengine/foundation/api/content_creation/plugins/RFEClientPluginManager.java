package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import rbasamoyai.ritchiesfirearmengine.RFEClient;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackMetadata;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class RFEClientPluginManager {

    private static final Map<String, RFEClientPlugin> CLIENT_PLUGINS = new LinkedHashMap<>();

    public static void registerAndInitPlugins(String packId, RFEPackMetadata metadata) throws Exception {
        Map<String, RFEClientPlugin> foundPlugins = new LinkedHashMap<>();
        for (RFEPlugin.Info info : metadata.clientPluginInfo()) {
            if (!CLIENT_PLUGINS.containsKey(info.classPath()))
                foundPlugins.put(info.classPath(), loadClientPlugin(packId, info, metadata));
        }
        for (RFEClientPlugin plugin : foundPlugins.values())
            plugin.registerClient();
        CLIENT_PLUGINS.putAll(foundPlugins);
    }

    private static RFEClientPlugin loadClientPlugin(String packId, RFEPlugin.Info info, RFEPackMetadata metadata) throws Exception {
        try {
            if (!RFEModUtils.isModPresent(info.modId()))
                throw new IllegalStateException("Client plugin requires mod " + info.modId());
            boolean dependenciesMatch = false;
            for (RFEPackMetadata.DependencyInfo dependency : metadata.dependencies()) {
                if (dependency.dependencyId().equals(info.modId())) {
                    dependenciesMatch = true;
                    break;
                }
            }
            if (!dependenciesMatch)
                throw new IllegalStateException("Client plugin's mod requirement is not present in pack metadata dependencies section");
            Class<?> clazz = Class.forName(info.classPath());
            if (!RFEClientPlugin.class.isAssignableFrom(clazz))
                throw new IllegalStateException("Client plugin " + info.classPath() + " must implement RFEClientPlugin");
            return (RFEClientPlugin) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception t) {
            RitchiesFirearmEngine.LOGGER.error("Error loading client plugin class {} of mod {} from source {}", info.classPath(), info.modId(), packId);
            throw t;
        }
    }

    public static void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        for (Map.Entry<String, RFEClientPlugin> entry : CLIENT_PLUGINS.entrySet())
            entry.getValue().registerResourceListeners(registry);
    }

    public static void registerParticleProviders(RFEClient.ParticleRegistry registry) {
        for (RFEClientPlugin plugin : CLIENT_PLUGINS.values())
            plugin.registerParticleProviders(registry);
    }

    public static void onClientSetup() {
        for (RFEClientPlugin plugin : CLIENT_PLUGINS.values())
            plugin.onClientSetup();
    }

    private RFEClientPluginManager() {}

}
