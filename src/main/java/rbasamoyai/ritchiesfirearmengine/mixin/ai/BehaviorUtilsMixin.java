package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior.RangedFirearmAttackBehavior;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

@Mixin(BehaviorUtils.class)
public class BehaviorUtilsMixin {

    @WrapMethod(method = "isWithinAttackRange")
    private static boolean ritchiesfirearmengine$isWithinAttackRange(Mob mob, LivingEntity target, int cooldown, Operation<Boolean> original) {
        // TODO dual wielding
        if (RangedFirearmAttackBehavior.isHoldingShootableFirearm(mob)) {
            if (mob.getMainHandItem().getItem() instanceof RFEFirearmItem firearmItem)
                return mob.closerThan(target, firearmItem.getAIShootingRange() - cooldown);
        }
        return original.call(mob, target, cooldown);
    }

}
