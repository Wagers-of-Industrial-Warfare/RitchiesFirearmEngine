package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins;

import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.pack_content.resources.RFEPackMetadata;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class RFEPluginManager {

    private static final Map<String, RFEPlugin> PLUGINS = new LinkedHashMap<>();

    public static void registerAndInitPlugins(String packId, RFEPackMetadata metadata) throws Exception {
        Map<String, RFEPlugin> foundPlugins = new LinkedHashMap<>();
        for (RFEPlugin.Info info : metadata.pluginInfo()) {
            if (!PLUGINS.containsKey(info.classPath()))
                foundPlugins.put(info.classPath(), loadPlugin(packId, info, metadata));
        }
        for (RFEPlugin plugin : foundPlugins.values()) {
            plugin.registerItemBuilders();
        }
        PLUGINS.putAll(foundPlugins);
    }

    private static RFEPlugin loadPlugin(String packId, RFEPlugin.Info info, RFEPackMetadata metadata) throws Exception {
        try {
            if (!RFEModUtils.isModPresent(info.modId()))
                throw new IllegalStateException("Plugin requires mod " + info.modId());
            boolean dependenciesMatch = false;
            for (RFEPackMetadata.DependencyInfo dependency : metadata.dependencies()) {
                if (dependency.modId().equals(info.modId())) {
                    dependenciesMatch = true;
                    break;
                }
            }
            if (!dependenciesMatch)
                throw new IllegalStateException("Plugin's mod requirement is not present in pack metadata dependencies section");
            Class<?> clazz = Class.forName(info.classPath());
            if (!RFEPlugin.class.isAssignableFrom(clazz))
                throw new IllegalStateException("Plugin must implement RFEPlugin");
            return (RFEPlugin) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception t) {
            RitchiesFirearmEngine.LOGGER.error("Error loading plugin class {} of mod {} from source {}", info.classPath(), info.modId(), packId);
            throw t;
        }
    }

    private RFEPluginManager() {}

}
