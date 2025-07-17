package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread.NoSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFESpreadProviderPackHandler {

    private static final Map<Item, RFEFirearmProperties<RFESpreadProvider>> SPREAD_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmProperties<RFESpreadProvider> NO_SPREAD = new RFEFirearmProperties<>(NoSpreadProvider.INSTANCE, ImmutableMap.of());

    private static final Logger LOGGER = LogUtils.getLogger();

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
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object when parsing firearm item spread properties");
                    JsonObject obj = el.getAsJsonObject();
                    SPREAD_PROVIDERS.put(item, readSpreadProviderProperties(obj, id));
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm spread properties for item {}: {}", id, e);
                }
            }
        }
    }

    private static RFEFirearmProperties<RFESpreadProvider> readSpreadProviderProperties(JsonObject obj, ResourceLocation id) {
        RFESpreadProvider defaultProvider = readSpreadProvider(obj);
        ImmutableMap.Builder<String, RFESpreadProvider> propertiesByMode = ImmutableMap.builder();
        if (GsonHelper.isObjectNode(obj, "modes")) {
            JsonObject modesObj = obj.getAsJsonObject("modes");
            for (Map.Entry<String, JsonElement> entry : modesObj.entrySet()) {
                String modeName = entry.getKey();
                JsonElement el = entry.getValue();
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object when parsing spread provider for mode '" + modeName + "' of firearm item " + id);
                propertiesByMode.put(modeName, readSpreadProvider(el.getAsJsonObject()));
            }
        }
        return new RFEFirearmProperties<>(defaultProvider, propertiesByMode.build());
    }

    private static RFESpreadProvider readSpreadProvider(JsonObject obj) {
        ResourceLocation typeId = RFEUtils.location(GsonHelper.getAsString(obj, "type"));
        RFESpreadProvider.Serializer<?> ser = RFEContentBuilderRegistry.getSpreadProviderSerializer(typeId);
        return ser.fromJson(obj);
    }

    public static RFEFirearmProperties<RFESpreadProvider> getSpreadProviders(Item item) { return SPREAD_PROVIDERS.getOrDefault(item, NO_SPREAD); }

    public static RFEFirearmProperties<RFESpreadProvider> getSpreadProviders(ItemStack itemStack) { return getSpreadProviders(itemStack.getItem()); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncSpreadProvidersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncSpreadProvidersPacket(), player);
    }

    public record ClientboundSyncSpreadProvidersPacket(Map<Item, RFEFirearmProperties<RFESpreadProvider>> spreadProviders) implements RFEPacket {
        private ClientboundSyncSpreadProvidersPacket() { this(new Reference2ObjectOpenHashMap<>(SPREAD_PROVIDERS)); }

        public static ClientboundSyncSpreadProvidersPacket decode(FriendlyByteBuf buf) {
            int sz = buf.readVarInt();
            ImmutableMap.Builder<Item, RFEFirearmProperties<RFESpreadProvider>> provs = ImmutableMap.builder();
            for (int i = 0; i < sz; ++i) {
                ResourceLocation itemLoc = buf.readResourceLocation();
                RFEFirearmProperties<RFESpreadProvider> prov = RFEFirearmProperties.fromNetwork(buf, ClientboundSyncSpreadProvidersPacket::fromNetwork);
                BuiltInRegistries.ITEM.getOptional(itemLoc).ifPresent(item -> provs.put(item, prov));
            }
            return new ClientboundSyncSpreadProvidersPacket(provs.build());
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.spreadProviders.size());
            for (Map.Entry<Item, RFEFirearmProperties<RFESpreadProvider>> entry : this.spreadProviders.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                RFEFirearmProperties.toNetwork(buf, entry.getValue(), ClientboundSyncSpreadProvidersPacket::toNetworkCasted);
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            SPREAD_PROVIDERS.clear();
            SPREAD_PROVIDERS.putAll(this.spreadProviders);
        }

        private static <T extends RFESpreadProvider> void toNetworkCasted(FriendlyByteBuf buf, T prov) {
            RFESpreadProvider.Serializer<T> ser = (RFESpreadProvider.Serializer<T>) prov.getSerializer();
            buf.writeResourceLocation(RFEContentBuilderRegistry.getSpreadProviderSerializerId(ser));
            ser.toNetwork(buf, prov);
        }

        private static RFESpreadProvider fromNetwork(FriendlyByteBuf buf) {
            RFESpreadProvider.Serializer<?> ser = RFEContentBuilderRegistry.getSpreadProviderSerializer(buf.readResourceLocation());
            return ser.fromNetwork(buf);
        }
    }

}
