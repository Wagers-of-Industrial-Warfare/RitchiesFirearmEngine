package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.CompareValueSource;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

public class RFEContentBuilderRegistry {

    private static final Object2ObjectOpenHashMap<ResourceLocation, RFEItemBuilder> ITEM_BUILDERS = new Object2ObjectOpenHashMap<>();

    public static void registerItemBuilder(ResourceLocation typeLoc, RFEItemBuilder builder) {
        if (ITEM_BUILDERS.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE item builder with id '" + typeLoc + "'");
        ITEM_BUILDERS.put(typeLoc, builder);
    }

    @ApiStatus.Internal
    public static Item buildItem(ResourceLocation type, JsonObject obj) {
        if (!ITEM_BUILDERS.containsKey(type))
            throw new IllegalStateException("RFE item builder of type '" + type + "' not present");
        return ITEM_BUILDERS.get(type).apply(obj);
    }

    private static final Object2ObjectOpenHashMap<ResourceLocation, CompareValueSource> COMPARE_VALUE_SOURCES = new Object2ObjectOpenHashMap<>();

    public static void registerCompareValueSource(ResourceLocation typeLoc, CompareValueSource source) {
        if (COMPARE_VALUE_SOURCES.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE compare value source with id '" + typeLoc + "'");
        COMPARE_VALUE_SOURCES.put(typeLoc, source);
    }

    public static CompareValueSource getCompareValueSource(ResourceLocation type) {
        if (!COMPARE_VALUE_SOURCES.containsKey(type))
            throw new IllegalStateException("RFE compare value source of type '" + type + "' not present");
        return COMPARE_VALUE_SOURCES.get(type);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFEProjectileType.Serializer<?>> PROJECTILE_TYPE_SERIALIZERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFEProjectileType.Serializer<?>, ResourceLocation> PROJECTILE_TYPE_SERIALIZERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerProjectileTypeSerializer(ResourceLocation id, RFEProjectileType.Serializer<?> serializer) {
        if (PROJECTILE_TYPE_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE projectile type serializer with id '" + id + "'");
        PROJECTILE_TYPE_SERIALIZERS.put(id, serializer);
        PROJECTILE_TYPE_SERIALIZERS_IDS.put(serializer, id);
    }

    public static RFEProjectileType.Serializer<?> getProjectileTypeSerializer(ResourceLocation id) {
        if (!PROJECTILE_TYPE_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("RFE projectile type serializer of type '" + id + "' not present");
        return PROJECTILE_TYPE_SERIALIZERS.get(id);
    }

    public static ResourceLocation getProjectileTypeSerializerId(RFEProjectileType.Serializer<?> ser) {
        if (!PROJECTILE_TYPE_SERIALIZERS_IDS.containsKey(ser))
            throw new IllegalStateException("Unknown RFE projectile type serializer " + ser);
        return PROJECTILE_TYPE_SERIALIZERS_IDS.get(ser);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFESpreadProvider.Serializer<?>> SPREAD_PROVIDER_SERIALIZERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFESpreadProvider.Serializer<?>, ResourceLocation> SPREAD_PROVIDER_SERIALIZERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerSpreadProviderSerializer(ResourceLocation id, RFESpreadProvider.Serializer<?> ser) {
        if (SPREAD_PROVIDER_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE spread provider serializer wtih id '" + id + "'");
        SPREAD_PROVIDER_SERIALIZERS.put(id, ser);
        SPREAD_PROVIDER_SERIALIZERS_IDS.put(ser, id);
    }

    public static RFESpreadProvider.Serializer<?> getSpreadProviderSerializer(ResourceLocation id) {
        if (!SPREAD_PROVIDER_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("RFE spread provider serializer of type '" + id + "' not present");
        return SPREAD_PROVIDER_SERIALIZERS.get(id);
    }

    public static ResourceLocation getSpreadProviderSerializerId(RFESpreadProvider.Serializer<?> ser) {
        if (!SPREAD_PROVIDER_SERIALIZERS_IDS.containsKey(ser))
            throw new IllegalStateException("Unknown RFE spread provider serializer " + ser);
        return SPREAD_PROVIDER_SERIALIZERS_IDS.get(ser);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFERecoilProvider.Serializer<?>> RECOIL_PROVIDER_SERIALIZERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFERecoilProvider.Serializer<?>, ResourceLocation> RECOIL_PROVIDER_SERIALIZERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerRecoilProviderSerializer(ResourceLocation id, RFERecoilProvider.Serializer<?> ser) {
        if (RECOIL_PROVIDER_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE recoil provider serializer wtih id '" + id + "'");
        RECOIL_PROVIDER_SERIALIZERS.put(id, ser);
        RECOIL_PROVIDER_SERIALIZERS_IDS.put(ser, id);
    }

    public static RFERecoilProvider.Serializer<?> getRecoilProviderSerializer(ResourceLocation id) {
        if (!RECOIL_PROVIDER_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("RFE recoil provider serializer of type '" + id + "' not present");
        return RECOIL_PROVIDER_SERIALIZERS.get(id);
    }

    public static ResourceLocation getRecoilProviderSerializerId(RFERecoilProvider.Serializer<?> ser) {
        if (!RECOIL_PROVIDER_SERIALIZERS_IDS.containsKey(ser))
            throw new IllegalStateException("Unknown RFE recoil provider serializer " + ser);
        return RECOIL_PROVIDER_SERIALIZERS_IDS.get(ser);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFEHitMultiplier.Provider> HIT_MULTIPLIER_PROVIDERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFEHitMultiplier.Provider, ResourceLocation> HIT_MULTIPLIER_PROVIDERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerHitMultiplierProvider(ResourceLocation id, RFEHitMultiplier.Provider prov) {
        if (HIT_MULTIPLIER_PROVIDERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE hit multiplier provider with id '" + id + "'");
        HIT_MULTIPLIER_PROVIDERS.put(id, prov);
        HIT_MULTIPLIER_PROVIDERS_IDS.put(prov, id);
    }

    public static RFEHitMultiplier.Provider getHitMultiplierProvider(ResourceLocation id) {
        if (!HIT_MULTIPLIER_PROVIDERS.containsKey(id))
            throw new IllegalStateException("RFE hit multiplier provider of type '" + id + "' not present");
        return HIT_MULTIPLIER_PROVIDERS.get(id);
    }

    public static ResourceLocation getHitMultiplierProviderId(RFEHitMultiplier.Provider prov) {
        if (!HIT_MULTIPLIER_PROVIDERS_IDS.containsKey(prov))
            throw new IllegalStateException("Unknown RFE hit multiplier provider " + prov);
        return HIT_MULTIPLIER_PROVIDERS_IDS.get(prov);
    }
    
    private static final Object2ReferenceMap<ResourceLocation, RFEMisfire.Provider> MISFIRE_PROVIDERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFEMisfire.Provider, ResourceLocation> MISFIRE_PROVIDERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerMisfireProvider(ResourceLocation id, RFEMisfire.Provider prov) {
        if (MISFIRE_PROVIDERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE misfire provider with id '" + id + "'");
        MISFIRE_PROVIDERS.put(id, prov);
        MISFIRE_PROVIDERS_IDS.put(prov, id);
    }

    public static RFEMisfire.Provider getMisfireProvider(ResourceLocation id) {
        if (!MISFIRE_PROVIDERS.containsKey(id))
            throw new IllegalStateException("RFE misfire provider of type '" + id + "' not present");
        return MISFIRE_PROVIDERS.get(id);
    }

    public static ResourceLocation getMisfireProviderId(RFEMisfire.Provider prov) {
        if (!MISFIRE_PROVIDERS_IDS.containsKey(prov))
            throw new IllegalStateException("Unknown RFE misfire provider " + prov);
        return MISFIRE_PROVIDERS_IDS.get(prov);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFEItemAttachmentProperties.Serializer<?>> ITEM_ATTACHMENT_SERIALIZERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFEItemAttachmentProperties.Serializer<?>, ResourceLocation> ITEM_ATTACHMENT_SERIALIZERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerItemAttachmentSerializer(ResourceLocation id, RFEItemAttachmentProperties.Serializer<?> prov) {
        if (ITEM_ATTACHMENT_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE item attachment properties serializer with id '" + id + "'");
        ITEM_ATTACHMENT_SERIALIZERS.put(id, prov);
        ITEM_ATTACHMENT_SERIALIZERS_IDS.put(prov, id);
    }

    public static RFEItemAttachmentProperties.Serializer<?> getItemAttachmentSerializer(ResourceLocation id) {
        if (!ITEM_ATTACHMENT_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("RFE item attachment properties serializer of type '" + id + "' not present");
        return ITEM_ATTACHMENT_SERIALIZERS.get(id);
    }

    public static ResourceLocation getItemAttachmentSerializerId(RFEItemAttachmentProperties.Serializer<?> prov) {
        if (!ITEM_ATTACHMENT_SERIALIZERS_IDS.containsKey(prov))
            throw new IllegalStateException("Unknown RFE item attachment properties serializer " + prov);
        return ITEM_ATTACHMENT_SERIALIZERS_IDS.get(prov);
    }

}