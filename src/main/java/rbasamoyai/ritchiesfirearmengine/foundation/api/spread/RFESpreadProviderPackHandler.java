package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread.NoSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import java.util.Map;
import java.util.concurrent.Executor;

public class RFESpreadProviderPackHandler {

    private static final Map<Item, RFEFirearmProperties<RFESpreadProvider>> SPREAD_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmProperties<RFESpreadProvider> NO_SPREAD = new RFEFirearmProperties<>(NoSpreadProvider.INSTANCE, ImmutableMap.of());

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Codec<RFEFirearmProperties<RFESpreadProvider>> CODEC = RFEFirearmProperties.makeCodec(RFESpreadProvider.CODEC);

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_spread"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            SPREAD_PROVIDERS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    RFEFirearmProperties<RFESpreadProvider> properties = CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    SPREAD_PROVIDERS.put(item, properties);
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm spread properties for item {}: {}", id, e);
                }
            }
        }
    }

    public static RFEFirearmProperties<RFESpreadProvider> getSpreadProviders(Item item) { return SPREAD_PROVIDERS.getOrDefault(item, NO_SPREAD); }

    public static RFEFirearmProperties<RFESpreadProvider> getSpreadProviders(ItemStack itemStack) { return getSpreadProviders(itemStack.getItem()); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncSpreadProvidersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncSpreadProvidersPacket(), player);
    }

    public record ClientboundSyncSpreadProvidersPacket(Reference2ObjectOpenHashMap<Item, RFEFirearmProperties<RFESpreadProvider>> spreadProviders) implements RFEPacket {
        private ClientboundSyncSpreadProvidersPacket() { this(new Reference2ObjectOpenHashMap<>(SPREAD_PROVIDERS)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncSpreadProvidersPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM),
                        RFEFirearmProperties.makeStreamCodec(RFESpreadProvider.STREAM_CODEC))
                        .map(ClientboundSyncSpreadProvidersPacket::new, ClientboundSyncSpreadProvidersPacket::spreadProviders);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            SPREAD_PROVIDERS.clear();
            SPREAD_PROVIDERS.putAll(this.spreadProviders);
        }
    }

}
