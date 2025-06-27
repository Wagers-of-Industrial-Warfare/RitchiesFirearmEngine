package rbasamoyai.ritchiesfirearmengine.content.ammo;

import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class AmmoPacketItemPropertiesHandler {

    private static final Map<Item, Map<AmmoPredicate, Integer>> AMMO_CAPACITIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, Map<AmmoPredicate, Integer>> DEFAULT_AMMO_CAPACITIES = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/ammo_packet_properties"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            resetAmmoLoaders();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        continue;
                    loadData(item, el.getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.warn("Error occurred loading ammo packet item data for {}: {}", id, e);
                }
            }
        }
    }

    private static void loadData(Item item, JsonObject obj) {
        if (GsonHelper.isArrayNode(obj, "ammo")) {
            boolean replace = GsonHelper.getAsBoolean(obj, "replace", false);
            if (replace)
                AMMO_CAPACITIES.remove(item);
            if (!AMMO_CAPACITIES.containsKey(item))
                AMMO_CAPACITIES.put(item, new Object2IntLinkedOpenHashMap<>());
            Map<AmmoPredicate, Integer> capacities = AMMO_CAPACITIES.get(item);
            JsonArray ammoArr = GsonHelper.getAsJsonArray(obj, "ammo");
            for (JsonElement el : ammoArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object value for ammo capacity");
                JsonObject capObj = el.getAsJsonObject();
                String str = GsonHelper.getAsString(capObj, "ammo");
                AmmoPredicate pred = AmmoPredicate.fromString(str);
                int capacity = GsonHelper.getAsInt(capObj, "capacity");
                if (capacities.put(pred, capacity) != null)
                    LOGGER.warn("Duplicate item predicate entry");
            }
        }
    }

    private static void resetAmmoLoaders() {
        AMMO_CAPACITIES.clear();
        AMMO_CAPACITIES.putAll(DEFAULT_AMMO_CAPACITIES);
    }

    public static void registerDefaults(Item item, Map<AmmoPredicate, Integer> ammoPredicates) {
        DEFAULT_AMMO_CAPACITIES.put(item, ammoPredicates);
    }

    @Nullable public static Map<AmmoPredicate, Integer> getAmmoCapacities(Item item) { return AMMO_CAPACITIES.get(item); }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncAmmoPacketPropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncAmmoPacketPropertiesPacket());
    }

    public record ClientboundSyncAmmoPacketPropertiesPacket(Map<Item, Map<AmmoPredicate, Integer>> capacities) implements RFEPacket {
        public ClientboundSyncAmmoPacketPropertiesPacket() { this(AMMO_CAPACITIES); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(AMMO_CAPACITIES.size());
            for (Map.Entry<Item, Map<AmmoPredicate, Integer>> entry : AMMO_CAPACITIES.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                Map<AmmoPredicate, Integer> capacities = entry.getValue();
                buf.writeVarInt(capacities.size());
                for (Map.Entry<AmmoPredicate, Integer> capacityEntry : capacities.entrySet()) {
                    AmmoPredicate.writeToNetwork(capacityEntry.getKey(), buf);
                    buf.writeVarInt(capacityEntry.getValue());
                }
            }
        }

        public static ClientboundSyncAmmoPacketPropertiesPacket decode(FriendlyByteBuf buf) {
            int ammoSz = buf.readVarInt();
            Map<Item, Map<AmmoPredicate, Integer>> ammoCapacitiesByItem = new Reference2ObjectOpenHashMap<>();
            for (int itemInd = 0; itemInd < ammoSz; ++itemInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                Map<AmmoPredicate, Integer> capacities = new Object2IntLinkedOpenHashMap<>();
                for (int capacityInd = 0; capacityInd < predSz; ++capacityInd)
                    capacities.put(AmmoPredicate.fromNetwork(buf), buf.readVarInt());
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    ammoCapacitiesByItem.put(i, capacities);
                }, () -> LOGGER.warn("Attempted to sync missing item {}", loc));
            }
            return new ClientboundSyncAmmoPacketPropertiesPacket(ammoCapacitiesByItem);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            AMMO_CAPACITIES.clear();
            AMMO_CAPACITIES.putAll(this.capacities);
        }
    }

}
