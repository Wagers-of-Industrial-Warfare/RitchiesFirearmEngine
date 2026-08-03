package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

public record RFEItemAttachmentsPropertiesHolder(Map<Item, Map<ResourceLocation, RFEItemAttachmentProperties>> attachmentPropertiesByItemAndSlot) {

    @Nullable
    public RFEItemAttachmentProperties getAttachmentProperties(Item item, ResourceLocation slot) {
        if (!this.attachmentPropertiesByItemAndSlot.containsKey(item))
            return null;
        return this.attachmentPropertiesByItemAndSlot.get(item).get(slot);
    }

    @Nullable
    public RFEItemAttachmentProperties getAttachmentProperties(ItemStack itemStack, ResourceLocation slot) {
        return this.getAttachmentProperties(itemStack.getItem(), slot);
    }

}
