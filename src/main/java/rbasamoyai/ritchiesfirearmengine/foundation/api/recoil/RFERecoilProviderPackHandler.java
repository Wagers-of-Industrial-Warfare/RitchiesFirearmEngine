package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil.NoRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFERecoilProviderPackHandler {

    private static final Map<Item, RFEFirearmProperties<RFERecoilProvider>> RECOIL_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmProperties<RFERecoilProvider> NO_RECOIL = new RFEFirearmProperties<>(NoRecoilProvider.INSTANCE, ImmutableMap.of());

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
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object when parsing firearm item recoil properties");
                    JsonObject obj = el.getAsJsonObject();
                    RECOIL_PROVIDERS.put(item, readRecoilProviderProperties(obj, id));
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm recoil properties for item {}: {}", id, e);
                }
            }
        }
    }

    private static RFEFirearmProperties<RFERecoilProvider> readRecoilProviderProperties(JsonObject obj, ResourceLocation id) {
        RFERecoilProvider defaultProvider = readRecoilProvider(obj);
        ImmutableMap.Builder<String, RFERecoilProvider> propertiesByMode = ImmutableMap.builder();
        if (GsonHelper.isObjectNode(obj, "modes")) {
            JsonObject modesObj = obj.getAsJsonObject("modes");
            for (Map.Entry<String, JsonElement> entry : modesObj.entrySet()) {
                String modeName = entry.getKey();
                JsonElement el = entry.getValue();
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object when parsing recoil provider for mode '" + modeName + "' of firearm item " + id);
                propertiesByMode.put(modeName, readRecoilProvider(el.getAsJsonObject()));
            }
        }
        return new RFEFirearmProperties<>(defaultProvider, propertiesByMode.build());
    }

    private static RFERecoilProvider readRecoilProvider(JsonObject obj) {
        ResourceLocation typeId = RFEUtils.location(GsonHelper.getAsString(obj, "type"));
        RFERecoilProvider.Serializer<?> ser = RFEContentBuilderRegistry.getRecoilProviderSerializer(typeId);
        return ser.fromJson(obj);
    }

    public static RFEFirearmProperties<RFERecoilProvider> getRecoilProviders(Item item) { return RECOIL_PROVIDERS.getOrDefault(item, NO_RECOIL); }

    public static RFEFirearmProperties<RFERecoilProvider> getRecoilProviders(ItemStack itemStack) { return getRecoilProviders(itemStack.getItem()); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new RFERecoilProviderPackHandler.ClientboundSyncRecoilProvidersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncRecoilProvidersPacket(), player);
    }

    public record ClientboundSyncRecoilProvidersPacket(Map<Item, RFEFirearmProperties<RFERecoilProvider>> recoilProviders) implements RFEPacket {
        private ClientboundSyncRecoilProvidersPacket() { this(new Reference2ObjectOpenHashMap<>(RECOIL_PROVIDERS)); }

        public static ClientboundSyncRecoilProvidersPacket decode(FriendlyByteBuf buf) {
            int sz = buf.readVarInt();
            ImmutableMap.Builder<Item, RFEFirearmProperties<RFERecoilProvider>> provs = ImmutableMap.builder();
            for (int i = 0; i < sz; ++i) {
                ResourceLocation itemLoc = buf.readResourceLocation();
                RFEFirearmProperties<RFERecoilProvider> prov = RFEFirearmProperties.fromNetwork(buf, ClientboundSyncRecoilProvidersPacket::fromNetwork);
                BuiltInRegistries.ITEM.getOptional(itemLoc).ifPresent(item -> provs.put(item, prov));
            }
            return new ClientboundSyncRecoilProvidersPacket(provs.build());
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.recoilProviders.size());
            for (Map.Entry<Item, RFEFirearmProperties<RFERecoilProvider>> entry : this.recoilProviders.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                RFEFirearmProperties.toNetwork(buf, entry.getValue(), ClientboundSyncRecoilProvidersPacket::toNetworkCasted);
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            RECOIL_PROVIDERS.clear();
            RECOIL_PROVIDERS.putAll(this.recoilProviders);
        }

        private static <T extends RFERecoilProvider> void toNetworkCasted(FriendlyByteBuf buf, T prov) {
            RFERecoilProvider.Serializer<T> ser = (RFERecoilProvider.Serializer<T>) prov.getSerializer();
            buf.writeResourceLocation(RFEContentBuilderRegistry.getRecoilProviderSerializerId(ser));
            ser.toNetwork(buf, prov);
        }

        private static RFERecoilProvider fromNetwork(FriendlyByteBuf buf) {
            RFERecoilProvider.Serializer<?> ser = RFEContentBuilderRegistry.getRecoilProviderSerializer(buf.readResourceLocation());
            return ser.fromNetwork(buf);
        }
    }

}
