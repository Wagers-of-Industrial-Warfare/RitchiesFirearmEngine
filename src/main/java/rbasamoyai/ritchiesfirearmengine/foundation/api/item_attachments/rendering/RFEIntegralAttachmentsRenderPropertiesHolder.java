package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record RFEIntegralAttachmentsRenderPropertiesHolder(Map<ResourceLocation, RFEItemAttachmentRenderProperties> renderDataBySlot) {
}
