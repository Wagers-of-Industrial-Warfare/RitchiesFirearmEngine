package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.CompareValueSource;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.projectiles.RFEProjectileType;

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
            throw new IllegalStateException("Unknown projectile type serializer " + ser);
        return PROJECTILE_TYPE_SERIALIZERS_IDS.get(ser);
    }

}
