package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IHasRFEItemAttachments {

    Map<ResourceLocation, ItemStack> getAttachments(ItemStack stack);

}
