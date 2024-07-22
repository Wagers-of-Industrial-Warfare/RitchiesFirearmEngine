package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins;

import net.minecraftforge.fml.ModList;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.pack_content.resources.RFEPackMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

public class RFEPluginManager {

    private static final Map<String, RFEPlugin> PLUGINS = new LinkedHashMap<>();

    public static void registerAndInitPlugins(String packId, RFEPackMetadata metadata) throws Exception {
        Map<String, RFEPlugin> foundPlugins = new LinkedHashMap<>();
        for (RFEPlugin.Info info : metadata.pluginInfo()) {
            if (!PLUGINS.containsKey(info.classPath()))
                foundPlugins.put(info.classPath(), loadPlugin(packId, info));
        }
        for (RFEPlugin plugin : foundPlugins.values()) {
            plugin.registerItemBuilders();
        }
        PLUGINS.putAll(foundPlugins);
    }

    private static RFEPlugin loadPlugin(String packId, RFEPlugin.Info info) throws Exception {
        try {
            if (!isModPresent(info.modId()))
                throw new IllegalStateException("Plugin requires mod " + info.modId());
            Class<?> clazz = Class.forName(info.classPath());
            if (!RFEPlugin.class.isAssignableFrom(clazz))
                throw new IllegalStateException("Plugin class " + info.classPath() + " in mod " + info.modId() + " must implement RFEPlugin");
            return (RFEPlugin) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception t) {
            RitchiesFirearmEngine.LOGGER.error("Error loading plugin class {} from source {}", info.classPath(), packId);
            throw t;
        }
    }

    private static boolean isModPresent(String modId) {
        return ModList.get().getModFileById(modId) != null;
    }

    private RFEPluginManager() {}

}
