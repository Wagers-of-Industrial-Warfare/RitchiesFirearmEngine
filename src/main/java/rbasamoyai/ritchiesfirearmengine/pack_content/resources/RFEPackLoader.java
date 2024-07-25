package rbasamoyai.ritchiesfirearmengine.pack_content.resources;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.RFEContentData;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.creative_mode_tab.RFECreativeModeTabBuilder;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPluginManager;
import rbasamoyai.ritchiesfirearmengine.utils.RFEModUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class RFEPackLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, RFEContentPack> LOADED_CONTENT_PACKS = new Object2ObjectLinkedOpenHashMap<>();
    private static final Set<String> CLAIMED_CONTENT_PACK_NAMESPACES = new ObjectOpenHashSet<>();
    private static final Set<String> RESERVED_NAMESPACES = Util.make(new ObjectOpenHashSet<>(), s -> {
       s.add("minecraft");
       s.add("brigadier");
       s.add("realms");
       s.add("forge");
       s.add("fabric");
       s.add("neoforge");
       s.add("quilt");
       s.add("c");
    });

    public static void prepareResources() {
        loadModBuiltInPacks();
        loadLocalPacks();
        LOGGER.info("Successfully loaded {} RFE content packs", LOADED_CONTENT_PACKS.size());
    }

    private static void loadModBuiltInPacks() {
        LOGGER.info("Loading built-in mod RFE content packs");
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            Path resourcePath = modFile.findResource(".").normalize();
            Set<String> modNamespaces = new ObjectOpenHashSet<>();
            for (IModInfo modInfo : modFile.getModInfos())
                modNamespaces.add(modInfo.getModId());
            try {
                Pack.ResourcesSupplier packResourcesSupplier = FolderRepositorySource.detectPackResources(resourcePath, false);
                if (packResourcesSupplier != null)
                    loadContentPack("mod/" + modFileInfo.moduleName(), resourcePath, packResourcesSupplier, new BuiltInPackContext(modNamespaces));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load built-in mod RFE content packs", exception);
            }
        }
    }

    private static void loadLocalPacks() {
        Path packsPath = Path.of(".", "rfe_packs").normalize();
        LOGGER.info("Loading local RFE content packs in {}", packsPath.toAbsolutePath());
        try {
            FileUtil.createDirectoriesSafe(packsPath);
            FolderRepositorySource.discoverPacks(packsPath, false, (path, sup) -> loadContentPack("file/" + nameFromPath(path), path, sup, null));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load local RFE content packs", exception);
        }
    }

    private static void loadContentPack(String packId, Path packPath, Pack.ResourcesSupplier packResourcesSupplier,
                                        @Nullable BuiltInPackContext builtInContext) {
        boolean builtIn = builtInContext != null;
        try (PackResources packResources = packResourcesSupplier.open(packId)) {
            RFEPackMetadata metadata = packResources.getMetadataSection(RFEPackMetadata.TYPE);
            if (metadata == null) {
                if (!builtIn)
                    LOGGER.error("Could not load metadata for RFE content pack {}, skipping content pack", packId);
                return;
            }
            if (!validatePackFromMetadata(metadata, packId, builtInContext))
                return;

            LOGGER.info("Found RFE content pack in {}", packPath.toAbsolutePath());
            RFEPluginManager.registerAndInitPlugins(packId, metadata);
            RFEContentData contentData = RFEContentData.loadContentData(packResources, metadata);
            if (contentData == null) {
                LOGGER.error("Could not load content data for RFE content pack {}, skipping content pack", packId);
            }
            Pack resourcePack = loadMinecraftPack(packResources, PackType.CLIENT_RESOURCES, packId, metadata, builtIn);
            if (resourcePack == null) {
                LOGGER.error("Could not load resource pack for RFE content pack {}, skipping content pack", packId);
                return;
            }
            Pack dataPack = loadMinecraftPack(packResources, PackType.SERVER_DATA, packId, metadata, builtIn);
            if (dataPack == null) {
                LOGGER.error("Could not load data pack for RFE content pack {}, skipping content pack", packId);
                return;
            }
            LOADED_CONTENT_PACKS.put(packId, new RFEContentPack(metadata, contentData, resourcePack, dataPack));
            CLAIMED_CONTENT_PACK_NAMESPACES.add(metadata.namespace());
        } catch (Exception exception) {
            LOGGER.error("Exception encountered loading RFE content pack {}, skipping content pack: {}", packId, exception);
        }
    }

    private static boolean validatePackFromMetadata(RFEPackMetadata metadata, String packId,
                                                    @Nullable BuiltInPackContext builtInContext) {
        String namespace = metadata.namespace();
        if (CLAIMED_CONTENT_PACK_NAMESPACES.contains(namespace)) {
            LOGGER.error("RFE content pack {} using already existing content pack namespace {}, skipping content pack", packId, namespace);
            return false;
        }
        if (RESERVED_NAMESPACES.contains(namespace)) {
            LOGGER.error("RFE content pack {} using reserved namespace {}, skipping content pack", packId, namespace);
            return false;
        }
        if (RFEModUtils.isModPresent(namespace)) {
            if (builtInContext == null) {
                LOGGER.error("Local RFE content pack {} using mod namespace {}, skipping content pack", packId, namespace);
                return false;
            } else if (!builtInContext.modIds.contains(namespace)) {
                LOGGER.error("Built-in RFE content pack {} using outside mod namespace {}, skipping content pack", packId, namespace);
                return false;
            }
        }
        for (RFEPackMetadata.DependencyInfo dependency : metadata.dependencies()) {
            if (!RFEModUtils.isModPresentAndSatisfiesVersion(dependency.modId(), dependency.version())) {
                LOGGER.error("RFE content pack {} requires mod {} with version {}, skipping content pack", packId,
                        dependency.modId(), dependency.version());
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Pack loadMinecraftPack(PackResources packResources, PackType packType, String packId,
                                          RFEPackMetadata metadata, boolean builtIn) {
        Component displayTitle = Component.literal(metadata.displayName() + " ")
                .append(Component.translatable("gui.ritchiesfirearmengine.pack_default"));
        PackResources resources = builtIn ? packResources : new SubFolderResourceSupplier(packResources, packType.getDirectory());
        return Pack.readMetaAndCreate(packId, displayTitle, true, $ -> resources, packType, Pack.Position.BOTTOM, PackSource.DEFAULT);
    }

    private static String nameFromPath(Path path) { return path.getFileName().toString(); }

    public static void loadItems() {
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
                    Registry.register(BuiltInRegistries.ITEM, entryKey, item);
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

    public static void loadCreativeModeTabs() {
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
                    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, entryKey, creativeModeTab);
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

    public static void addPacks(PackType packType, Consumer<RepositorySource> cons) {
        List<Pack> packList = new LinkedList<>();
        for (RFEContentPack contentPack : LOADED_CONTENT_PACKS.values())
            packList.add(packType == PackType.CLIENT_RESOURCES ? contentPack.resourcePack : contentPack.dataPack);
        cons.accept(new RFEPackRepository(packList));
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
