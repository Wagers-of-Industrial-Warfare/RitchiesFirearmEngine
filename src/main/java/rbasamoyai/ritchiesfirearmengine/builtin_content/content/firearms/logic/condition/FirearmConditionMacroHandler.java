package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.FolderPackDetector;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoadingException;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FirearmConditionMacroHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, FirearmCondition> CONDITION_MACROS = new Object2ReferenceOpenHashMap<>();

    private static final Gson GSON = new Gson();

    public static void loadMacros() {
        loadModMacros();
        loadFileMacros();
    }

    private static void loadModMacros() {
        DirectoryValidator validator = new DirectoryValidator(path -> false);
        FolderPackDetector folderPackDetector = new FolderPackDetector(validator);
        for (IModFileInfo modFileInfo : LoadingModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            Path resourcePath = modFile.findResource(".").normalize();
            try {
                List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                Pack.ResourcesSupplier resourcesSupplier = folderPackDetector.detectPackResources(resourcePath, list);
                if (!list.isEmpty()) {
                    LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(resourcePath, list));
                } else if (resourcesSupplier != null) {
                    String packId = "mod/" + modFileInfo.moduleName();
                    PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                    loadMacrosFromContentPack(locationInfo, resourcesSupplier);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load built-in mod RFE firearm macros", exception);
            }
        }
    }

    private static void loadFileMacros() {
        Path packsPath = Path.of(".", "rfe_packs").normalize();
        DirectoryValidator validator = new DirectoryValidator(path -> false);
        try {
            FileUtil.createDirectoriesSafe(packsPath);
            FolderRepositorySource.discoverPacks(packsPath, validator, (path, sup) -> {
                String packId = "file/" + path.getFileName().toString();
                PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                loadMacrosFromContentPack(locationInfo, sup);
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load local RFE firearm macros", exception);
        }
    }

    private static void loadMacrosFromContentPack(PackLocationInfo info, Pack.ResourcesSupplier packResourcesSupplier) {
        String packId = info.id();
        int mcMetaPackVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        Pack.Metadata mcMetadata = Pack.readPackMetadata(info, packResourcesSupplier, mcMetaPackVersion);
        if (mcMetadata == null)
            throw new RFEPackLoadingException("Could not load firearm condition macros for RFE content pack " + packId);
        try (PackResources packResources = packResourcesSupplier.openFull(info, mcMetadata)) {
            if (!RFEPackLoader.isPackLoaded(info.id()))
                return;
            ResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(packResources));
            FileToIdConverter idCreator = FileToIdConverter.json("rfe_content/firearm_condition_macros");
            String namespace = RFEPackLoader.getMetadata(info.id()).namespace();
            // Adapted from SimpleJsonResourceReloadListener with extra JsonObject and namespace checks
            for (Map.Entry<ResourceLocation, Resource> entry : idCreator.listMatchingResources(resourceManager).entrySet()) {
                ResourceLocation fileId = entry.getKey();
                ResourceLocation objectId = idCreator.fileToId(fileId);
                if (!objectId.getNamespace().equals(namespace)) {
                    throw new IllegalStateException("RFE firearm condition macro " + objectId + " from " + fileId +
                            " found in RFE pack with different namespace " + namespace);
                }
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement readElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                    if (!readElement.isJsonObject())
                        throw new IllegalStateException("Non-JSON object firearm condition macro file with ID " + objectId);
                    ResourceLocation macroKey = RFEUtils.location(namespace, objectId.getPath());
                    FirearmCondition preExistingElement = CONDITION_MACROS.put(macroKey, FirearmCondition.fromJson(readElement.getAsJsonObject(), false));
                    if (preExistingElement != null)
                        throw new IllegalStateException("Duplicate firearm condition macro file with ID " + objectId);
                } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                    throw new IllegalStateException("Could not parse RFE firearm condition macro file " + objectId + " from " + fileId, jsonparseexception);
                }
            }
        } catch (Exception exception) {
            throw new RFEPackLoadingException("Fatal exception encountered loading RFE firearm macros from pack " + packId + ": " + exception);
        }
    }

    public static FirearmCondition getMacro(ResourceLocation loc) {
        if (!CONDITION_MACROS.containsKey(loc))
            throw new IllegalStateException("Firearm condition macro " + loc + " does not exist");
        return CONDITION_MACROS.get(loc);
    }

}
