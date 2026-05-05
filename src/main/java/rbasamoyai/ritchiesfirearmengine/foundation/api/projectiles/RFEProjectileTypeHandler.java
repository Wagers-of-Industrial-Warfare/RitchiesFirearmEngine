package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashBigSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

public class RFEProjectileTypeHandler {

    public static final StreamCodec<ByteBuf, RFEProjectileType> LOADED_TYPE_STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
            rl -> Objects.requireNonNull(RFEProjectileTypeHandler.getProjectileType(rl)),
            t -> Objects.requireNonNull(RFEProjectileTypeHandler.getProjectileTypeId(t)));

    private static final Map<ResourceLocation, RFEProjectileType> PROJECTILE_TYPES = new Object2ReferenceOpenHashMap<>();
    private static final Map<RFEProjectileType, ResourceLocation> PROJECTILE_TYPE_IDS = new Reference2ObjectOpenHashMap<>();
    private static final Set<ResourceLocation> SUBPROJECTILE_TYPE_IDS = new ObjectOpenHashBigSet<>();

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
                    RFEProjectileType type = RFEProjectileType.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(m -> new IllegalStateException("Error reading RFE projectile type properties: " + m));
                    PROJECTILE_TYPES.put(id, type);
                    PROJECTILE_TYPE_IDS.put(type, id);
                    if (type instanceof RFEProjectileType.HasCombinedProjectiles combined) {
                        for (Map.Entry<String, RFEProjectileType> extra : combined.getSubprojectileTypes().entrySet()) {
                            ResourceLocation extraId = id.withSuffix("_" + extra.getKey());
                            PROJECTILE_TYPES.put(extraId, extra.getValue());
                            PROJECTILE_TYPE_IDS.put(extra.getValue(), extraId);
                            SUBPROJECTILE_TYPE_IDS.add(extraId);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading projectile type {}: {}", id, e);
                }
            }
        }
    }

    private static void clear() {
        PROJECTILE_TYPES.clear();
        PROJECTILE_TYPE_IDS.clear();
        SUBPROJECTILE_TYPE_IDS.clear();
    }

    @Nullable public static RFEProjectileType getProjectileType(ResourceLocation id) { return PROJECTILE_TYPES.get(id); }

    @Nullable public static ResourceLocation getProjectileTypeId(RFEProjectileType type) { return PROJECTILE_TYPE_IDS.get(type); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncRFEProjectileTypesPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncRFEProjectileTypesPacket(), player);
    }

    public record ClientboundSyncRFEProjectileTypesPacket(Object2ReferenceOpenHashMap<ResourceLocation, RFEProjectileType> projectileTypes) implements RFEPacket {
        ClientboundSyncRFEProjectileTypesPacket() { this(getProjectileTypesNoSubprojectiles()); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncRFEProjectileTypesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Object2ReferenceOpenHashMap::new, ResourceLocation.STREAM_CODEC, RFEProjectileType.STREAM_CODEC)
                        .map(ClientboundSyncRFEProjectileTypesPacket::new, ClientboundSyncRFEProjectileTypesPacket::projectileTypes);

        private static Object2ReferenceOpenHashMap<ResourceLocation, RFEProjectileType> getProjectileTypesNoSubprojectiles() {
            Object2ReferenceOpenHashMap<ResourceLocation, RFEProjectileType> result = new Object2ReferenceOpenHashMap<>(PROJECTILE_TYPES);
            for (ResourceLocation subprojectileId : SUBPROJECTILE_TYPE_IDS)
                result.remove(subprojectileId);
            return result;
        }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            PROJECTILE_TYPES.clear();
            PROJECTILE_TYPES.putAll(this.projectileTypes);
            PROJECTILE_TYPE_IDS.clear();
            SUBPROJECTILE_TYPE_IDS.clear();
            for (Map.Entry<ResourceLocation, RFEProjectileType> entry : this.projectileTypes.entrySet()) {
                ResourceLocation id = entry.getKey();
                RFEProjectileType projectileType = entry.getValue();
                PROJECTILE_TYPE_IDS.put(projectileType, id);
                if (projectileType instanceof RFEProjectileType.HasCombinedProjectiles combined) {
                    for (Map.Entry<String, RFEProjectileType> extra : combined.getSubprojectileTypes().entrySet()) {
                        ResourceLocation extraId = id.withSuffix("_" + extra.getKey());
                        PROJECTILE_TYPES.put(extraId, extra.getValue());
                        PROJECTILE_TYPE_IDS.put(extra.getValue(), extraId);
                        SUBPROJECTILE_TYPE_IDS.add(extraId);
                    }
                }
            }
        }
    }

}
