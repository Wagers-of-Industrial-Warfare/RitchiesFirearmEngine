package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.creative_mode_tab.RFECreativeModeTabMetadata;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackMetadata;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public record RFEContentData(RFEPackMetadata metadata, Map<String, JsonObject> itemData,
                             Map<String, JsonObject> creativeModeTabsData, RFECreativeModeTabMetadata creativeModeTabMetadata) {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    @Nullable
    public static RFEContentData loadContentData(PackResources packResources, RFEPackMetadata metadata) {
        try {
            ResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(packResources));
            Map<String, JsonObject> itemData = getContentInFolder(resourceManager, "rfe_content/items", metadata);
            Map<String, JsonObject> creativeModeTabsData = getContentInFolder(resourceManager, "rfe_content/creative_mode_tabs", metadata);
            RFECreativeModeTabMetadata creativeModeTabMetadata = getCreativeModeTabMetadata(resourceManager, metadata, creativeModeTabsData.keySet());
            return new RFEContentData(metadata, itemData, creativeModeTabsData, creativeModeTabMetadata);
        } catch (Exception exception) {
            return null;
        }
    }

    private static Map<String, JsonObject> getContentInFolder(ResourceManager resourceManager, String folder, RFEPackMetadata metadata) {
        Map<String, JsonObject> output = new HashMap<>();
        FileToIdConverter idCreator = FileToIdConverter.json(folder);
        String namespace = metadata.namespace();
        // Adapted from SimpleJsonResourceReloadListener with extra JsonObject and namespace checks
        for (Map.Entry<ResourceLocation, Resource> entry : idCreator.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            ResourceLocation objectId = idCreator.fileToId(fileId);
            if (!objectId.getNamespace().equals(namespace)) {
                LOGGER.error("RFE content addition {} from {} found in RFE pack with different namespace {}, ignoring entry",
                        objectId, fileId, namespace);
                continue;
            }
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement readElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (!readElement.isJsonObject())
                    throw new IllegalStateException("Non-JSON object data file with ID " + objectId);
                JsonElement preExistingElement = output.put(objectId.getPath(), readElement.getAsJsonObject());
                if (preExistingElement != null)
                    throw new IllegalStateException("Duplicate data file with ID " + objectId);
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                LOGGER.error("Could not parse RFE data file {} from {}", objectId, fileId, jsonparseexception);
            }
        }
        return output;
    }

    private static RFECreativeModeTabMetadata getCreativeModeTabMetadata(ResourceManager resourceManager, RFEPackMetadata metadata, Set<String> tabNames) {
        FileToIdConverter idCreator = FileToIdConverter.json("rfe_content");
        String namespace = metadata.namespace();
        ResourceLocation fileId = idCreator.idToFile(ResourceLocation.fromNamespaceAndPath(namespace, "creative_mode_tabs"));
        Optional<Resource> file = resourceManager.getResource(fileId);
        if (file.isEmpty())
            return RFECreativeModeTabMetadata.createDefault(namespace, tabNames);
        try (Reader reader = file.get().openAsReader()) {
            JsonElement readElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
            RFECreativeModeTabMetadata firstPass = RFECreativeModeTabMetadata.codec(namespace).parse(JsonOps.INSTANCE, readElement)
                    .getOrThrow(s -> new IllegalStateException("Error parsing JSON: " + s));
            List<ResourceLocation> order = new ArrayList<>(firstPass.order());
            Set<ResourceLocation> presentInOrder = new HashSet<>(order);
            List<ResourceLocation> missingEntries = new ArrayList<>();
            for (String tabName : tabNames) {
                ResourceLocation tabLoc = ResourceLocation.fromNamespaceAndPath(namespace, tabName);
                if (!presentInOrder.contains(tabLoc)) {
                    missingEntries.add(tabLoc);
                    order.add(tabLoc);
                }
            }
            if (!missingEntries.isEmpty()) {
                String missing = missingEntries.stream()
                        .map(rl -> String.format("'%s'", rl))
                        .collect(Collectors.joining(", "));
                LOGGER.warn("Creative mode tab metadata order in {} missing entries {}, automatically added", fileId, missing);
            }
            return new RFECreativeModeTabMetadata(order, firstPass.priority());
        } catch (IllegalArgumentException | IOException | JsonParseException e) {
            LOGGER.error("Could not parse RFE creative mode tab metadata file from {}", fileId);
            return RFECreativeModeTabMetadata.createDefault(namespace, tabNames);
        }
    }

}
