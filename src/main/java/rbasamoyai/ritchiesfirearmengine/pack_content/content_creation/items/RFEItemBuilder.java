package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.items;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;

import java.util.function.Function;

@FunctionalInterface
public interface RFEItemBuilder extends Function<JsonObject, Item> {
}
