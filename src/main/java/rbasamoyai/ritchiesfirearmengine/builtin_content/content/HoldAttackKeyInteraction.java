package rbasamoyai.ritchiesfirearmengine.builtin_content.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface HoldAttackKeyInteraction {

    boolean isHoldingAttackKey(ItemStack itemStack, LivingEntity entity);
    boolean onPressAttackKey(ItemStack itemStack, LivingEntity entity);
    void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity);

}
