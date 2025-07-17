package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config;

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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFirearmModeHandlingProperties;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executor;

public class RFEFirearmHandlingPropertiesHandler {

    private static final Map<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> HANDLING_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_handling"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    JsonElement el = entry.getValue();
                    if (!el.isJsonObject())
                        throw new JsonParseException("Expected JSON object when parsing firearm item ammo properties");
                    JsonObject obj = el.getAsJsonObject();
                    HANDLING_PROPERTIES.put(item, readHandlingProperties(obj, id));
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm handling properties for item {}: {}", id, e);
                }
            }
        }
    }

    private static void clear() {
        HANDLING_PROPERTIES.clear();
    }

    private static ImmutableMap<String, RFEFirearmModeHandlingProperties> readHandlingProperties(JsonObject obj, ResourceLocation loc) {
        ImmutableMap.Builder<String, RFEFirearmModeHandlingProperties> builder = ImmutableMap.builder();
        JsonObject modes = GsonHelper.getAsJsonObject(obj, "modes");
        for (Map.Entry<String, JsonElement> entry : modes.entrySet()) {
            String mode = entry.getKey();
            JsonElement el = entry.getValue();
            if (!el.isJsonObject())
                throw new JsonParseException("Expected JSON object while parsing handling properties for mode " + mode + " for item " + loc);
            JsonObject modeObj = el.getAsJsonObject();
            float jamChance = GsonHelper.getAsFloat(modeObj, "jam_chance", 0);
            boolean manualCharging = GsonHelper.getAsBoolean(modeObj, "manual_charge", false);
            float heatCapacity = GsonHelper.getAsFloat(modeObj, "heat_capacity", 0);
            float heatRemovedPerTick = GsonHelper.getAsFloat(modeObj, "heat_removed_per_tick", 0);
            float heatRemovedOnCharge = GsonHelper.getAsFloat(modeObj, "heat_removed_on_charge", 0);
            float heatAddedOnFiring = GsonHelper.getAsFloat(modeObj, "heat_added_on_firing", 0);
            int coolingDelayTime = GsonHelper.getAsInt(modeObj, "cooling_delay_time", 0);
            builder.put(mode, new RFEFirearmModeHandlingProperties.Builder()
                    .jamChance(jamChance)
                    .manualCharging(manualCharging)
                    .heatCapacity(heatCapacity)
                    .heatRemovedPerTick(heatRemovedPerTick)
                    .heatRemovedOnCharge(heatRemovedOnCharge)
                    .heatAddedOnFiring(heatAddedOnFiring)
                    .coolingDelay(coolingDelayTime)
                    .build());
        }
        return builder.build();
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

    public record ClientboundSyncFirearmHandlingPropertiesPacket(Map<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> properties) implements RFEPacket {
        ClientboundSyncFirearmHandlingPropertiesPacket() { this(new Reference2ObjectOpenHashMap<>(HANDLING_PROPERTIES)); }

        public static ClientboundSyncFirearmHandlingPropertiesPacket decode(FriendlyByteBuf buf) {
            int dataSz = buf.readVarInt();
            Map<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> handlingProperties = new Reference2ObjectOpenHashMap<>();
            for (int dataInd = 0; dataInd < dataSz; ++dataInd) {
                ImmutableMap.Builder<String, RFEFirearmModeHandlingProperties> propertiesByMode = ImmutableMap.builder();
                ResourceLocation loc = buf.readResourceLocation();
                int modeSz = buf.readVarInt();
                for (int modeInd = 0; modeInd < modeSz; ++modeInd)
                    propertiesByMode.put(buf.readUtf(), RFEFirearmModeHandlingProperties.fromNetwork(buf));
                BuiltInRegistries.ITEM.getOptional(loc).ifPresent(item -> handlingProperties.put(item, propertiesByMode.build()));
            }
            return new ClientboundSyncFirearmHandlingPropertiesPacket(handlingProperties);
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.properties.size());
            for (Map.Entry<Item, ImmutableMap<String, RFEFirearmModeHandlingProperties>> itemEntry : this.properties.entrySet()) {
                ImmutableMap<String, RFEFirearmModeHandlingProperties> propertiesByMode = itemEntry.getValue();
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(itemEntry.getKey()))
                        .writeVarInt(propertiesByMode.size());
                for (Map.Entry<String, RFEFirearmModeHandlingProperties> modeEntry : propertiesByMode.entrySet()) {
                    buf.writeUtf(modeEntry.getKey());
                    RFEFirearmModeHandlingProperties.toNetwork(buf, modeEntry.getValue());
                }
            }
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            HANDLING_PROPERTIES.clear();
            HANDLING_PROPERTIES.putAll(this.properties);
        }
    }

}
