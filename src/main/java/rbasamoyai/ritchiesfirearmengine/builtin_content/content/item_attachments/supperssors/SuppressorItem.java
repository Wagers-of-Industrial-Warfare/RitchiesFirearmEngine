package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.supperssors;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;

public class SuppressorItem extends Item {

    public SuppressorItem(Properties properties) {
        super(properties);
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject jsonObject) {
            return new SuppressorItem(new Item.Properties().stacksTo(1));
        }
    }

}
