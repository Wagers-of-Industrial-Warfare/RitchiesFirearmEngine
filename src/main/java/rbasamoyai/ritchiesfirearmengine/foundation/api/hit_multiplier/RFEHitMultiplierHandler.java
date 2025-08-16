package rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEHitMultiplierHandler {

    private static final Multimap<RFEProjectileType, RFEHitMultiplier> HIT_MULTIPLIERS = LinkedHashMultimap.create();
    private static final Multimap<ResourceLocation, RFEHitMultiplier> UNRESOLVED_HIT_MULTIPLIERS = LinkedHashMultimap.create();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/hit_multipliers"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            HIT_MULTIPLIERS.clear();
            UNRESOLVED_HIT_MULTIPLIERS.clear();

            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object when parsing hit multiplier data");
                    loadData(id, el.getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.warn("Error occurred loading hit multiplier data for projectile type {}: {}", id, e);
                }
            }
        }
    }

    private static void loadData(ResourceLocation id, JsonObject obj) {
        if (GsonHelper.getAsBoolean(obj, "replace", false))
            UNRESOLVED_HIT_MULTIPLIERS.removeAll(id);
        JsonObject multipliers = GsonHelper.getAsJsonObject(obj, "hit_multipliers");
        for (Map.Entry<String, JsonElement> entry : multipliers.entrySet())
            UNRESOLVED_HIT_MULTIPLIERS.put(id, RFEContentBuilderRegistry.getHitMultiplierProvider(RFEUtils.location(entry.getKey()))
                    .apply(entry.getValue().getAsFloat()));
    }

    public static void loadProjectileTypes() {
        HIT_MULTIPLIERS.clear();
        for (Map.Entry<ResourceLocation, RFEHitMultiplier> entry : UNRESOLVED_HIT_MULTIPLIERS.entries())
            HIT_MULTIPLIERS.put(RFEProjectileTypeHandler.getProjectileType(entry.getKey()), entry.getValue());
        UNRESOLVED_HIT_MULTIPLIERS.clear();
    }

    public static Collection<RFEHitMultiplier> getHitMultipliers(RFEProjectileType type) { return HIT_MULTIPLIERS.get(type); }

    public static void syncToAll() {
        RFENetwork.sendToAll(new ClientboundSyncHitMultipliersPacket());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(new ClientboundSyncHitMultipliersPacket(), player);
    }

    public record ClientboundSyncHitMultipliersPacket(Multimap<ResourceLocation, RFEHitMultiplier> hitMultipliers) implements RFEPacket {
        public static ClientboundSyncHitMultipliersPacket decode(FriendlyByteBuf buf) {
            int sz = buf.readVarInt();
            Multimap<ResourceLocation, RFEHitMultiplier> map = LinkedHashMultimap.create();
            for (int i = 0; i < sz; ++i) {
                ResourceLocation typeLoc = buf.readResourceLocation();
                ResourceLocation multiplierLoc = buf.readResourceLocation();
                float mul = buf.readFloat();
                map.put(typeLoc, RFEContentBuilderRegistry.getHitMultiplierProvider(multiplierLoc).apply(mul));
            }
            return new ClientboundSyncHitMultipliersPacket(map);
        }

        ClientboundSyncHitMultipliersPacket() { this(prepareForSyncing()); }

        private static Multimap<ResourceLocation, RFEHitMultiplier> prepareForSyncing() {
            Multimap<ResourceLocation, RFEHitMultiplier> map = LinkedHashMultimap.create();
            for (Map.Entry<RFEProjectileType, RFEHitMultiplier> entry : HIT_MULTIPLIERS.entries()) {
                ResourceLocation loc = RFEProjectileTypeHandler.getProjectileTypeId(entry.getKey());
                if (loc != null)
                    map.put(loc, entry.getValue());
            }
            return map;
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.hitMultipliers.size());
            for (Map.Entry<ResourceLocation, RFEHitMultiplier> entry : this.hitMultipliers.entries()) {
                buf.writeResourceLocation(entry.getKey())
                        .writeResourceLocation(RFEContentBuilderRegistry.getHitMultiplierProviderId(entry.getValue().getProvider()))
                        .writeFloat(entry.getValue().getMultiplier());
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            HIT_MULTIPLIERS.clear();
            UNRESOLVED_HIT_MULTIPLIERS.clear();
            UNRESOLVED_HIT_MULTIPLIERS.putAll(this.hitMultipliers);
            loadProjectileTypes();
        }
    }

}
