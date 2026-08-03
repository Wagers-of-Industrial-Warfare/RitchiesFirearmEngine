package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;

public record RFEItemAttachmentsRenderPropertiesHolder(Map<Item, Map<ResourceLocation, RFEItemAttachmentRenderProperties>> renderDataByItemAndSlot) {
}
