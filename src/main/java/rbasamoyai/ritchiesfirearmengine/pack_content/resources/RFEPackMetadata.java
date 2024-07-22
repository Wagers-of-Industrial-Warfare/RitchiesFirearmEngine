package rbasamoyai.ritchiesfirearmengine.pack_content.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin;

import java.util.ArrayList;
import java.util.List;

public record RFEPackMetadata(String namespace, String displayName, List<RFEPlugin.Info> pluginInfo) {
    public static final Serializer TYPE = new Serializer();

    public static class Serializer implements MetadataSectionSerializer<RFEPackMetadata> {
        @Override public String getMetadataSectionName() { return "rfe_pack"; }

        @Override
        public RFEPackMetadata fromJson(JsonObject obj) {
            String namespace = GsonHelper.getAsString(obj, "namespace");
            String displayName = GsonHelper.getAsString(obj, "display_name");

            List<RFEPlugin.Info> allPluginInfo = new ArrayList<>();
            JsonArray plugins = GsonHelper.getAsJsonArray(obj, "plugins", new JsonArray());
            for (int i = 0; i < plugins.size(); ++i) {
                JsonObject pluginInfoObject = plugins.get(i).getAsJsonObject();
                String modId = GsonHelper.getAsString(pluginInfoObject, "mod");
                String classPath = GsonHelper.getAsString(pluginInfoObject, "class");
                allPluginInfo.add(new RFEPlugin.Info(modId, classPath));
            }

            return new RFEPackMetadata(namespace, displayName, allPluginInfo);
        }
    }

}
