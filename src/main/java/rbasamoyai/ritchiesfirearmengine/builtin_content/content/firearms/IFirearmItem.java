package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.SimultaneousUseAndAttack;

public interface IFirearmItem extends SimultaneousUseAndAttack, HoldAttackKeyInteraction, FovModifyingItem {

    boolean isAiming(ItemStack itemStack, LivingEntity entity);

}
