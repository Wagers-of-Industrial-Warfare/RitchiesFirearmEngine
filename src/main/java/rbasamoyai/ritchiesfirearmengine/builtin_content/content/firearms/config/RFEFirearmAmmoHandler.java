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
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEFirearmAmmoHandler {

    private static final Map<Item, RFEFirearmProperties<RFEFirearmModeAmmoProperties>> FIREARM_AMMO_PROPERTIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, UnresolvedItemAmmoProperties> UNRESOLVED_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmModeAmmoProperties EMPTY_MODE = new RFEFirearmModeAmmoProperties(ImmutableMap.of(),
            ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), null);
    private static final RFEFirearmProperties<RFEFirearmModeAmmoProperties> EMPTY = new RFEFirearmProperties<>(EMPTY_MODE, ImmutableMap.of());

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
    }

    public static void loadProjectileTypes() {
        FIREARM_AMMO_PROPERTIES.clear();
        for (Map.Entry<Item, UnresolvedItemAmmoProperties> entry : UNRESOLVED_PROPERTIES.entrySet())
            FIREARM_AMMO_PROPERTIES.put(entry.getKey(), entry.getValue().resolve(entry.getKey()));
        UNRESOLVED_PROPERTIES.clear();
    }

    private static void loadIncomplete(JsonObject obj, Item item) {
        if (!UNRESOLVED_PROPERTIES.containsKey(item))
            UNRESOLVED_PROPERTIES.put(item, new UnresolvedItemAmmoProperties());
        UnresolvedItemAmmoProperties properties = UNRESOLVED_PROPERTIES.get(item);

        loadUnresolvedModeProperties(obj, item, properties.defaultModeProperties);

        if (GsonHelper.isObjectNode(obj, "modes")) {
            JsonObject modesObj = GsonHelper.getAsJsonObject(obj, "modes");
            for (Map.Entry<String, JsonElement> entry : modesObj.entrySet()) {
                JsonElement el = entry.getValue();
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object while parsing mode ammo for item " + BuiltInRegistries.ITEM.getKey(item));
                JsonObject modeObj = el.getAsJsonObject();
                String modeName = entry.getKey();
                UnresolvedModeAmmoProperties modeProperties = properties.modeProperties.computeIfAbsent(modeName,
                        s -> properties.defaultModeProperties.fork());
                loadUnresolvedModeProperties(modeObj, item, modeProperties);
            }
        }
    }

    private static void loadUnresolvedModeProperties(JsonObject obj, Item item, UnresolvedModeAmmoProperties properties) {
        if (GsonHelper.isArrayNode(obj, "primary_ammo")) {
            if (GsonHelper.getAsBoolean(obj, "replace_primary_ammo", false))
                properties.primaryAmmo.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "primary_ammo");
            for (JsonElement el : arr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object while parsing primary ammo for item " + BuiltInRegistries.ITEM.getKey(item));
                JsonObject primaryObj = el.getAsJsonObject();
                AmmoPredicate predicate = AmmoPredicate.fromString(GsonHelper.getAsString(primaryObj, "ammo"));
                ResourceLocation loc = RFEUtils.location(GsonHelper.getAsString(primaryObj, "fires"));
                properties.primaryAmmo.put(predicate, loc);
            }
        }
        if (GsonHelper.getAsBoolean(obj, "no_unlimited_projectile", false)) {
            properties.unlimitedProjectile = null;
        } else if (GsonHelper.isStringValue(obj, "unlimited_projectile")) {
            properties.unlimitedProjectile = RFEUtils.location(GsonHelper.getAsString(obj, "unlimited_projectile"));
        }
        if (GsonHelper.isArrayNode(obj, "magazines")) {
            if (GsonHelper.getAsBoolean(obj, "replace_magazines", false))
                properties.magazines.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "magazines");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing magazines for item " + BuiltInRegistries.ITEM.getKey(item));
                properties.magazines.add(AmmoPredicate.fromString(el.getAsString()));
            }
        }
        if (GsonHelper.isArrayNode(obj, "speedloaders")) {
            if (GsonHelper.getAsBoolean(obj, "replace_speedloaders", false))
                properties.speedloaders.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "speedloaders");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing speedloaders for item " + BuiltInRegistries.ITEM.getKey(item));
                properties.speedloaders.add(AmmoPredicate.fromString(el.getAsString()));
            }
        }
        if (GsonHelper.isArrayNode(obj, "secondary_ammo")) {
            if (GsonHelper.getAsBoolean(obj, "replace_secondary_ammo", false))
                properties.secondaryAmmo.clear();
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "secondary_ammo");
            for (JsonElement el : arr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected valid item predicate while parsing secondary ammo for item " + BuiltInRegistries.ITEM.getKey(item));
                properties.secondaryAmmo.add(AmmoPredicate.fromString(el.getAsString()));
            }
        }
    }

    public static RFEFirearmProperties<RFEFirearmModeAmmoProperties> getAmmoProperties(Item item) {
        return FIREARM_AMMO_PROPERTIES.getOrDefault(item, EMPTY);
    }

    public static RFEFirearmProperties<RFEFirearmModeAmmoProperties> getAmmoProperties(ItemStack item) {
        return getAmmoProperties(item.getItem());
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(ClientboundSyncFirearmAmmoPropertiesPacket.fromLoadedProperties());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(ClientboundSyncFirearmAmmoPropertiesPacket.fromLoadedProperties(), player);
    }

    public record ClientboundSyncFirearmAmmoPropertiesPacket(Map<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> properties) implements RFEPacket {
        public static ClientboundSyncFirearmAmmoPropertiesPacket decode(FriendlyByteBuf buf) {
            Map<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> properties = new Reference2ObjectOpenHashMap<>();
            int sz = buf.readVarInt();
            for (int i = 0; i < sz; ++i) {
                Item item = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
                RFEFirearmProperties<UnresolvedModeAmmoProperties> prop = RFEFirearmProperties.fromNetwork(buf,
                        ClientboundSyncFirearmAmmoPropertiesPacket::modePropertiesFromNetwork);
                properties.put(item, prop);
            }
            return new ClientboundSyncFirearmAmmoPropertiesPacket(properties);
        }

        static ClientboundSyncFirearmAmmoPropertiesPacket fromLoadedProperties() {
            Map<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> unresolvedPropertiesByItem = new Reference2ObjectOpenHashMap<>();

            for (Map.Entry<Item, RFEFirearmProperties<RFEFirearmModeAmmoProperties>> entry : FIREARM_AMMO_PROPERTIES.entrySet()) {
                RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolved = entry.getValue();
                UnresolvedModeAmmoProperties defaultUnresolved = fromResolvedProperties(resolved.defaultProperties());
                ImmutableMap.Builder<String, UnresolvedModeAmmoProperties> unresolvedByMode = ImmutableMap.builder();
                for (Map.Entry<String, RFEFirearmModeAmmoProperties> modeEntry : resolved.propertiesByMode().entrySet())
                    unresolvedByMode.put(modeEntry.getKey(), fromResolvedProperties(modeEntry.getValue()));
                unresolvedPropertiesByItem.put(entry.getKey(), new RFEFirearmProperties<>(defaultUnresolved, unresolvedByMode.build()));
            }

            return new ClientboundSyncFirearmAmmoPropertiesPacket(unresolvedPropertiesByItem);
        }

        private static UnresolvedModeAmmoProperties fromResolvedProperties(RFEFirearmModeAmmoProperties resolved) {
            UnresolvedModeAmmoProperties unresolved = new UnresolvedModeAmmoProperties();
            for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : resolved.primaryAmmo().entrySet()) {
                ResourceLocation loc = RFEProjectileTypeHandler.getProjectileTypeId(entry.getValue());
                if (loc != null)
                    unresolved.primaryAmmo.put(entry.getKey(), loc);
            }
            unresolved.magazines.addAll(resolved.magazines());
            unresolved.speedloaders.addAll(resolved.speedloaders());
            unresolved.secondaryAmmo.addAll(resolved.secondaryAmmo());
            if (resolved.unlimitedProjectile() != null)
                unresolved.unlimitedProjectile = RFEProjectileTypeHandler.getProjectileTypeId(resolved.unlimitedProjectile());
            return unresolved;
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.properties.size());
            for (Map.Entry<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> entry : this.properties.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                RFEFirearmProperties.toNetwork(buf, entry.getValue(), ClientboundSyncFirearmAmmoPropertiesPacket::modePropertiesToNetwork);
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            FIREARM_AMMO_PROPERTIES.clear();
            for (Map.Entry<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> entry : this.properties.entrySet()) {
                Item item = entry.getKey();
                RFEFirearmProperties<UnresolvedModeAmmoProperties> unresolved = entry.getValue();

                ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> resolvedPropertiesByMode = ImmutableMap.builder();
                for (Map.Entry<String, UnresolvedModeAmmoProperties> entry1 : unresolved.propertiesByMode().entrySet())
                    resolvedPropertiesByMode.put(entry1.getKey(), entry1.getValue().resolve(item));

                RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolved = new RFEFirearmProperties<>(unresolved.defaultProperties().resolve(item),
                        resolvedPropertiesByMode.build());
                FIREARM_AMMO_PROPERTIES.put(item, resolved);
            }
        }

        private static UnresolvedModeAmmoProperties modePropertiesFromNetwork(FriendlyByteBuf buf) {
            UnresolvedModeAmmoProperties properties = new UnresolvedModeAmmoProperties();
            int primarySz = buf.readVarInt();
            for (int i = 0; i < primarySz; ++i)
                properties.primaryAmmo.put(AmmoPredicate.fromNetwork(buf), buf.readResourceLocation());

            int magazineSz = buf.readVarInt();
            for (int i = 0; i < magazineSz; ++i)
                properties.magazines.add(AmmoPredicate.fromNetwork(buf));

            int speedloaderSz = buf.readVarInt();
            for (int i = 0; i < speedloaderSz; ++i)
                properties.speedloaders.add(AmmoPredicate.fromNetwork(buf));

            int secondaryAmmoSz = buf.readVarInt();
            for (int i = 0; i < secondaryAmmoSz; ++i)
                properties.secondaryAmmo.add(AmmoPredicate.fromNetwork(buf));

            if (buf.readBoolean())
                properties.unlimitedProjectile = buf.readResourceLocation();

            return properties;
        }

        private static void modePropertiesToNetwork(FriendlyByteBuf buf, UnresolvedModeAmmoProperties properties) {
            buf.writeVarInt(properties.primaryAmmo.size());
            for (Map.Entry<AmmoPredicate, ResourceLocation> entry : properties.primaryAmmo.entrySet()) {
                ResourceLocation id = entry.getValue();
                AmmoPredicate.writeToNetwork(entry.getKey(), buf);
                buf.writeResourceLocation(id);
            }

            buf.writeVarInt(properties.magazines.size());
            for (AmmoPredicate pred : properties.magazines)
                AmmoPredicate.writeToNetwork(pred, buf);

            buf.writeVarInt(properties.speedloaders.size());
            for (AmmoPredicate pred : properties.speedloaders)
                AmmoPredicate.writeToNetwork(pred, buf);

            buf.writeVarInt(properties.secondaryAmmo.size());
            for (AmmoPredicate pred : properties.secondaryAmmo)
                AmmoPredicate.writeToNetwork(pred, buf);

            buf.writeBoolean(properties.unlimitedProjectile != null);
            if (properties.unlimitedProjectile != null)
                buf.writeResourceLocation(properties.unlimitedProjectile);
        }
    }

    private static class UnresolvedItemAmmoProperties {
        public UnresolvedModeAmmoProperties defaultModeProperties = new UnresolvedModeAmmoProperties();
        public Map<String, UnresolvedModeAmmoProperties> modeProperties = new Object2ObjectOpenHashMap<>();

        public RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolve(Item item) {
            ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> resolvedModeProperties = ImmutableMap.builder();
            for (Map.Entry<String, UnresolvedModeAmmoProperties> entry : this.modeProperties.entrySet())
                resolvedModeProperties.put(entry.getKey(), entry.getValue().resolve(item));
            return new RFEFirearmProperties<>(this.defaultModeProperties.resolve(item), resolvedModeProperties.build());
        }
    }

    private static class UnresolvedModeAmmoProperties {
        public Map<AmmoPredicate, ResourceLocation> primaryAmmo = new LinkedHashMap<>();
        public List<AmmoPredicate> magazines = new ArrayList<>();
        public List<AmmoPredicate> speedloaders = new ArrayList<>();
        public List<AmmoPredicate> secondaryAmmo = new ArrayList<>();
        public ResourceLocation unlimitedProjectile = null;

        public UnresolvedModeAmmoProperties fork() {
            UnresolvedModeAmmoProperties newProperties = new UnresolvedModeAmmoProperties();
            newProperties.primaryAmmo = new LinkedHashMap<>(this.primaryAmmo);
            newProperties.magazines = new ArrayList<>(this.magazines);
            newProperties.speedloaders = new ArrayList<>(this.speedloaders);
            newProperties.secondaryAmmo = new ArrayList<>(this.secondaryAmmo);
            newProperties.unlimitedProjectile = this.unlimitedProjectile;
            return newProperties;
        }

        public RFEFirearmModeAmmoProperties resolve(Item item) {
            ImmutableMap.Builder<AmmoPredicate, RFEProjectileType> primaryAmmo = ImmutableMap.builder();
            for (Map.Entry<AmmoPredicate, ResourceLocation> entry : this.primaryAmmo.entrySet()) {
                RFEProjectileType projectileType = loadProjectileTypeOrWarnIgnore(entry.getValue(), item);
                if (projectileType != null)
                    primaryAmmo.put(entry.getKey(), projectileType);
            }
            ImmutableList.Builder<AmmoPredicate> magazines = ImmutableList.builder();
            magazines.addAll(this.magazines);
            ImmutableList.Builder<AmmoPredicate> speedloaders = ImmutableList.builder();
            speedloaders.addAll(this.speedloaders);
            ImmutableList.Builder<AmmoPredicate> secondaryAmmo = ImmutableList.builder();
            secondaryAmmo.addAll(this.secondaryAmmo);
            RFEProjectileType unlimitedProjectile = null;
            if (this.unlimitedProjectile != null)
                unlimitedProjectile = loadProjectileTypeOrWarnIgnore(this.unlimitedProjectile, item);
            return new RFEFirearmModeAmmoProperties(primaryAmmo.build(), magazines.build(), speedloaders.build(), secondaryAmmo.build(), unlimitedProjectile);
        }

        @Nullable
        private static RFEProjectileType loadProjectileTypeOrWarnIgnore(ResourceLocation typeId, Item item) {
            RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(typeId);
            if (type == null) {
                LOGGER.warn("Missing projectile type {} in firearm item ammo properties for firearm item {}, skipping",
                        typeId, BuiltInRegistries.ITEM.getKey(item));
                return null;
            } else {
                return type;
            }
        }
    }

}
