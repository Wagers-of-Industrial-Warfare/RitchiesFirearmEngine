package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

public interface IHasRFEItemAttachments {

    Map<ResourceLocation, ItemStack> getAttachments(ItemStack stack);
    Set<ResourceLocation> getAttachmentSlots(ItemStack stack);
    ImmutableMultimap<ResourceLocation, ResourceLocation> getMutuallyExclusiveSlots(ItemStack stack);

    void setAttachment(ItemStack itemStack, ResourceLocation slot, ItemStack attachmentItem);

    default ItemStack getAttachmentInSlot(ItemStack itemStack, ResourceLocation slot) {
        return this.getAttachments(itemStack).getOrDefault(slot, ItemStack.EMPTY);
    }

}
