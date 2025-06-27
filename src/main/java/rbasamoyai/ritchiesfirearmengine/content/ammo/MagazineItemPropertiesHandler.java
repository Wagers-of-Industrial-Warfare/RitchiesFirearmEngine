package rbasamoyai.ritchiesfirearmengine.content.ammo;

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
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class MagazineItemPropertiesHandler {

    private static final Map<Item, List<AmmoPredicate>> VALID_AMMO = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, List<AmmoPredicate>> DEFAULT_VALID_AMMO = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, List<AmmoPredicate>> VALID_SPEEDLOADERS = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, List<AmmoPredicate>> DEFAULT_VALID_SPEEDLOADERS = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/magazine_properties"); }

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
                    LOGGER.warn("Error occurred loading magazine item data for {}: {}", id, e);
                }
            }
        }
    }

    private static void loadData(Item item, JsonObject obj) {
        if (GsonHelper.isArrayNode(obj, "valid_ammo")) {
            boolean replaceAmmo = GsonHelper.getAsBoolean(obj, "replace_valid_ammo", false);
            if (replaceAmmo)
                VALID_AMMO.remove(item);
            if (!VALID_AMMO.containsKey(item))
                VALID_AMMO.put(item, new LinkedList<>());
            List<AmmoPredicate> list = VALID_AMMO.get(item);
            JsonArray ammoArr = GsonHelper.getAsJsonArray(obj, "valid_ammo");
            for (JsonElement el : ammoArr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected string value for ammo predicate");
                list.add(AmmoPredicate.fromString(el.getAsString()));
            }
        }
        if (GsonHelper.isArrayNode(obj, "valid_speedloaders")) {
            boolean replaceSpeedloaders = GsonHelper.getAsBoolean(obj, "replace_valid_speedloaders", false);
            if (replaceSpeedloaders)
                VALID_SPEEDLOADERS.remove(item);
            if (!VALID_SPEEDLOADERS.containsKey(item))
                VALID_SPEEDLOADERS.put(item, new LinkedList<>());
            List<AmmoPredicate> list = VALID_SPEEDLOADERS.get(item);
            JsonArray ammoArr = GsonHelper.getAsJsonArray(obj, "valid_speedloaders");
            for (JsonElement el : ammoArr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected string value for speedloader predicate");
                list.add(AmmoPredicate.fromString(el.getAsString()));
            }
        }
    }

    private static void resetAmmoLoaders() {
        VALID_AMMO.clear();
        VALID_AMMO.putAll(DEFAULT_VALID_AMMO);
        VALID_SPEEDLOADERS.clear();
        VALID_SPEEDLOADERS.putAll(DEFAULT_VALID_SPEEDLOADERS);
    }

    public static void registerDefaults(Item item, List<AmmoPredicate> ammoPredicates, List<AmmoPredicate> speedloaderPredicates) {
        DEFAULT_VALID_AMMO.put(item, ammoPredicates);
        DEFAULT_VALID_SPEEDLOADERS.put(item, speedloaderPredicates);
    }

    @Nullable public static List<AmmoPredicate> getValidAmmoPredicates(Item item) { return VALID_AMMO.get(item); }

    @Nullable public static List<AmmoPredicate> getValidSpeedloaderPredicates(Item item) { return VALID_SPEEDLOADERS.get(item); }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncMagazinePropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncMagazinePropertiesPacket());
    }

    public record ClientboundSyncMagazinePropertiesPacket(Map<Item, List<AmmoPredicate>> ammoPredicates, Map<Item,
            List<AmmoPredicate>> speedloaderPredicates) implements RFEPacket {
        public ClientboundSyncMagazinePropertiesPacket() { this(VALID_AMMO, VALID_SPEEDLOADERS); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(VALID_AMMO.size());
            for (Map.Entry<Item, List<AmmoPredicate>> entry : VALID_AMMO.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                List<AmmoPredicate> list = entry.getValue();
                buf.writeVarInt(list.size());
                for (AmmoPredicate pred : list)
                    AmmoPredicate.writeToNetwork(pred, buf);
            }
            buf.writeVarInt(VALID_SPEEDLOADERS.size());
            for (Map.Entry<Item, List<AmmoPredicate>> entry : VALID_SPEEDLOADERS.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                List<AmmoPredicate> list = entry.getValue();
                buf.writeVarInt(list.size());
                for (AmmoPredicate pred : list)
                    AmmoPredicate.writeToNetwork(pred, buf);
            }
        }

        public static ClientboundSyncMagazinePropertiesPacket decode(FriendlyByteBuf buf) {
            int ammoSz = buf.readVarInt();
            Map<Item, List<AmmoPredicate>> validAmmo = new Reference2ObjectOpenHashMap<>();
            for (int ammoInd = 0; ammoInd < ammoSz; ++ammoInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                List<AmmoPredicate> list = new LinkedList<>();
                for (int predInd = 0; predInd < predSz; ++predInd)
                    list.add(AmmoPredicate.fromNetwork(buf));
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    validAmmo.put(i, list);
                }, () -> LOGGER.warn("Attempted to sync missing item {}", loc));
            }
            int speedloaderSz = buf.readVarInt();
            Map<Item, List<AmmoPredicate>> validSpeedloader = new Reference2ObjectOpenHashMap<>();
            for (int speedloaderInd = 0; speedloaderInd < speedloaderSz; ++speedloaderInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                List<AmmoPredicate> list = new LinkedList<>();
                for (int predInd = 0; predInd < predSz; ++predInd)
                    list.add(AmmoPredicate.fromNetwork(buf));
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    validSpeedloader.put(i, list);
                }, () -> LOGGER.warn("Attempted to sync missing item {}", loc));
            }
            return new ClientboundSyncMagazinePropertiesPacket(validAmmo, validSpeedloader);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            VALID_AMMO.clear();
            VALID_AMMO.putAll(this.ammoPredicates);
            VALID_SPEEDLOADERS.clear();
            VALID_SPEEDLOADERS.putAll(this.speedloaderPredicates);
        }
    }

}
