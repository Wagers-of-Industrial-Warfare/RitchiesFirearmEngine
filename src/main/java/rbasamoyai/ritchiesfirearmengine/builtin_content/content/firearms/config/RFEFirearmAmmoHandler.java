package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFirearmItemAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFirearmModeAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEFirearmAmmoHandler {

    private static final Map<Item, RFEFirearmItemAmmoProperties> FIREARM_AMMO_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final Map<Item, RFEFirearmItemAmmoProperties> UNRESOLVED_PROPERTIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, UnresolvedProjectileTypes> UNRESOLVED_PROJECTILE_TYPES = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmModeAmmoProperties EMPTY_MODE = new RFEFirearmModeAmmoProperties(ImmutableMap.of(),
            ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
    private static final RFEFirearmItemAmmoProperties EMPTY = new RFEFirearmItemAmmoProperties(EMPTY_MODE, ImmutableMap.of());

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_ammo"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object when parsing firearm item ammo properties");
                    loadIncomplete(el.getAsJsonObject(), item);
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm ammo properties for {}: {}", id, e);
                }
            }
        }
    }

    private static void clear() {
        FIREARM_AMMO_PROPERTIES.clear();
        UNRESOLVED_PROPERTIES.clear();
        UNRESOLVED_PROJECTILE_TYPES.clear();
    }

    public static void loadProjectileTypes() {
        FIREARM_AMMO_PROPERTIES.clear();

        for (Map.Entry<Item, RFEFirearmItemAmmoProperties> entry : UNRESOLVED_PROPERTIES.entrySet()) {
            Item item = entry.getKey();
            if (!UNRESOLVED_PROJECTILE_TYPES.containsKey(item)) {
                LOGGER.error("Missing projectile types for firearm item {}", BuiltInRegistries.ITEM.getKey(item));
                continue;
            }
            RFEFirearmItemAmmoProperties unresolvedItemProperties = entry.getValue();
            RFEFirearmModeAmmoProperties oldDefaultProperties = unresolvedItemProperties.defaultProperties();
            UnresolvedProjectileTypes unresolvedProjectileTypes = UNRESOLVED_PROJECTILE_TYPES.get(item);

            ImmutableMap.Builder<AmmoPredicate, RFEProjectileType> defaultResolvedPrimaryAmmo = ImmutableMap.builder();
            for (Map.Entry<AmmoPredicate, ResourceLocation> unresolvedPrimaryAmmo : unresolvedProjectileTypes.defaultMode().entrySet()) {
                RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(unresolvedPrimaryAmmo.getValue());
                if (type == null) {
                    LOGGER.warn("Missing projectile type {} in firearm item ammo properties for firearm item {}, skipping",
                            unresolvedPrimaryAmmo.getValue(), BuiltInRegistries.ITEM.getKey(item));
                    continue;
                }
                defaultResolvedPrimaryAmmo.put(unresolvedPrimaryAmmo.getKey(), type);
            }
            RFEFirearmModeAmmoProperties completeDefaultProperties = new RFEFirearmModeAmmoProperties(defaultResolvedPrimaryAmmo.build(),
                    oldDefaultProperties.magazines(), oldDefaultProperties.speedloaders(), oldDefaultProperties.secondaryAmmo());

            ImmutableMap<String, RFEFirearmModeAmmoProperties> unresolvedPropertiesByMode = unresolvedItemProperties.propertiesByMode();
            ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> completePropertiesByMode = ImmutableMap.builder();
            for (Map.Entry<String, Map<AmmoPredicate, ResourceLocation>> modeEntry : unresolvedProjectileTypes.otherModes().entrySet()) {
                String modeName = modeEntry.getKey();
                RFEFirearmModeAmmoProperties oldModeProperties = unresolvedPropertiesByMode.get(modeName);
                if (oldModeProperties == null) {
                    LOGGER.error("Internal error: missing mode {} in firearm item ammo properties for firearm item {}, skipping",
                            modeName, BuiltInRegistries.ITEM.getKey(item));
                    continue;
                }
                ImmutableMap.Builder<AmmoPredicate, RFEProjectileType> modeResolvedPrimaryAmmo = ImmutableMap.builder();
                for (Map.Entry<AmmoPredicate, ResourceLocation> unresolvedPrimaryAmmo : modeEntry.getValue().entrySet()) {
                    RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(unresolvedPrimaryAmmo.getValue());
                    if (type == null) {
                        LOGGER.warn("Missing projectile type {} in firearm item ammo properties for firearm item {}, skipping",
                                unresolvedPrimaryAmmo.getValue(), BuiltInRegistries.ITEM.getKey(item));
                        continue;
                    }
                    modeResolvedPrimaryAmmo.put(unresolvedPrimaryAmmo.getKey(), type);
                }
                completePropertiesByMode.put(modeName, new RFEFirearmModeAmmoProperties(modeResolvedPrimaryAmmo.build(),
                        oldModeProperties.magazines(), oldModeProperties.speedloaders(), oldModeProperties.secondaryAmmo()));
            }

            FIREARM_AMMO_PROPERTIES.put(item, new RFEFirearmItemAmmoProperties(completeDefaultProperties, completePropertiesByMode.build()));
        }
        UNRESOLVED_PROJECTILE_TYPES.clear();
        UNRESOLVED_PROPERTIES.clear();
    }

    private static void loadIncomplete(JsonObject obj, Item item) {
        if (!UNRESOLVED_PROPERTIES.containsKey(item))
            UNRESOLVED_PROPERTIES.put(item, EMPTY);
        if (!UNRESOLVED_PROJECTILE_TYPES.containsKey(item))
            UNRESOLVED_PROJECTILE_TYPES.put(item, new UnresolvedProjectileTypes(new Object2ObjectOpenHashMap<>(), new Object2ObjectOpenHashMap<>()));
        RFEFirearmItemAmmoProperties oldItemProperties = UNRESOLVED_PROPERTIES.get(item);
        UnresolvedProjectileTypes oldUnresolvedTypes = UNRESOLVED_PROJECTILE_TYPES.get(item);

        RFEFirearmModeAmmoProperties defaultModeProperties = loadIncompleteModeProperties(obj, item, oldItemProperties.defaultProperties());
        Map<AmmoPredicate, ResourceLocation> defaultUnresolvedPrimaryAmmo = loadUnresolvedPrimaryAmmo(obj, item, oldUnresolvedTypes.defaultMode());

        ImmutableMap<String, RFEFirearmModeAmmoProperties> oldPropertiesByMode = oldItemProperties.propertiesByMode();
        ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> propertiesByMode = ImmutableMap.builder();
        propertiesByMode.putAll(oldPropertiesByMode);
        Map<String, Map<AmmoPredicate, ResourceLocation>> unresolvedPrimaryAmmoByMode = new Object2ObjectOpenHashMap<>(oldUnresolvedTypes.otherModes());

        if (GsonHelper.isObjectNode(obj, "modes")) {
            JsonObject modesObj = GsonHelper.getAsJsonObject(obj, "modes");
            for (Map.Entry<String, JsonElement> entry : modesObj.entrySet()) {
                JsonElement el = entry.getValue();
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object while parsing mode ammo for item " + BuiltInRegistries.ITEM.getKey(item));
                JsonObject modeObj = el.getAsJsonObject();
                String modeName = entry.getKey();
                propertiesByMode.put(modeName, loadIncompleteModeProperties(modeObj, item, oldPropertiesByMode.getOrDefault(modeName, EMPTY_MODE)));
                unresolvedPrimaryAmmoByMode.put(modeName, loadUnresolvedPrimaryAmmo(modeObj, item,
                        unresolvedPrimaryAmmoByMode.getOrDefault(modeName, new Object2ObjectOpenHashMap<>())));
            }
        }

        UNRESOLVED_PROPERTIES.put(item, new RFEFirearmItemAmmoProperties(defaultModeProperties, propertiesByMode.build()));
        UNRESOLVED_PROJECTILE_TYPES.put(item, new UnresolvedProjectileTypes(defaultUnresolvedPrimaryAmmo, unresolvedPrimaryAmmoByMode));
    }

    private static Map<AmmoPredicate, ResourceLocation> loadUnresolvedPrimaryAmmo(JsonObject obj, Item item, Map<AmmoPredicate, ResourceLocation> unresolvedTypes) {
        if (!GsonHelper.isArrayNode(obj, "primary_ammo"))
            return unresolvedTypes;
        Map<AmmoPredicate, ResourceLocation> newUnresolvedTypes = new Object2ObjectOpenHashMap<>(unresolvedTypes);
        if (GsonHelper.getAsBoolean(obj, "replace_primary_ammo", false))
            newUnresolvedTypes.clear();
        JsonArray arr = GsonHelper.getAsJsonArray(obj, "primary_ammo");
        for (JsonElement el : arr) {
            if (!el.isJsonObject())
                throw new JsonParseException("Expected JSON object while parsing primary ammo for item " + BuiltInRegistries.ITEM.getKey(item));
            JsonObject primaryObj = el.getAsJsonObject();
            AmmoPredicate predicate = AmmoPredicate.fromString(GsonHelper.getAsString(primaryObj, "ammo"));
            ResourceLocation loc = RFEUtils.location(GsonHelper.getAsString(primaryObj, "fires"));
            newUnresolvedTypes.put(predicate, loc);
        }
        return newUnresolvedTypes;
    }

    private static RFEFirearmModeAmmoProperties loadIncompleteModeProperties(JsonObject obj, Item item, RFEFirearmModeAmmoProperties properties) {
        if (GsonHelper.isArrayNode(obj, "magazines")) {
            List<AmmoPredicate> magazineList = new ArrayList<>(properties.magazines());
            if (GsonHelper.getAsBoolean(obj, "replace_magazines", false))
                magazineList.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "magazines");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing magazines for item " + BuiltInRegistries.ITEM.getKey(item));
                magazineList.add(AmmoPredicate.fromString(el.getAsString()));
            }
            properties = new RFEFirearmModeAmmoProperties(properties.primaryAmmo(), ImmutableList.<AmmoPredicate>builder().addAll(magazineList).build(),
                    properties.speedloaders(), properties.secondaryAmmo());
        }
        if (GsonHelper.isArrayNode(obj, "speedloaders")) {
            List<AmmoPredicate> speedloaderList = new ArrayList<>(properties.speedloaders());
            if (GsonHelper.getAsBoolean(obj, "replace_speedloaders", false))
                speedloaderList.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "speedloaders");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing speedloaders for item " + BuiltInRegistries.ITEM.getKey(item));
                speedloaderList.add(AmmoPredicate.fromString(el.getAsString()));
            }
            properties = new RFEFirearmModeAmmoProperties(properties.primaryAmmo(), properties.magazines(),
                    ImmutableList.<AmmoPredicate>builder().addAll(speedloaderList).build(), properties.secondaryAmmo());
        }
        if (GsonHelper.isArrayNode(obj, "secondary_ammo")) {
            List<AmmoPredicate> secondaryAmmoList = new ArrayList<>(properties.secondaryAmmo());
            if (GsonHelper.getAsBoolean(obj, "replace_secondary_ammo", false))
                secondaryAmmoList.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "secondary_ammo");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing secondary ammo for item " + BuiltInRegistries.ITEM.getKey(item));
                secondaryAmmoList.add(AmmoPredicate.fromString(el.getAsString()));
            }
            properties = new RFEFirearmModeAmmoProperties(properties.primaryAmmo(), properties.magazines(), properties.speedloaders(),
                    ImmutableList.<AmmoPredicate>builder().addAll(secondaryAmmoList).build());
        }
        return properties;
    }

    public static RFEFirearmItemAmmoProperties getAmmoProperties(Item item) { return FIREARM_AMMO_PROPERTIES.getOrDefault(item, EMPTY); }

    public static RFEFirearmItemAmmoProperties getAmmoProperties(ItemStack item) { return getAmmoProperties(item.getItem()); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncFirearmAmmoPropertiesPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncFirearmAmmoPropertiesPacket(), player);
    }

    public record ClientboundSyncFirearmAmmoPropertiesPacket(Map<Item, RFEFirearmItemAmmoProperties> properties) implements RFEPacket {
        public static ClientboundSyncFirearmAmmoPropertiesPacket decode(FriendlyByteBuf buf) {
            Map<Item, RFEFirearmItemAmmoProperties> properties = new Reference2ObjectOpenHashMap<>();
            int sz = buf.readVarInt();
            for (int i = 0; i < sz; ++i) {
                Item item = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
                RFEFirearmItemAmmoProperties prop = RFEFirearmItemAmmoProperties.fromNetwork(buf);
                properties.put(item, prop);
            }
            return new ClientboundSyncFirearmAmmoPropertiesPacket(properties);
        }

        ClientboundSyncFirearmAmmoPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(FIREARM_AMMO_PROPERTIES)); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.properties.size());
            for (Map.Entry<Item, RFEFirearmItemAmmoProperties> entry : this.properties.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                RFEFirearmItemAmmoProperties.toNetwork(buf, entry.getValue());
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            FIREARM_AMMO_PROPERTIES.clear();
            FIREARM_AMMO_PROPERTIES.putAll(this.properties);
        }
    }

    private record UnresolvedProjectileTypes(Map<AmmoPredicate, ResourceLocation> defaultMode, Map<String,
            Map<AmmoPredicate, ResourceLocation>> otherModes) {
    }

}
