package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil.NoRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import java.util.Map;
import java.util.concurrent.Executor;

public class RFERecoilProviderPackHandler {

    private static final Map<Item, RFEFirearmProperties<RFERecoilProvider>> RECOIL_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmProperties<RFERecoilProvider> NO_RECOIL = new RFEFirearmProperties<>(NoRecoilProvider.INSTANCE, ImmutableMap.of());

    private static final Codec<RFEFirearmProperties<RFERecoilProvider>> CODEC = RFEFirearmProperties.makeCodec(RFERecoilProvider.CODEC);

    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_recoil"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            RECOIL_PROVIDERS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    RFEFirearmProperties<RFERecoilProvider> properties = CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));
                    RECOIL_PROVIDERS.put(item, properties);
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm recoil properties for item {}: {}", id, e);
                }
            }
        }
    }

    public static RFEFirearmProperties<RFERecoilProvider> getRecoilProviders(Item item) { return RECOIL_PROVIDERS.getOrDefault(item, NO_RECOIL); }

    public static RFEFirearmProperties<RFERecoilProvider> getRecoilProviders(ItemStack itemStack) { return getRecoilProviders(itemStack.getItem()); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new RFERecoilProviderPackHandler.ClientboundSyncRecoilProvidersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncRecoilProvidersPacket(), player);
    }

    public record ClientboundSyncRecoilProvidersPacket(Reference2ObjectOpenHashMap<Item, RFEFirearmProperties<RFERecoilProvider>> recoilProviders) implements RFEPacket {
        private ClientboundSyncRecoilProvidersPacket() { this(new Reference2ObjectOpenHashMap<>(RECOIL_PROVIDERS)); }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncRecoilProvidersPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM),
                        RFEFirearmProperties.makeStreamCodec(RFERecoilProvider.STREAM_CODEC))
                        .map(ClientboundSyncRecoilProvidersPacket::new, ClientboundSyncRecoilProvidersPacket::recoilProviders);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            RECOIL_PROVIDERS.clear();
            RECOIL_PROVIDERS.putAll(this.recoilProviders);
        }
    }

}
