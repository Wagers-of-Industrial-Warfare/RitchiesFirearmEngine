package rbasamoyai.ritchiesfirearmengine.foundation.pack_loading;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentData;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.creative_mode_tab.RFECreativeModeTabBuilder;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPluginManager;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RFEPackLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, RFEPackMetadata> FOUND_METADATA_BY_PATH = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, RFEPackMetadata> FOUND_METADATA_BY_NAMESPACE = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, String> NAMESPACE_TO_PATH = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, RFEContentPack> LOADED_CONTENT_PACKS = new Object2ObjectLinkedOpenHashMap<>();
    private static final Set<String> RESERVED_NAMESPACES = Util.make(new ObjectOpenHashSet<>(), s -> {
       s.add("minecraft");
       s.add("brigadier");
       s.add("realms");
       s.add("forge");
       s.add("fabric");
       s.add("neoforge");
       s.add("quilt");
       s.add("fabric-api");
       s.add("fabricloader");
       s.add("c");
       s.add("ritchiesfirearmengine");
    });

    public static void prepareResources() {
        findResources();
        LOGGER.info("Successfully found {} RFE content packs", FOUND_METADATA_BY_PATH.size());

        loadModBuiltInPacks();
        loadLocalPacks();
        LOGGER.info("Successfully loaded {} RFE content packs", LOADED_CONTENT_PACKS.size());

        RFEPluginManager.afterPackLoading();
    }

    private static void findResources() {
        LOGGER.info("Finding built-in mod RFE content packs");
        DirectoryValidator validator = new DirectoryValidator(path -> false);
        FolderPackDetector folderPackDetector = new FolderPackDetector(validator);
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            Path resourcePath = modFile.findResource(".").normalize();
            Set<String> modNamespaces = new ObjectOpenHashSet<>();
            for (IModInfo modInfo : modFile.getModInfos())
                modNamespaces.add(modInfo.getModId());
            try {
                List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                Pack.ResourcesSupplier resourcesSupplier = folderPackDetector.detectPackResources(resourcePath, list);
                if (!list.isEmpty()) {
                    LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(resourcePath, list));
                } else if (resourcesSupplier != null) {
                    String packId = "mod/" + modFileInfo.moduleName();
                    PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                    loadContentPackMetadata(locationInfo, resourcePath, resourcesSupplier, new BuiltInPackContext(modNamespaces));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load built-in mod RFE content pack metadata", exception);
            }
        }

        Path packsPath = Path.of(".", "rfe_packs").normalize();
        LOGGER.info("Finding local RFE content packs in {}", packsPath.toAbsolutePath());
        try {
            FileUtil.createDirectoriesSafe(packsPath);
            FolderRepositorySource.discoverPacks(packsPath, validator, (path, sup) -> {
                String packId = "file/" + nameFromPath(path);
                PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                loadContentPackMetadata(locationInfo, path, sup, null);
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load local RFE content pack metadata", exception);
        }
    }

    private static void loadModBuiltInPacks() {
        LOGGER.info("Loading built-in mod RFE content packs");
        DirectoryValidator validator = new DirectoryValidator(path -> false);
        FolderPackDetector folderPackDetector = new FolderPackDetector(validator);
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            Path resourcePath = modFile.findResource(".").normalize();
            Set<String> modNamespaces = new ObjectOpenHashSet<>();
            for (IModInfo modInfo : modFile.getModInfos())
                modNamespaces.add(modInfo.getModId());
            try {
                List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                Pack.ResourcesSupplier resourcesSupplier = folderPackDetector.detectPackResources(resourcePath, list);
                if (!list.isEmpty()) {
                    LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(resourcePath, list));
                } else if (resourcesSupplier != null) {
                    String packId = "mod/" + modFileInfo.moduleName();
                    PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                    loadContentPack(locationInfo, resourcesSupplier, new BuiltInPackContext(modNamespaces));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load built-in mod RFE content packs", exception);
            }
        }
    }

    private static void loadLocalPacks() {
        Path packsPath = Path.of(".", "rfe_packs").normalize();
        LOGGER.info("Loading local RFE content packs in {}", packsPath.toAbsolutePath());
        DirectoryValidator validator = new DirectoryValidator(path -> false);
        try {
            FileUtil.createDirectoriesSafe(packsPath);
            FolderRepositorySource.discoverPacks(packsPath, validator, (path, sup) -> {
                String packId = "file/" + nameFromPath(path);
                PackLocationInfo locationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.DEFAULT, Optional.empty());
                loadContentPack(locationInfo, sup, null);
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load local RFE content packs", exception);
        }
    }

    private static void loadContentPackMetadata(PackLocationInfo locationInfo, Path packPath, Pack.ResourcesSupplier packResourcesSupplier,
                                                @Nullable BuiltInPackContext builtInContext) {
        boolean builtIn = builtInContext != null;
        String packId = locationInfo.id();
        try (PackResources packResources = packResourcesSupplier.openPrimary(locationInfo)) {
            RFEPackMetadata metadata = packResources.getMetadataSection(RFEPackMetadata.TYPE);
            if (metadata == null) {
                if (!builtIn)
                    LOGGER.error("Could not load metadata for RFE content pack {}, skipping content pack", packId);
                return;
            }
            String namespace = metadata.namespace();
            if (NAMESPACE_TO_PATH.containsKey(namespace)) {
                LOGGER.error("RFE content pack {} has duplicate namespace {} found in other content pack {}, skipping content pack",
                        packId, namespace, NAMESPACE_TO_PATH.get(namespace));
                return;
            }
            FOUND_METADATA_BY_PATH.put(packId, metadata);
            FOUND_METADATA_BY_NAMESPACE.put(namespace, metadata);
            NAMESPACE_TO_PATH.put(namespace, packId);
            LOGGER.info("Found RFE content pack in {}", packPath.toAbsolutePath());
        } catch (Exception exception) {
            // Few exceptions present as failing to load pack metadata by itself is considered not fatal; crash via lack of required packs
            LOGGER.error("Exception encountered loading RFE content pack metadata for {}, skipping content pack: {}", packId, exception);
        }
    }

    private static void loadContentPack(PackLocationInfo locationInfo, Pack.ResourcesSupplier packResourcesSupplier,
                                        @Nullable BuiltInPackContext builtInContext) {
        boolean builtIn = builtInContext != null;
        String packId = locationInfo.id();
        int mcMetaPackVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        Pack.Metadata mcMetadata = Pack.readPackMetadata(locationInfo, packResourcesSupplier, mcMetaPackVersion);
        if (mcMetadata == null)
            throw new RFEPackLoadingException("Could not load content data for RFE content pack " + packId);
        try (PackResources packResources = packResourcesSupplier.openFull(locationInfo, mcMetadata)) {
            RFEPackMetadata rfeMetadata = FOUND_METADATA_BY_PATH.get(packId);
            if (rfeMetadata == null)
                return;
            validatePackFromMetadata(rfeMetadata, packId, builtInContext);

            RFEPluginManager.registerAndInitPlugins(packId, rfeMetadata);

            RFEContentData contentData = RFEContentData.loadContentData(packResources, rfeMetadata);
            if (contentData == null)
                throw new IllegalStateException("Could not load content data for RFE content pack " + packId);
            Pack resourcePack = loadMinecraftPack(packResources, PackType.CLIENT_RESOURCES, packId, rfeMetadata, builtIn);
            if (resourcePack == null)
                throw new IllegalStateException("Could not load resource pack for RFE content pack " + packId);
            Pack dataPack = loadMinecraftPack(packResources, PackType.SERVER_DATA, packId, rfeMetadata, builtIn);
            if (dataPack == null)
                throw new IllegalStateException("Could not load data pack for RFE content pack " + packId);
            LOADED_CONTENT_PACKS.put(packId, new RFEContentPack(rfeMetadata, contentData, resourcePack, dataPack));
        } catch (Exception exception) {
            throw new RFEPackLoadingException("Fatal exception encountered loading RFE content pack " + packId + ": " + exception);
        }
    }

    private static void validatePackFromMetadata(RFEPackMetadata metadata, String packId, @Nullable BuiltInPackContext builtInContext) {
        String namespace = metadata.namespace();
        if (RESERVED_NAMESPACES.contains(namespace))
            throw new IllegalStateException("RFE content pack " + packId + " using reserved namespace " + namespace);
        if (RFEModUtils.isModPresent(namespace)) {
            if (builtInContext == null)
                throw new IllegalStateException("Local RFE content pack " + packId + " using mod namespace " + namespace);
            if (!builtInContext.modIds.contains(namespace))
                throw new IllegalStateException("Built-in RFE content pack " + packId + " using outside mod namespace " + namespace);
        }
        for (RFEPackMetadata.DependencyInfo dependency : metadata.dependencies())
            assertCompatibleDependency(dependency, packId);
    }
    
    private static void assertCompatibleDependency(RFEPackMetadata.DependencyInfo dependency, String packId) {
        String dependencyId = dependency.dependencyId();
        String versionRange = dependency.version();
        RFEPackMetadata.DependencyInfo.ContentType contentType = dependency.contentType();
        RFEPackMetadata.DependencyInfo.RelationType relation = dependency.relationType();

        if (contentType == RFEPackMetadata.DependencyInfo.ContentType.CONTENT_PACK) {
            RFEPackMetadata metadata = FOUND_METADATA_BY_NAMESPACE.get(dependencyId);
            switch (relation) {
                case REQUIRED:
                    if (metadata == null)
                        throw new IllegalStateException("RFE content pack in " + packId + " requires missing content pack "
                                + dependencyId + " in version range " + versionRange);
                    if (!doesRFEContentPackSatisfyVersionRange(metadata, dependencyId, versionRange, packId))
                        throw new IllegalStateException("RFE content pack in " + packId + " is incompatible with content pack "
                                + dependencyId + " version " + metadata.version() + ", must be in version range " + versionRange);
                    break;
                case OPTIONAL:
                    break;
                case DISCOURAGED:
                    if (metadata != null && doesRFEContentPackSatisfyVersionRange(metadata, dependencyId, versionRange, packId))
                        LOGGER.warn("RFE content pack in {} discourages using content pack {} version {}", packId, dependencyId, metadata.version());
                    break;
                case INCOMPATIBLE:
                    if (metadata != null && doesRFEContentPackSatisfyVersionRange(metadata, dependencyId, versionRange, packId))
                        throw new IllegalStateException("RFE content pack in " + packId + " is incompatible with content pack "
                                + dependencyId + " version " + metadata.version());
                    break;
            }
        } else if (contentType == RFEPackMetadata.DependencyInfo.ContentType.MOD) {
            switch (relation) {
                case REQUIRED:
                    if (!RFEModUtils.isModPresent(dependencyId))
                        throw new IllegalStateException("RFE content pack in " + packId + " requires missing mod "
                                + dependencyId + " in version range " + versionRange);
                    if (!RFEModUtils.isModPresentAndSatisfiesVersion(dependencyId, versionRange))
                        throw new IllegalStateException("RFE content pack in " + packId + " is incompatible with mod "
                                + dependencyId + ", must be in version range " + versionRange);
                    break;
                case OPTIONAL:
                    break;
                case DISCOURAGED:
                    if (RFEModUtils.isModPresentAndSatisfiesVersion(dependencyId, versionRange))
                        LOGGER.warn("RFE content pack in {} discourages using mod {} with version {}", packId, dependencyId, versionRange);
                    break;
                case INCOMPATIBLE:
                    if (RFEModUtils.isModPresentAndSatisfiesVersion(dependencyId, versionRange))
                        throw new IllegalStateException("RFE content pack in " + packId + " is incompatible with mod " + dependencyId
                                + " in version range " + versionRange);
                    break;
            }
        }
    }

    private static boolean doesRFEContentPackSatisfyVersionRange(RFEPackMetadata metadata, String dependencyId, String version, String packId) {
        VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec(version);
        } catch (InvalidVersionSpecificationException exception) {
            throw new IllegalStateException("RFE content pack dependency " + dependencyId + " for pack " + packId + " has invalid version specification " + version);
        }
        return range.containsVersion(new DefaultArtifactVersion(metadata.version()));
    }

    @Nullable
    private static Pack loadMinecraftPack(PackResources packResources, PackType packType, String packId,
                                          RFEPackMetadata metadata, boolean builtIn) {
        Component displayTitle = Component.literal(metadata.displayName() + " ")
                .append(Component.translatable("gui.ritchiesfirearmengine.pack_default"));
        PackLocationInfo locInfo = new PackLocationInfo(packId, displayTitle, PackSource.DEFAULT, Optional.empty());
        Pack.ResourcesSupplier resources = builtIn ? BuiltInPackSource.fixedResources(packResources)
                : BuiltInPackSource.fixedResources(new SubFolderResourceSupplier(packResources, packType.getDirectory()));
        return Pack.readMetaAndCreate(locInfo, resources, packType, new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
    }

    private static String nameFromPath(Path path) { return path.getFileName().toString(); }

    public static void loadItems(BiConsumer<ResourceLocation, Item> cons) {
        LOGGER.info("Registering RFE content pack items");
        int totalObjectCount = 0;
        int successfulObjectCount = 0;
        for (Map.Entry<String, RFEContentPack> packEntry : LOADED_CONTENT_PACKS.entrySet()) {
            RFEContentPack contentPack = packEntry.getValue();
            String namespace = contentPack.metadata().namespace();
            for (Map.Entry<String, JsonObject> itemEntry : contentPack.contentData.itemData().entrySet()) {
                ++totalObjectCount;
                ResourceLocation entryKey = RFEUtils.location(namespace, itemEntry.getKey());
                try {
                    JsonObject itemDefinition = itemEntry.getValue();
                    ResourceLocation typeLocation = RFEUtils.location(GsonHelper.getAsString(itemDefinition, "type"));
                    Item item = RFEContentBuilderRegistry.buildItem(typeLocation, itemDefinition);
                    cons.accept(entryKey, item);
                    ++successfulObjectCount;
                } catch (Exception e) {
                    LOGGER.error("Exception encountered while registering RFE content pack item {} from pack {}, skipping item: {}",
                            entryKey, packEntry.getKey(), e);
                    throw e;
                }
            }
        }
        LOGGER.debug("RFE successfully registered {} out of {} found items", successfulObjectCount, totalObjectCount);
    }

    public static void loadCreativeModeTabs(BiConsumer<ResourceLocation, CreativeModeTab> cons) {
        LOGGER.info("Registering RFE content pack creative mode tabs");
        int totalObjectCount = 0;
        int successfulObjectCount = 0;
        for (Map.Entry<String, RFEContentPack> packEntry : LOADED_CONTENT_PACKS.entrySet()) {
            RFEContentPack contentPack = packEntry.getValue();
            String namespace = contentPack.metadata().namespace();
            for (Map.Entry<String, JsonObject> tabEntry : contentPack.contentData.creativeModeTabsData().entrySet()) {
                ++totalObjectCount;
                ResourceLocation entryKey = RFEUtils.location(namespace, tabEntry.getKey());
                try {
                    JsonObject tabDefinition = tabEntry.getValue();
                    CreativeModeTab creativeModeTab = RFECreativeModeTabBuilder.buildTab(entryKey, tabDefinition);
                    cons.accept(entryKey, creativeModeTab);
                    ++successfulObjectCount;
                } catch (Exception e) {
                    LOGGER.error("Exception encountered while registering RFE content pack item {} from pack {}, skipping item: {}",
                            entryKey, packEntry.getKey(), e);
                    throw e;
                }
            }
        }
        LOGGER.debug("RFE successfully registered {} out of {} found creative mode tabs", successfulObjectCount, totalObjectCount);
    }

    public static void addPacks(PackType packType, Consumer<RepositorySource> cons) {
        List<Pack> packList = new LinkedList<>();
        for (RFEContentPack contentPack : LOADED_CONTENT_PACKS.values())
            packList.add(packType == PackType.CLIENT_RESOURCES ? contentPack.resourcePack : contentPack.dataPack);
        cons.accept(new RFEPackRepository(packList));
    }

    public static LinkedHashMap<String, String> getPackVersions() {
        LinkedHashMap<String, String> versions = new LinkedHashMap<>();
        for (RFEContentPack pack : LOADED_CONTENT_PACKS.values())
            versions.put(pack.metadata().namespace(), pack.metadata().version());
        return versions;
    }

    public static boolean isPackLoaded(String id) { return LOADED_CONTENT_PACKS.containsKey(id); }

    public static RFEPackMetadata getMetadata(String packId) {
        if (!FOUND_METADATA_BY_PATH.containsKey(packId))
            throw new IllegalStateException("No metadata associated with pack " + packId);
        return FOUND_METADATA_BY_PATH.get(packId);
    }

    private record RFEContentPack(RFEPackMetadata metadata, RFEContentData contentData, Pack resourcePack, Pack dataPack) {
    }

    private record RFEPackRepository(List<Pack> list) implements RepositorySource {
        @Override
        public void loadPacks(Consumer<Pack> onLoad) {
            this.list.forEach(onLoad);
        }
    }

    private record BuiltInPackContext(Set<String> modIds) {
    }

    private RFEPackLoader() {}

}
