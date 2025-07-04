package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEProjectileTypeHandler {

    private static final Map<ResourceLocation, RFEProjectileType> PROJECTILE_TYPES = new Object2ReferenceOpenHashMap<>();
    private static final Map<RFEProjectileType, ResourceLocation> PROJECTILE_TYPE_IDS = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/projectile_types"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object for RFE projectile type");
                    JsonObject obj = el.getAsJsonObject();
                    ResourceLocation typeId = RFEUtils.location(GsonHelper.getAsString(obj, "type"));
                    RFEProjectileType.Serializer<?> ser = RFEContentBuilderRegistry.getProjectileTypeSerializer(typeId);
                    RFEProjectileType type = ser.fromJson(obj);
                    PROJECTILE_TYPES.put(id, type);
                    PROJECTILE_TYPE_IDS.put(type, id);
                } catch (Exception e) {
                    LOGGER.error("Error loading projectile type {}: {}", id, e);
                }
            }
        }
    }

    private static void clear() {
        PROJECTILE_TYPES.clear();
        PROJECTILE_TYPE_IDS.clear();
    }

    @Nullable public static RFEProjectileType getProjectileType(ResourceLocation id) { return PROJECTILE_TYPES.get(id); }

    @Nullable public static ResourceLocation getProjectileTypeId(RFEProjectileType type) { return PROJECTILE_TYPE_IDS.get(type); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncRFEProjectileTypesPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncRFEProjectileTypesPacket(), player);
    }

    public record ClientboundSyncRFEProjectileTypesPacket(Map<ResourceLocation, RFEProjectileType> projectileTypes) implements RFEPacket {
        public ClientboundSyncRFEProjectileTypesPacket() { this(new Object2ReferenceOpenHashMap<>(PROJECTILE_TYPES)); }

        public static ClientboundSyncRFEProjectileTypesPacket decode(FriendlyByteBuf buf) {
            Map<ResourceLocation, RFEProjectileType> projectileTypes = new Object2ReferenceOpenHashMap<>();
            int sz = buf.readVarInt();
            for (int i = 0; i < sz; ++i) {
                ResourceLocation typeId = buf.readResourceLocation();
                RFEProjectileType.Serializer<?> ser = RFEContentBuilderRegistry.getProjectileTypeSerializer(buf.readResourceLocation());
                RFEProjectileType projectileType = ser.fromNetwork(buf);
                projectileTypes.put(typeId, projectileType);
            }
            return new ClientboundSyncRFEProjectileTypesPacket(projectileTypes);
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.projectileTypes.size());
            for (Map.Entry<ResourceLocation, RFEProjectileType> entry : this.projectileTypes.entrySet()) {
                buf.writeResourceLocation(entry.getKey());
                buf.writeResourceLocation(RFEContentBuilderRegistry.getProjectileTypeSerializerId(entry.getValue().getSerializer()));
                toNetworkCasted(buf, entry.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        private static <T extends RFEProjectileType> void toNetworkCasted(FriendlyByteBuf buf, T type) {
            RFEProjectileType.Serializer<T> ser = (RFEProjectileType.Serializer<T>) type.getSerializer();
            ser.toNetwork(buf, type);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            PROJECTILE_TYPES.clear();
            PROJECTILE_TYPES.putAll(this.projectileTypes);
            PROJECTILE_TYPE_IDS.clear();
            for (Map.Entry<ResourceLocation, RFEProjectileType> entry : this.projectileTypes.entrySet())
                PROJECTILE_TYPE_IDS.put(entry.getValue(), entry.getKey());
        }
    }

}
