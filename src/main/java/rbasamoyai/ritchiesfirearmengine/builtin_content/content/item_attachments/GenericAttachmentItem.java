package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;

public class GenericAttachmentItem extends Item {

    public GenericAttachmentItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject jsonObject) {
            return new GenericAttachmentItem(new Item.Properties());
        }
    }

}
