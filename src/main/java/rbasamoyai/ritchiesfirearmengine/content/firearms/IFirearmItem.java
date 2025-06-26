package rbasamoyai.ritchiesfirearmengine.content.firearms;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.content.SimultaneousUseAndAttack;

public interface IFirearmItem extends SimultaneousUseAndAttack, HoldAttackKeyInteraction, FovModifyingItem {

    boolean isAiming(ItemStack itemStack, LivingEntity entity);

}
