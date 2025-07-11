package rbasamoyai.ritchiesfirearmengine.foundation.pack_loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPlugin;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RFEPackMetadata(String namespace, String version, String displayName, List<DependencyInfo> dependencies,
                              List<RFEPlugin.Info> pluginInfo, List<RFEPlugin.Info> clientPluginInfo) {
    public static final Serializer TYPE = new Serializer();

    public static class Serializer implements MetadataSectionSerializer<RFEPackMetadata> {
        @Override public String getMetadataSectionName() { return "rfe_pack"; }

        @Override
        public RFEPackMetadata fromJson(JsonObject obj) {
            String namespace = GsonHelper.getAsString(obj, "namespace");
            String packVersion = GsonHelper.getAsString(obj, "version");
            String displayName = GsonHelper.getAsString(obj, "display_name");

            List<DependencyInfo> allDependencies = new ArrayList<>();
            JsonArray dependencyJson = GsonHelper.getAsJsonArray(obj, "dependencies", new JsonArray());
            int dependencySize = dependencyJson.size();
            String versionField = RFEModUtils.getModVersionSpecifier();
            for (int i = 0; i < dependencySize; ++i) {
                JsonObject dependencyInfoObject = dependencyJson.get(i).getAsJsonObject();

                String contentString = GsonHelper.getAsString(dependencyInfoObject, "content");
                DependencyInfo.ContentType contentType = DependencyInfo.ContentType.fromString(contentString);
                if (contentType == null)
                    throw new IllegalStateException("Invalid dependency content type '" + contentString + "', must be 'content_pack', 'mod'");

                String dependencyId = GsonHelper.getAsString(dependencyInfoObject, "id");
                // Version only loads the loader version based on the field
                String version = GsonHelper.getAsString(dependencyInfoObject, versionField);

                String relationString = GsonHelper.getAsString(dependencyInfoObject, "relation");
                DependencyInfo.RelationType relationType = DependencyInfo.RelationType.fromString(relationString);
                if (relationType == null)
                    throw new IllegalStateException("Invalid dependency relation type '" + relationString + "', must be 'required', 'optional', 'discouraged', 'incompatible'");
                allDependencies.add(new DependencyInfo(dependencyId, version, contentType, relationType));
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

            List<RFEPlugin.Info> allClientPluginInfo = new ArrayList<>();
            JsonArray clientPluginsJson = GsonHelper.getAsJsonArray(obj, "client_plugins", new JsonArray());
            int clientPluginSize = clientPluginsJson.size();
            for (int i = 0; i < clientPluginSize; ++i) {
                JsonObject clientPluginInfoObject = clientPluginsJson.get(i).getAsJsonObject();
                String modId = GsonHelper.getAsString(clientPluginInfoObject, "mod");
                String classPath = GsonHelper.getAsString(clientPluginInfoObject, "class");
                allClientPluginInfo.add(new RFEPlugin.Info(modId, classPath));
            }

            assertValidNamespace(namespace);
            assertValidPackVersion(packVersion, namespace);
            return new RFEPackMetadata(namespace, packVersion, displayName, allDependencies, allPluginInfo, allClientPluginInfo);
        }
    }

    public record DependencyInfo(String dependencyId, String version, ContentType contentType, RelationType relationType) {
        public enum ContentType {
            CONTENT_PACK,
            MOD;

            private static final Map<String, ContentType> BY_STRING = Arrays.stream(values())
                    .collect(Collectors.toMap(t -> t.name().toLowerCase(Locale.ROOT), Function.identity()));

            @Nullable public static ContentType fromString(String str) { return BY_STRING.get(str); }
        }

        /**
         * Modeled after NeoForge requirements types
         */
        public enum RelationType {
            REQUIRED,
            OPTIONAL,
            DISCOURAGED,
            INCOMPATIBLE;

            private static final Map<String, RelationType> BY_STRING = Arrays.stream(values())
                    .collect(Collectors.toMap(t -> t.name().toLowerCase(Locale.ROOT), Function.identity()));

            @Nullable public static RFEPackMetadata.DependencyInfo.RelationType fromString(String str) { return BY_STRING.get(str); }
        }
    }

    public static void assertValidNamespace(String namespace) {
        if (namespace.length() > 64)
            throw new IllegalStateException("RFE content pack namespace is too long (over 64 characters):" + namespace);
        if (!ResourceLocation.isValidNamespace(namespace))
            throw new IllegalStateException("Non [a-z0-9_.-] character in namespace of RFE content pack: " + namespace);
    }

    public static void assertValidPackVersion(String version, String namespace) {
        if (version.length() > 64)
            throw new IllegalStateException("RFE content pack version for pack " + namespace + " is too long (over 64 characters)");
        if (!isValidPackVersionCharacters(version))
            throw new IllegalStateException("Non [a-zA-Z0-9_.-+] character in version of RFE content pack " + namespace + ": " + version);
    }

    public static boolean isValidPackVersionCharacters(String version) {
        int l = version.length();
        for (int i = 0; i < l; ++i) {
            if (!isValidPackVersionCharacter(version.charAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isValidPackVersionCharacter(char chr) {
        return chr >= '0' && chr <= '9' || chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z'
                || chr == '_' || chr == '.' || chr == '-' || chr == '+';
    }

}
