package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.FovModifyingItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.RFEFiringInput;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilClientImpulse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface IFirearmItem extends HoldAttackKeyInteraction, FovModifyingItem {

    boolean isAiming(ItemStack itemStack, LivingEntity entity);

    void handleClientFireInputOnServer(ItemStack itemStack, LivingEntity entity, List<RFEFiringInput> firingInputs, boolean jam,
                                       @Nullable UUID recoilUUID, InteractionHand hand);

    void handleServerAutomaticFireOnClient(ItemStack itemStack, LivingEntity entity, InteractionHand hand, RFERecoilClientImpulse impulse);

}
