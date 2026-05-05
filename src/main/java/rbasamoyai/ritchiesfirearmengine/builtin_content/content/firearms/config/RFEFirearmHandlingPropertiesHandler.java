package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeHandlingProperties;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class RFEFirearmHandlingPropertiesHandler {

    private static final Map<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> HANDLING_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Codec<ImmutableMap<String, RFEFirearmModeHandlingProperties>> CODEC = RecordCodecBuilder.create(o -> o.group(
            ExtraCodecs.strictUnboundedMap(Codec.STRING, RFEFirearmModeHandlingProperties.CODEC)
                    .xmap(map -> ImmutableMap.<String, RFEFirearmModeHandlingProperties>builder().putAll(map).build(), LinkedHashMap::new)
                    .fieldOf("modes").forGetter(Function.identity())
    ).apply(o, Function.identity()));

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_handling"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            HANDLING_PROPERTIES.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    ImmutableMap<String, RFEFirearmModeHandlingProperties> handling = CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    HANDLING_PROPERTIES.put(item, handling);
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm handling properties for item {}: {}", id, e);
                }
            }
        }
    }

    @Nonnull
    public static ImmutableMap<String, RFEFirearmModeHandlingProperties> getHandlingProperties(Item item) {
        return HANDLING_PROPERTIES.getOrDefault(item, ImmutableMap.of());
    }

    @Nonnull
    public static ImmutableMap<String, RFEFirearmModeHandlingProperties> getHandlingProperties(ItemStack itemStack) {
        return getHandlingProperties(itemStack.getItem());
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncFirearmHandlingPropertiesPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncFirearmHandlingPropertiesPacket(), player);
    }

    public record ClientboundSyncFirearmHandlingPropertiesPacket(Reference2ObjectOpenHashMap<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> properties) implements RFEPacket {
        ClientboundSyncFirearmHandlingPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(HANDLING_PROPERTIES)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncFirearmHandlingPropertiesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM),
                        RFEByteBufCodecUtils.immutableMap(ByteBufCodecs.STRING_UTF8, RFEFirearmModeHandlingProperties.STREAM_CODEC))
                        .map(ClientboundSyncFirearmHandlingPropertiesPacket::new, ClientboundSyncFirearmHandlingPropertiesPacket::properties);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            HANDLING_PROPERTIES.clear();
            HANDLING_PROPERTIES.putAll(this.properties);
        }
    }

}
