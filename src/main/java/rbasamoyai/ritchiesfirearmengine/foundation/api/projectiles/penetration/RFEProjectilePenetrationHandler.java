package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEBlockPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEEntityTypePredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationProperties.PenetrationStats;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
                    PropertiesLayer layer = PropertiesLayer.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    PropertiesBuilder builder = builtObjects.computeIfAbsent(id, k -> new PropertiesBuilder());
                    applyLoadedData(builder, layer);
                } catch (Exception e) {
                    LOGGER.error("Error loading projectile penetration properties {}: {}", id, e);
                }
            }

            for (Map.Entry<ResourceLocation, PropertiesBuilder> entry : builtObjects.entrySet())
                PENETRATION_PROPERTIES.put(entry.getKey(), entry.getValue().build());
        }
    }
    
    private static final PenetrationStats DEFAULT_PENETRATION_STATS = new PenetrationStats(1, 0.25f);

    public static RFEProjectilePenetrationProperties getPenetrationProperties(ResourceLocation id) {
        return PENETRATION_PROPERTIES.getOrDefault(id, DEFAULT_PENETRATION);
    }

    public static void syncToAll() { RFENetwork.sendToAll(new ClientboundSyncProjectilePenetrationPacket()); }

    public static void syncToPlayer(ServerPlayer player) { RFENetwork.sendToPlayer(new ClientboundSyncProjectilePenetrationPacket(), player); }

    private static void applyLoadedData(PropertiesBuilder builder, PropertiesLayer layer) {
        if (layer.defaultEntityPenetration != null)
            builder.defaultEntityPenetration = layer.defaultEntityPenetration;
        if (layer.replaceEntityPenetration)
            builder.entityPenetration.clear();
        builder.entityPenetration.putAll(layer.entityPenetration);

        if (layer.defaultBlockPenetration != null)
            builder.defaultBlockPenetration = layer.defaultBlockPenetration;
        if (layer.replaceBlockPenetration)
            builder.blockPenetration.clear();
        builder.blockPenetration.putAll(layer.blockPenetration);

        if (layer.defaultBlockBreaking != null)
            builder.defaultBlockBreaking = layer.defaultBlockBreaking;
        if (layer.replaceBlockBreaking)
            builder.blockBreaking.clear();
        builder.blockBreaking.putAll(layer.blockBreaking);
    }

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

    private record PropertiesLayer(@Nullable PenetrationStats defaultEntityPenetration, boolean replaceEntityPenetration,
                                   Map<RFEEntityTypePredicate, PenetrationStats> entityPenetration,
                                   @Nullable PenetrationStats defaultBlockPenetration, boolean replaceBlockPenetration,
                                   Map<RFEBlockPredicate, PenetrationStats> blockPenetration,
                                   @Nullable PenetrationStats defaultBlockBreaking, boolean replaceBlockBreaking,
                                   Map<RFEBlockPredicate, PenetrationStats> blockBreaking) {
        private static final Codec<Pair<RFEEntityTypePredicate, PenetrationStats>> ENTITY_PENETRATION_CODEC =
                Codec.either(
                        RecordCodecBuilder.<Pair<RFEEntityTypePredicate, PenetrationStats>>create(o -> o.group(
                                RFEEntityTypePredicate.CODEC.fieldOf("type").forGetter(Pair::getFirst),
                                PenetrationStats.CODEC.forGetter(Pair::getSecond)
                        ).apply(o, Pair::of)),
                        RFEEntityTypePredicate.CODEC
                ).xmap(either -> Either.unwrap(either.mapRight(pred -> new Pair<>(pred, DEFAULT_PENETRATION_STATS))), Either::left);

        private static final Codec<Map<RFEEntityTypePredicate, PenetrationStats>> ENTITY_PENETRATION_MAP_CODEC =
                ENTITY_PENETRATION_CODEC.listOf()
                        .xmap(li -> li.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
                                map -> map.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).toList());

        private static final Codec<Pair<RFEBlockPredicate, PenetrationStats>> BLOCK_PENETRATION_CODEC =
                Codec.either(
                        RecordCodecBuilder.<Pair<RFEBlockPredicate, PenetrationStats>>create(o -> o.group(
                                RFEBlockPredicate.CODEC.fieldOf("block").forGetter(Pair::getFirst),
                                PenetrationStats.CODEC.forGetter(Pair::getSecond)
                        ).apply(o, Pair::of)),
                        RFEBlockPredicate.CODEC
                ).xmap(either -> Either.unwrap(either.mapRight(pred -> new Pair<>(pred, DEFAULT_PENETRATION_STATS))), Either::left);

        private static final Codec<Map<RFEBlockPredicate, PenetrationStats>> BLOCK_PENETRATION_MAP_CODEC =
                BLOCK_PENETRATION_CODEC.listOf()
                        .xmap(li -> li.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
                                map -> map.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).toList());

        private static final Codec<PropertiesLayer> CODEC = RecordCodecBuilder.create(o -> o.group(
                PenetrationStats.CODEC.codec().optionalFieldOf("default_entity_penetration").xmap(op -> op.orElse(null), Optional::ofNullable)
                        .forGetter(PropertiesLayer::defaultEntityPenetration),
                Codec.BOOL.optionalFieldOf("replace_entity_penetration", false).forGetter(PropertiesLayer::replaceEntityPenetration),
                ENTITY_PENETRATION_MAP_CODEC.optionalFieldOf("entity_penetration", Map.of()).forGetter(PropertiesLayer::entityPenetration),
                PenetrationStats.CODEC.codec().optionalFieldOf("default_block_penetration").xmap(op -> op.orElse(null), Optional::ofNullable)
                        .forGetter(PropertiesLayer::defaultBlockPenetration),
                Codec.BOOL.optionalFieldOf("replace_block_penetration", false).forGetter(PropertiesLayer::replaceBlockPenetration),
                BLOCK_PENETRATION_MAP_CODEC.optionalFieldOf("block_penetration", Map.of()).forGetter(PropertiesLayer::blockPenetration),
                PenetrationStats.CODEC.codec().optionalFieldOf("default_block_breaking").xmap(op -> op.orElse(null), Optional::ofNullable)
                        .forGetter(PropertiesLayer::defaultBlockBreaking),
                Codec.BOOL.optionalFieldOf("replace_block_breaking", false).forGetter(PropertiesLayer::replaceBlockBreaking),
                BLOCK_PENETRATION_MAP_CODEC.optionalFieldOf("block_breaking", Map.of()).forGetter(PropertiesLayer::blockBreaking)
        ).apply(o, PropertiesLayer::new));
    }

    public record ClientboundSyncProjectilePenetrationPacket(Object2ReferenceOpenHashMap<ResourceLocation, RFEProjectilePenetrationProperties> map) implements RFEPacket {
        ClientboundSyncProjectilePenetrationPacket() { this(new Object2ReferenceOpenHashMap<>(PENETRATION_PROPERTIES)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncProjectilePenetrationPacket> STREAM_CODEC =
                ByteBufCodecs.map(Object2ReferenceOpenHashMap::new, ResourceLocation.STREAM_CODEC, RFEProjectilePenetrationProperties.STREAM_CODEC)
                        .map(ClientboundSyncProjectilePenetrationPacket::new, ClientboundSyncProjectilePenetrationPacket::map);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            PENETRATION_PROPERTIES.clear();
            PENETRATION_PROPERTIES.putAll(this.map);
        }
    }

}
