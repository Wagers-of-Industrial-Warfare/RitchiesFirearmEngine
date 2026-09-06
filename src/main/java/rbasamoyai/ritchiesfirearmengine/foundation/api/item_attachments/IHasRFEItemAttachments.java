package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public interface IHasRFEItemAttachments {

    Map<ResourceLocation, ItemStack> getAttachments(ItemStack stack);
    Map<ResourceLocation, DataComponentPatch> getIntegralAttachmentData(ItemStack stack);

    Set<ResourceLocation> getAttachmentSlots(ItemStack stack);
    Set<ResourceLocation> getIntegralAttachmentSlots(ItemStack stack);
    ImmutableMultimap<ResourceLocation, ResourceLocation> getMutuallyExclusiveSlots(ItemStack stack);

    void setAttachment(ItemStack itemStack, ResourceLocation slot, ItemStack attachmentItem);
    void setIntegralAttachmentData(ItemStack itemStack, ResourceLocation slot, DataComponentPatch data);

    default ItemStack getAttachmentInSlot(ItemStack itemStack, ResourceLocation slot) {
        return this.getAttachments(itemStack).getOrDefault(slot, ItemStack.EMPTY);
    }

    default DataComponentPatch getIntegralAttachmentDataInSlot(ItemStack itemStack, ResourceLocation slot) {
        return this.getIntegralAttachmentData(itemStack).getOrDefault(slot, DataComponentPatch.EMPTY);
    }

    default Set<ResourceLocation> getBlockingSlots(ItemStack parentStack, ResourceLocation slotId) {
        ImmutableMultimap<ResourceLocation, ResourceLocation> mutuallyExclusiveSlots = this.getMutuallyExclusiveSlots(parentStack);
        Map<ResourceLocation, ItemStack> attachmentStacks = this.getAttachments(parentStack);
        Set<ResourceLocation> integralSlots = this.getIntegralAttachmentSlots(parentStack);
        Map<ResourceLocation, DataComponentPatch> integralAttachments = this.getIntegralAttachmentData(parentStack);
        Set<ResourceLocation> blockingSlots = new LinkedHashSet<>();
        for (ResourceLocation slot : mutuallyExclusiveSlots.get(slotId)) {
            if (!attachmentStacks.getOrDefault(slot, ItemStack.EMPTY).isEmpty())
                blockingSlots.add(slot);
            if (integralSlots.contains(slot) && !FirearmDataUtils.isAttachmentRemoved(integralAttachments.getOrDefault(slot, DataComponentPatch.EMPTY)))
                blockingSlots.add(slot);
        }
        return blockingSlots;
    }

}
