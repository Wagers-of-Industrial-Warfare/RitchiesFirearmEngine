package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEBlockPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEEntityTypePredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationProperties.PenetrationStats;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEProjectilePenetrationHandler {

    private static final Map<ResourceLocation, RFEProjectilePenetrationProperties> PENETRATION_PROPERTIES = new Object2ReferenceOpenHashMap<>();
    private static final RFEProjectilePenetrationProperties DEFAULT_PENETRATION = new PropertiesBuilder().build();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/projectile_penetration"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            PENETRATION_PROPERTIES.clear();

            Map<ResourceLocation, PropertiesBuilder> builtObjects = new Object2ObjectOpenHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object for RFE projectile penetration properties");
                    loadProperties(el.getAsJsonObject(), builtObjects.computeIfAbsent(id, k -> new PropertiesBuilder()));
                } catch (Exception e) {
                    LOGGER.error("Error loading projectile penetration properties {}: {}", id, e);
                }
            }

            for (Map.Entry<ResourceLocation, PropertiesBuilder> entry : builtObjects.entrySet())
                PENETRATION_PROPERTIES.put(entry.getKey(), entry.getValue().build());
        }
    }
    
    private static final PenetrationStats DEFAULT_JSON_STATS = new PenetrationStats(1, 0.25f);

    private static void loadProperties(JsonObject obj, PropertiesBuilder oldBuilder) {
        if (GsonHelper.isObjectNode(obj, "default_entity_penetration"))
            oldBuilder.defaultEntityPenetration = statsFromJson(obj.getAsJsonObject("default_entity_penetration"));
        if (GsonHelper.getAsBoolean(obj, "replace_entity_penetration", false))
            oldBuilder.entityPenetration.clear();
        if (GsonHelper.isArrayNode(obj, "entity_penetration")) {
            for (JsonElement el : GsonHelper.getAsJsonArray(obj, "entity_penetration")) {
                if (el.isJsonObject()) {
                    JsonObject statsObj = el.getAsJsonObject();
                    RFEEntityTypePredicate pred = RFEEntityTypePredicate.of(GsonHelper.getAsString(statsObj, "type"));
                    oldBuilder.entityPenetration.put(pred, statsFromJson(statsObj));
                } else if (GsonHelper.isStringValue(el)) {
                    oldBuilder.entityPenetration.put(RFEEntityTypePredicate.of(el.getAsString()), DEFAULT_JSON_STATS);
                } else {
                    throw new JsonParseException("Entity penetration entry must either be JSON object or string (resource location)");
                }
            }
        }
        if (GsonHelper.isObjectNode(obj, "default_block_penetration"))
            oldBuilder.defaultBlockPenetration = statsFromJson(obj.getAsJsonObject("default_block_penetration"));
        if (GsonHelper.getAsBoolean(obj, "replace_block_penetration", false))
            oldBuilder.blockPenetration.clear();
        if (GsonHelper.isArrayNode(obj, "block_penetration")) {
            for (JsonElement el : GsonHelper.getAsJsonArray(obj, "block_penetration")) {
                if (el.isJsonObject()) {
                    JsonObject statsObj = el.getAsJsonObject();
                    RFEBlockPredicate pred = RFEBlockPredicate.of(GsonHelper.getAsString(statsObj, "block"));
                    oldBuilder.blockPenetration.put(pred, statsFromJson(statsObj));
                } else if (GsonHelper.isStringValue(el)) {
                    oldBuilder.blockPenetration.put(RFEBlockPredicate.of(el.getAsString()), DEFAULT_JSON_STATS);
                } else {
                    throw new JsonParseException("Block penetration entry must either be JSON object or string (resource location)");
                }
            }
        }
        if (GsonHelper.isObjectNode(obj, "default_block_breaking"))
            oldBuilder.defaultBlockBreaking = statsFromJson(obj.getAsJsonObject("default_block_breaking"));
        if (GsonHelper.getAsBoolean(obj, "replace_block_breaking", false))
            oldBuilder.blockBreaking.clear();
        if (GsonHelper.isArrayNode(obj, "block_breaking")) {
            for (JsonElement el : GsonHelper.getAsJsonArray(obj, "block_breaking")) {
                if (el.isJsonObject()) {
                    JsonObject statsObj = el.getAsJsonObject();
                    RFEBlockPredicate pred = RFEBlockPredicate.of(GsonHelper.getAsString(statsObj, "block"));
                    oldBuilder.blockBreaking.put(pred, statsFromJson(statsObj));
                } else if (GsonHelper.isStringValue(el)) {
                    oldBuilder.blockBreaking.put(RFEBlockPredicate.of(el.getAsString()), DEFAULT_JSON_STATS);
                } else {
                    throw new JsonParseException("Block breaking entry must either be JSON object or string (resource location)");
                }
            }
        }
    }
    
    private static PenetrationStats statsFromJson(JsonObject obj) {
        return new PenetrationStats(Mth.clamp(GsonHelper.getAsFloat(obj, "chance", 1), 0, 1),
                Mth.clamp(GsonHelper.getAsFloat(obj, "damage_to_projectile", 0.25f), 0, 1));
    }

    public static RFEProjectilePenetrationProperties getPenetrationProperties(ResourceLocation id) {
        return PENETRATION_PROPERTIES.getOrDefault(id, DEFAULT_PENETRATION);
    }

    public static void syncToAll() { RFENetwork.sendToAll(new ClientboundSyncProjectilePenetrationPacket()); }

    public static void syncToPlayer(ServerPlayer player) { RFENetwork.sendToPlayer(new ClientboundSyncProjectilePenetrationPacket(), player); }

    private static class PropertiesBuilder {
        public PenetrationStats defaultEntityPenetration = new PenetrationStats(0, 0);
        public final Map<RFEEntityTypePredicate, PenetrationStats> entityPenetration = new LinkedHashMap<>();
        public PenetrationStats defaultBlockPenetration = new PenetrationStats(0, 0);
        public final Map<RFEBlockPredicate, PenetrationStats> blockPenetration = new LinkedHashMap<>();
        public PenetrationStats defaultBlockBreaking = new PenetrationStats(0, 0);
        public final Map<RFEBlockPredicate, PenetrationStats> blockBreaking = new LinkedHashMap<>();

        public RFEProjectilePenetrationProperties build() {
            return new RFEProjectilePenetrationProperties(this.defaultEntityPenetration,
                    ImmutableMap.copyOf(this.entityPenetration),
                    this.defaultBlockPenetration,
                    ImmutableMap.copyOf(this.blockPenetration),
                    this.defaultBlockBreaking,
                    ImmutableMap.copyOf(this.blockBreaking));
        }
    }

    public record ClientboundSyncProjectilePenetrationPacket(Map<ResourceLocation, RFEProjectilePenetrationProperties> map) implements RFEPacket {
        public static ClientboundSyncProjectilePenetrationPacket decode(FriendlyByteBuf buf) {
            int sz = buf.readVarInt();
            Map<ResourceLocation, RFEProjectilePenetrationProperties> map = new Object2ReferenceOpenHashMap<>();
            for (int i = 0; i < sz; ++i) {
                ResourceLocation id = buf.readResourceLocation();
                RFEProjectilePenetrationProperties properties = RFEProjectilePenetrationProperties.fromNetwork(buf);
                map.put(id, properties);
            }
            return new ClientboundSyncProjectilePenetrationPacket(map);
        }

        ClientboundSyncProjectilePenetrationPacket() { this(new Object2ReferenceOpenHashMap<>(PENETRATION_PROPERTIES)); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.map.size());
            for (Map.Entry<ResourceLocation, RFEProjectilePenetrationProperties> entry : this.map.entrySet()) {
                buf.writeResourceLocation(entry.getKey());
                RFEProjectilePenetrationProperties.toNetwork(buf, entry.getValue());
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            PENETRATION_PROPERTIES.clear();
            PENETRATION_PROPERTIES.putAll(this.map);
        }
    }

}
