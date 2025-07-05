package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class AmmoPacketItemPropertiesHandler {

    private static final Map<Item, ImmutableMap<AmmoPredicate, Integer>> AMMO_CAPACITIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, ImmutableMap<AmmoPredicate, Integer>> DEFAULT_AMMO_CAPACITIES = new Reference2ObjectOpenHashMap<>();

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
                        throw new JsonParseException("Expected JSON object when parsing ammo packet item data");
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
                AMMO_CAPACITIES.put(item, ImmutableMap.of());
            ImmutableMap<AmmoPredicate, Integer> capacities = AMMO_CAPACITIES.get(item);
            ImmutableMap.Builder<AmmoPredicate, Integer> capMod = ImmutableMap.builder();
            capMod.putAll(capacities);
            JsonArray ammoArr = GsonHelper.getAsJsonArray(obj, "ammo");
            for (JsonElement el : ammoArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object value for ammo capacity");
                JsonObject capObj = el.getAsJsonObject();
                String str = GsonHelper.getAsString(capObj, "ammo");
                AmmoPredicate pred = AmmoPredicate.fromString(str);
                int capacity = GsonHelper.getAsInt(capObj, "capacity");
                if (capacities.containsKey(pred))
                    LOGGER.warn("Duplicate item predicate entry for item {}", BuiltInRegistries.ITEM.getKey(item));
                capMod.put(pred, capacity);
            }
            AMMO_CAPACITIES.put(item, capMod.build());
        }
    }

    private static void resetAmmoLoaders() {
        AMMO_CAPACITIES.clear();
        AMMO_CAPACITIES.putAll(DEFAULT_AMMO_CAPACITIES);
    }

    public static void registerDefaults(Item item, ImmutableMap<AmmoPredicate, Integer> ammoPredicates) {
        DEFAULT_AMMO_CAPACITIES.put(item, ammoPredicates);
    }

    @Nullable public static ImmutableMap<AmmoPredicate, Integer> getAmmoCapacities(Item item) { return AMMO_CAPACITIES.get(item); }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncAmmoPacketPropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncAmmoPacketPropertiesPacket());
    }

    public record ClientboundSyncAmmoPacketPropertiesPacket(Map<Item, ImmutableMap<AmmoPredicate, Integer>> capacities) implements RFEPacket {
        ClientboundSyncAmmoPacketPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(AMMO_CAPACITIES)); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.capacities.size());
            for (Map.Entry<Item, ImmutableMap<AmmoPredicate, Integer>> entry : this.capacities.entrySet()) {
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
            Map<Item, ImmutableMap<AmmoPredicate, Integer>> ammoCapacitiesByItem = new Reference2ObjectOpenHashMap<>();
            for (int itemInd = 0; itemInd < ammoSz; ++itemInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                ImmutableMap.Builder<AmmoPredicate, Integer> capacities = ImmutableMap.builder();
                for (int capacityInd = 0; capacityInd < predSz; ++capacityInd)
                    capacities.put(AmmoPredicate.fromNetwork(buf), buf.readVarInt());
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    ammoCapacitiesByItem.put(i, capacities.build());
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
