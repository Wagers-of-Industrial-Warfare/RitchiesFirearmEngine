package rbasamoyai.ritchiesfirearmengine.builtin_content.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

import javax.annotation.Nullable;

public class RFEItemLengths {

    public static float getItemLength(ItemStack itemStack, @Nullable LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.getItemLength(itemStack, entity);
        return 1;
    }

    private RFEItemLengths() {}

}
