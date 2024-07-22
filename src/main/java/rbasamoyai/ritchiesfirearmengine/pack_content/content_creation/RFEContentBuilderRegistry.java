package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items.RFEItemBuilder;

public class RFEContentBuilderRegistry {

    private static final Object2ObjectOpenHashMap<ResourceLocation, RFEItemBuilder> ITEM_BUILDERS = new Object2ObjectOpenHashMap<>();

    /**
     * Internal use only. For adding more item builders, see {@link rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin}
     * and its related documentation.
     */
    public static void registerItemBuilder(ResourceLocation typeLoc, RFEItemBuilder builder) {
        if (ITEM_BUILDERS.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE item builder with id '" + typeLoc + "'");
        ITEM_BUILDERS.put(typeLoc, builder);
    }

    public static Item buildItem(ResourceLocation type, JsonObject obj) {
        if (!ITEM_BUILDERS.containsKey(type))
            throw new IllegalStateException("RFE item builder of type '" + type + "' not present");
        return ITEM_BUILDERS.get(type).apply(obj);
    }

}
