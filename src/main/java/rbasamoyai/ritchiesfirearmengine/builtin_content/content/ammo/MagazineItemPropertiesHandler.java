package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableList;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class MagazineItemPropertiesHandler {

    private static final Reference2ObjectOpenHashMap<Item, MagazineItemProperties> DEFAULT_PROPERTIES = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Item, MagazineItemProperties> PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/magazine_properties"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            PROPERTIES.clear();

            Reference2ObjectOpenHashMap<Item, MagazineItemProperties.Builder> builders = new Reference2ObjectOpenHashMap<>();
            DEFAULT_PROPERTIES.reference2ObjectEntrySet().fastForEach(e -> {
                builders.put(e.getKey(), MagazineItemProperties.Builder.fromExistingProperties(e.getValue()));
            });

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    MagazineItemProperties.Layer layer = MagazineItemProperties.Layer.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    MagazineItemProperties.Builder builder = builders.computeIfAbsent(item, i -> MagazineItemProperties.builder());
                    applyData(builder, layer);
                } catch (Exception e) {
                    LOGGER.warn("Error occurred loading magazine item data for {}: {}", id, e);
                }
            }

            builders.reference2ObjectEntrySet().fastForEach(e -> {
                PROPERTIES.put(e.getKey(), e.getValue().build());
            });
        }
    }

    private static void applyData(MagazineItemProperties.Builder builder, MagazineItemProperties.Layer layer) {
        if (layer.replaceValidAmmo)
            builder.ammoPredicates.clear();
        builder.ammoPredicates.addAll(layer.ammoPredicates);
        if (layer.replaceValidSpeedloaders)
            builder.speedloaderPredicates.clear();
        builder.speedloaderPredicates.addAll(layer.speedloaderPredicates);
        if (layer.replaceValidSecondaries)
            builder.secondaryPredicates.clear();
        builder.secondaryPredicates.addAll(layer.secondaryPredicates);
    }

    @ApiStatus.Internal
    public static void registerDefaults(Item item, ImmutableList<AmmoPredicate> ammoPredicates,
                                        ImmutableList<AmmoPredicate> speedloaderPredicates,
                                        ImmutableList<AmmoPredicate> secondaryPredicates) {
        if (DEFAULT_PROPERTIES.containsKey(item))
            throw new IllegalStateException("Already registered default magazine properties for item");
        DEFAULT_PROPERTIES.put(item, new MagazineItemProperties(ammoPredicates, speedloaderPredicates, secondaryPredicates));
    }

    @Nullable
    public static ImmutableList<AmmoPredicate> getValidAmmoPredicates(Item item) {
        return PROPERTIES.containsKey(item) ? PROPERTIES.get(item).ammoPredicates : null;
    }

    @Nullable
    public static ImmutableList<AmmoPredicate> getValidSpeedloaderPredicates(Item item) {
        return PROPERTIES.containsKey(item) ? PROPERTIES.get(item).speedloaderPredicates : null;
    }

    @Nullable
    public static ImmutableList<AmmoPredicate> getValidSecondaryPredicates(Item item) {
        return PROPERTIES.containsKey(item) ? PROPERTIES.get(item).secondaryPredicates : null;
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncMagazinePropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncMagazinePropertiesPacket());
    }

    public record ClientboundSyncMagazinePropertiesPacket(Reference2ObjectOpenHashMap<Item, MagazineItemProperties> properties) implements RFEPacket {
        ClientboundSyncMagazinePropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(PROPERTIES)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncMagazinePropertiesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM), MagazineItemProperties.STREAM_CODEC)
                        .map(ClientboundSyncMagazinePropertiesPacket::new, ClientboundSyncMagazinePropertiesPacket::properties);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            PROPERTIES.clear();
            PROPERTIES.putAll(this.properties);
        }
    }

    private record MagazineItemProperties(ImmutableList<AmmoPredicate> ammoPredicates,
                                          ImmutableList<AmmoPredicate> speedloaderPredicates,
                                          ImmutableList<AmmoPredicate> secondaryPredicates) {
        private static final StreamCodec<RegistryFriendlyByteBuf, MagazineItemProperties> STREAM_CODEC = StreamCodec.composite(
                AmmoPredicate.STREAM_CODEC.apply(RFEByteBufCodecUtils.immutableList()), MagazineItemProperties::ammoPredicates,
                AmmoPredicate.STREAM_CODEC.apply(RFEByteBufCodecUtils.immutableList()), MagazineItemProperties::speedloaderPredicates,
                AmmoPredicate.STREAM_CODEC.apply(RFEByteBufCodecUtils.immutableList()), MagazineItemProperties::secondaryPredicates,
                MagazineItemProperties::new);

        public static Builder builder() { return new Builder(); }

        private static class Builder {
            public final List<AmmoPredicate> ammoPredicates = new ArrayList<>();
            public final List<AmmoPredicate> speedloaderPredicates = new ArrayList<>();
            public final List<AmmoPredicate> secondaryPredicates = new ArrayList<>();

            public MagazineItemProperties build() {
                return new MagazineItemProperties(ImmutableList.copyOf(this.ammoPredicates),
                        ImmutableList.copyOf(this.speedloaderPredicates), ImmutableList.copyOf(this.secondaryPredicates));
            }

            public static Builder fromExistingProperties(MagazineItemProperties properties) {
                Builder builder = new Builder();
                builder.ammoPredicates.addAll(properties.ammoPredicates);
                builder.speedloaderPredicates.addAll(properties.speedloaderPredicates);
                builder.secondaryPredicates.addAll(properties.secondaryPredicates);
                return builder;
            }
        }

        private record Layer(boolean replaceValidAmmo, List<AmmoPredicate> ammoPredicates,
                             boolean replaceValidSpeedloaders, List<AmmoPredicate> speedloaderPredicates,
                             boolean replaceValidSecondaries, List<AmmoPredicate> secondaryPredicates) {
            private static final Codec<Layer> CODEC = RecordCodecBuilder.create(o -> o.group(
                    Codec.BOOL.optionalFieldOf("replace_valid_ammo", false).forGetter(Layer::replaceValidAmmo),
                    Codec.list(AmmoPredicate.CODEC).optionalFieldOf("valid_ammo", new ArrayList<>()).forGetter(Layer::ammoPredicates),
                    Codec.BOOL.optionalFieldOf("replace_valid_speedloaders", false).forGetter(Layer::replaceValidSpeedloaders),
                    Codec.list(AmmoPredicate.CODEC).optionalFieldOf("valid_speedloaders", new ArrayList<>()).forGetter(Layer::speedloaderPredicates),
                    Codec.BOOL.optionalFieldOf("replace_valid_secondaries", false).forGetter(Layer::replaceValidSecondaries),
                    Codec.list(AmmoPredicate.CODEC).optionalFieldOf("valid_secondaries", new ArrayList<>()).forGetter(Layer::secondaryPredicates)
            ).apply(o, Layer::new));
        }
    }

}
