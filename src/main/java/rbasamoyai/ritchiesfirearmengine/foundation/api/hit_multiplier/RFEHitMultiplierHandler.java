package rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEHitMultiplierHandler {

    private static final Multimap<ResourceLocation, RFEHitMultiplier> HIT_MULTIPLIERS = LinkedHashMultimap.create();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Codec<Pair<Boolean, List<RFEHitMultiplier>>> CODEC = RecordCodecBuilder.create(o -> o.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(Pair::getFirst),
            RFEHitMultiplier.LIST_CODEC.fieldOf("hit_multipliers").forGetter(Pair::getSecond)
    ).apply(o, Pair::of));

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/hit_multipliers"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            HIT_MULTIPLIERS.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Pair<Boolean, List<RFEHitMultiplier>> result = CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    if (result.getFirst())
                        HIT_MULTIPLIERS.removeAll(id);
                    HIT_MULTIPLIERS.putAll(id, result.getSecond());
                } catch (Exception e) {
                    LOGGER.warn("Error occurred loading hit multiplier data for projectile type {}: {}", id, e);
                }
            }
        }
    }

    public static Collection<RFEHitMultiplier> getHitMultipliers(ResourceLocation id) { return HIT_MULTIPLIERS.get(id); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncHitMultipliersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncHitMultipliersPacket(), player);
    }

    public record ClientboundSyncHitMultipliersPacket(Multimap<ResourceLocation, RFEHitMultiplier> hitMultipliers) implements RFEPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncHitMultipliersPacket> STREAM_CODEC =
                StreamCodec.<RegistryFriendlyByteBuf, Pair<ResourceLocation, RFEHitMultiplier>, ResourceLocation, RFEHitMultiplier>composite(
                        ResourceLocation.STREAM_CODEC, Pair::getFirst,
                        RFEHitMultiplier.STREAM_CODEC, Pair::getSecond,
                        Pair::of).apply(ByteBufCodecs.list())
                        .map(ClientboundSyncHitMultipliersPacket::fromPairList, ClientboundSyncHitMultipliersPacket::fromMultimap);

        ClientboundSyncHitMultipliersPacket() { this(LinkedHashMultimap.create(HIT_MULTIPLIERS)); }

        private static ClientboundSyncHitMultipliersPacket fromPairList(List<Pair<ResourceLocation, RFEHitMultiplier>> list) {
            Multimap<ResourceLocation, RFEHitMultiplier> map = LinkedHashMultimap.create();
            for (Pair<ResourceLocation, RFEHitMultiplier> pair : list)
                map.put(pair.getFirst(), pair.getSecond());
            return new ClientboundSyncHitMultipliersPacket(map);
        }

        private List<Pair<ResourceLocation, RFEHitMultiplier>> fromMultimap() {
            return this.hitMultipliers.entries().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).toList();
        }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            HIT_MULTIPLIERS.clear();
            HIT_MULTIPLIERS.putAll(this.hitMultipliers);
        }
    }

}
