package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo;

import com.google.common.collect.ImmutableList;
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

public class MagazineItemPropertiesHandler {

    private static final Map<Item, ImmutableList<AmmoPredicate>> VALID_AMMO = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, ImmutableList<AmmoPredicate>> DEFAULT_VALID_AMMO = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, ImmutableList<AmmoPredicate>> VALID_SPEEDLOADERS = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, ImmutableList<AmmoPredicate>> DEFAULT_VALID_SPEEDLOADERS = new Reference2ObjectOpenHashMap<>();

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
                        throw new JsonParseException("Expected JSON object when parsing magazine item data");
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
                VALID_AMMO.put(item, ImmutableList.of());
            ImmutableList<AmmoPredicate> list = VALID_AMMO.get(item);
            ImmutableList.Builder<AmmoPredicate> newList = ImmutableList.builder();
            newList.addAll(list);
            JsonArray ammoArr = GsonHelper.getAsJsonArray(obj, "valid_ammo");
            for (JsonElement el : ammoArr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected string value for ammo predicate");
                newList.add(AmmoPredicate.fromString(el.getAsString()));
            }
            VALID_AMMO.put(item, newList.build());
        }
        if (GsonHelper.isArrayNode(obj, "valid_speedloaders")) {
            boolean replaceSpeedloaders = GsonHelper.getAsBoolean(obj, "replace_valid_speedloaders", false);
            if (replaceSpeedloaders)
                VALID_SPEEDLOADERS.remove(item);
            if (!VALID_SPEEDLOADERS.containsKey(item))
                VALID_SPEEDLOADERS.put(item, ImmutableList.of());
            ImmutableList<AmmoPredicate> list = VALID_SPEEDLOADERS.get(item);
            ImmutableList.Builder<AmmoPredicate> newList = ImmutableList.builder();
            newList.addAll(list);
            JsonArray speedloaderArr = GsonHelper.getAsJsonArray(obj, "valid_speedloaders");
            for (JsonElement el : speedloaderArr) {
                if (!GsonHelper.isStringValue(el))
                    throw new JsonParseException("Expected string value for speedloader predicate");
                newList.add(AmmoPredicate.fromString(el.getAsString()));
            }
            VALID_SPEEDLOADERS.put(item, newList.build());
        }
    }

    private static void resetAmmoLoaders() {
        VALID_AMMO.clear();
        VALID_AMMO.putAll(DEFAULT_VALID_AMMO);
        VALID_SPEEDLOADERS.clear();
        VALID_SPEEDLOADERS.putAll(DEFAULT_VALID_SPEEDLOADERS);
    }

    public static void registerDefaults(Item item, ImmutableList<AmmoPredicate> ammoPredicates, ImmutableList<AmmoPredicate> speedloaderPredicates) {
        if (DEFAULT_VALID_AMMO.containsKey(item) || DEFAULT_VALID_SPEEDLOADERS.containsKey(item))
            throw new IllegalStateException("Already registered default magazine properties for item");
        DEFAULT_VALID_AMMO.put(item, ammoPredicates);
        DEFAULT_VALID_SPEEDLOADERS.put(item, speedloaderPredicates);
    }

    @Nullable public static ImmutableList<AmmoPredicate> getValidAmmoPredicates(Item item) { return VALID_AMMO.get(item); }

    @Nullable public static ImmutableList<AmmoPredicate> getValidSpeedloaderPredicates(Item item) { return VALID_SPEEDLOADERS.get(item); }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncMagazinePropertiesPacket(), player);
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncMagazinePropertiesPacket());
    }

    public record ClientboundSyncMagazinePropertiesPacket(Map<Item, ImmutableList<AmmoPredicate>> ammoPredicates,
                                                          Map<Item, ImmutableList<AmmoPredicate>> speedloaderPredicates) implements RFEPacket {
        ClientboundSyncMagazinePropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(VALID_AMMO), new Reference2ObjectOpenHashMap<>(VALID_SPEEDLOADERS)); }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.ammoPredicates.size());
            for (Map.Entry<Item, ImmutableList<AmmoPredicate>> entry : this.ammoPredicates.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                ImmutableList<AmmoPredicate> list = entry.getValue();
                buf.writeVarInt(list.size());
                for (AmmoPredicate pred : list)
                    AmmoPredicate.writeToNetwork(pred, buf);
            }
            buf.writeVarInt(this.speedloaderPredicates.size());
            for (Map.Entry<Item, ImmutableList<AmmoPredicate>> entry : this.speedloaderPredicates.entrySet()) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                ImmutableList<AmmoPredicate> list = entry.getValue();
                buf.writeVarInt(list.size());
                for (AmmoPredicate pred : list)
                    AmmoPredicate.writeToNetwork(pred, buf);
            }
        }

        public static ClientboundSyncMagazinePropertiesPacket decode(FriendlyByteBuf buf) {
            int ammoSz = buf.readVarInt();
            Map<Item, ImmutableList<AmmoPredicate>> validAmmo = new Reference2ObjectOpenHashMap<>();
            for (int ammoInd = 0; ammoInd < ammoSz; ++ammoInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                ImmutableList.Builder<AmmoPredicate> list = ImmutableList.builder();
                for (int predInd = 0; predInd < predSz; ++predInd)
                    list.add(AmmoPredicate.fromNetwork(buf));
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    validAmmo.put(i, list.build());
                }, () -> LOGGER.warn("Attempted to sync missing item {}", loc));
            }
            int speedloaderSz = buf.readVarInt();
            Map<Item, ImmutableList<AmmoPredicate>> validSpeedloader = new Reference2ObjectOpenHashMap<>();
            for (int speedloaderInd = 0; speedloaderInd < speedloaderSz; ++speedloaderInd) {
                ResourceLocation loc = buf.readResourceLocation();
                int predSz = buf.readVarInt();
                ImmutableList.Builder<AmmoPredicate> list = ImmutableList.builder();
                for (int predInd = 0; predInd < predSz; ++predInd)
                    list.add(AmmoPredicate.fromNetwork(buf));
                BuiltInRegistries.ITEM.getOptional(loc).ifPresentOrElse(i -> {
                    validSpeedloader.put(i, list.build());
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
