package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.FileUtil;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoadingException;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirearmCondtionMacroHandler {

    private static final Map<ResourceLocation, FirearmCondition> CONDITION_MACROS = new Object2ReferenceOpenHashMap<>();

    private static final Gson GSON = new Gson();

    public static void loadMacros() {
        loadModMacros();
        loadFileMacros();
    }

    private static void loadModMacros() {
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            Path resourcePath = modFile.findResource(".").normalize();
            Set<String> modNamespaces = new ObjectOpenHashSet<>();
            for (IModInfo modInfo : modFile.getModInfos())
                modNamespaces.add(modInfo.getModId());
            try {
                Pack.ResourcesSupplier packResourcesSupplier = FolderRepositorySource.detectPackResources(resourcePath, false);
                if (packResourcesSupplier != null)
                    loadMacrosFromContentPack("mod/" + modFileInfo.moduleName(), resourcePath, packResourcesSupplier);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load built-in mod RFE firearm macros", exception);
            }
        }
    }

    private static void loadFileMacros() {
        Path packsPath = Path.of(".", "rfe_packs").normalize();
        try {
            FileUtil.createDirectoriesSafe(packsPath);
            FolderRepositorySource.discoverPacks(packsPath, false, (path, sup) -> loadMacrosFromContentPack("file/" + path.getFileName().toString(), path, sup));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load local RFE firearm macros", exception);
        }
    }

    private static void loadMacrosFromContentPack(String packId, Path packPath, Pack.ResourcesSupplier packResourcesSupplier) {
        try (PackResources packResources = packResourcesSupplier.open(packId)) {
            if (!RFEPackLoader.isPackLoaded(packId))
                return;
            ResourceManager resourceManager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(packResources));
            FileToIdConverter idCreator = FileToIdConverter.json("rfe_content/firearm_condition_macros");
            String namespace = RFEPackLoader.getMetadata(packId).namespace();
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
