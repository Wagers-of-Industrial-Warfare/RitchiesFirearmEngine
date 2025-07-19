package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.SimultaneousUseAndAttack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFiringInput;

import java.util.List;

public interface IFirearmItem extends SimultaneousUseAndAttack, HoldAttackKeyInteraction, FovModifyingItem {

    boolean isAiming(ItemStack itemStack, LivingEntity entity);

    void handleClientFireInputOnServer(ItemStack itemStack, LivingEntity entity, List<RFEFiringInput> firingInputs, boolean jam);

}
