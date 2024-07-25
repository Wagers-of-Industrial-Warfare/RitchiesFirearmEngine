package rbasamoyai.ritchiesfirearmengine.pack_content.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;

import java.util.ArrayList;
import java.util.List;

public record RFEPackMetadata(String namespace, String displayName, List<DependencyInfo> dependencies,
                              List<RFEPlugin.Info> pluginInfo) {
    public static final Serializer TYPE = new Serializer();

    public static class Serializer implements MetadataSectionSerializer<RFEPackMetadata> {
        @Override public String getMetadataSectionName() { return "rfe_pack"; }

        @Override
        public RFEPackMetadata fromJson(JsonObject obj) {
            String namespace = GsonHelper.getAsString(obj, "namespace");
            String displayName = GsonHelper.getAsString(obj, "display_name");

            List<DependencyInfo> allDependencies = new ArrayList<>();
            JsonArray dependencyJson = GsonHelper.getAsJsonArray(obj, "dependencies", new JsonArray());
            int dependencySize = dependencyJson.size();
            String versionField = RFEModUtils.getModVersionSpecifier();
            for (int i = 0; i < dependencySize; ++i) {
                JsonObject pluginInfoObject = dependencyJson.get(i).getAsJsonObject();
                String modId = GsonHelper.getAsString(pluginInfoObject, "mod");
                // Version only loads the loader version based on the field
                String version = GsonHelper.getAsString(pluginInfoObject, versionField);
                allDependencies.add(new DependencyInfo(modId, version));
            }

            List<RFEPlugin.Info> allPluginInfo = new ArrayList<>();
            JsonArray pluginsJson = GsonHelper.getAsJsonArray(obj, "plugins", new JsonArray());
            int pluginSize = pluginsJson.size();
            for (int i = 0; i < pluginSize; ++i) {
                JsonObject pluginInfoObject = pluginsJson.get(i).getAsJsonObject();
                String modId = GsonHelper.getAsString(pluginInfoObject, "mod");
                String classPath = GsonHelper.getAsString(pluginInfoObject, "class");
                allPluginInfo.add(new RFEPlugin.Info(modId, classPath));
            }

            return new RFEPackMetadata(namespace, displayName, allDependencies, allPluginInfo);
        }
    }

    public record DependencyInfo(String modId, String version) {
    }

}
