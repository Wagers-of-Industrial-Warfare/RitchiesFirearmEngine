package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class AmmoPacketItemPropertiesHandler {

    private static final Map<Item, AmmoPacketItemProperties> PROPERTIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, AmmoPacketItemProperties> DEFAULT_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/ammo_packet_properties"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            PROPERTIES.clear();

            Map<Item, AmmoPacketItemProperties.Builder> builders = new Reference2ObjectOpenHashMap<>();
            for (Map.Entry<Item, AmmoPacketItemProperties> entry : DEFAULT_PROPERTIES.entrySet())
                builders.put(entry.getKey(), AmmoPacketItemProperties.Builder.fromExistingProperties(entry.getValue()));

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    AmmoPacketItemProperties.Layer layer = AmmoPacketItemProperties.Layer.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    AmmoPacketItemProperties.Builder builder = builders.computeIfAbsent(item, i -> AmmoPacketItemProperties.builder());
                    applyLoadedData(builder, layer);
                } catch (Exception e) {
                    LOGGER.warn("Error occurred loading ammo packet item data for {}: {}", id, e);
                }
            }

            for (Map.Entry<Item, AmmoPacketItemProperties.Builder> builder : builders.entrySet())
                PROPERTIES.put(builder.getKey(), builder.getValue().build());
        }
    }

    private static void applyLoadedData(AmmoPacketItemProperties.Builder builder, AmmoPacketItemProperties.Layer layer) {
        if (layer.replaceAmmoCapacities)
            builder.ammoCapacities.clear();
        builder.ammoCapacities.putAll(layer.ammoCapacities);
    }

    @ApiStatus.Internal
    public static void registerDefaults(Item item, ImmutableMap<AmmoPredicate, Integer> ammoCapacities) {
        if (DEFAULT_PROPERTIES.containsKey(item))
            throw new IllegalStateException("Already registered default ammo packet properties for item");
        DEFAULT_PROPERTIES.put(item, new AmmoPacketItemProperties(ammoCapacities));
    }

    @Nullable
    public static ImmutableMap<AmmoPredicate, Integer> getAmmoCapacities(Item item) {
        return PROPERTIES.containsKey(item) ? PROPERTIES.get(item).ammoCapacities : null;
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncAmmoPacketPropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncAmmoPacketPropertiesPacket());
    }

    public record ClientboundSyncAmmoPacketPropertiesPacket(Reference2ObjectOpenHashMap<Item, AmmoPacketItemProperties> properties) implements RFEPacket {
        ClientboundSyncAmmoPacketPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(PROPERTIES)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncAmmoPacketPropertiesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM), AmmoPacketItemProperties.STREAM_CODEC)
                        .map(ClientboundSyncAmmoPacketPropertiesPacket::new, ClientboundSyncAmmoPacketPropertiesPacket::properties);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            PROPERTIES.clear();
            PROPERTIES.putAll(this.properties);
        }
    }

    // TODO secondary ammo capacities
    private record AmmoPacketItemProperties(ImmutableMap<AmmoPredicate, Integer> ammoCapacities) {
        private static final StreamCodec<RegistryFriendlyByteBuf, AmmoPacketItemProperties> STREAM_CODEC = StreamCodec.composite(
                RFEByteBufCodecUtils.immutableMap(AmmoPredicate.STREAM_CODEC, ByteBufCodecs.VAR_INT), AmmoPacketItemProperties::ammoCapacities,
                AmmoPacketItemProperties::new);

        public static Builder builder() { return new Builder(); }

        private static class Builder {
            public final Map<AmmoPredicate, Integer> ammoCapacities = new LinkedHashMap<>();

            public AmmoPacketItemProperties build() {
                return new AmmoPacketItemProperties(ImmutableMap.<AmmoPredicate, Integer>builder().putAll(this.ammoCapacities).build());
            }

            public static Builder fromExistingProperties(AmmoPacketItemProperties properties) {
                Builder builder = new Builder();
                builder.ammoCapacities.putAll(properties.ammoCapacities);
                return builder;
            }
        }

        private record Layer(boolean replaceAmmoCapacities, Map<AmmoPredicate, Integer> ammoCapacities) {
            private static final Codec<Layer> CODEC = RecordCodecBuilder.create(o -> o.group(
                    Codec.BOOL.optionalFieldOf("replace_ammo", false).forGetter(Layer::replaceAmmoCapacities),
                    ExtraCodecs.strictUnboundedMap(AmmoPredicate.CODEC.fieldOf("ammo").codec(), Codec.intRange(0, Integer.MAX_VALUE).fieldOf("capacity").codec())
                            .optionalFieldOf("ammo", new LinkedHashMap<>()).forGetter(Layer::ammoCapacities)
            ).apply(o, Layer::new));
        }
    }

}
