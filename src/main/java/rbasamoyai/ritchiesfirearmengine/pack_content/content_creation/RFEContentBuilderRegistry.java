package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.content.firearms.logic.CompareValueSource;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items.RFEItemBuilder;

public class RFEContentBuilderRegistry {

    private static final Object2ObjectOpenHashMap<ResourceLocation, RFEItemBuilder> ITEM_BUILDERS = new Object2ObjectOpenHashMap<>();

    public static void registerItemBuilder(ResourceLocation typeLoc, RFEItemBuilder builder) {
        if (ITEM_BUILDERS.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE item builder with id '" + typeLoc + "'");
        ITEM_BUILDERS.put(typeLoc, builder);
    }

    /**
     * Internal use only.
     */
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

    /**
     * Internal use only.
     */
    public static CompareValueSource getCompareValueSource(ResourceLocation type) {
        if (!COMPARE_VALUE_SOURCES.containsKey(type))
            throw new IllegalStateException("RFE compare value source of type '" + type + "' not present");
        return COMPARE_VALUE_SOURCES.get(type);
    }

}
