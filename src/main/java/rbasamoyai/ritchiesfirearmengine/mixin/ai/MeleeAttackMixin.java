package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import org.spongepowered.asm.mixin.Mixin;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior.RangedFirearmAttackBehavior;

@Mixin(MeleeAttack.class)
public class MeleeAttackMixin {

    @WrapMethod(method = "isHoldingUsableProjectileWeapon")
    private static boolean ritchiesfirearmengine$isHoldingUsableProjectileWeapon(Mob mob, Operation<Boolean> original) {
        if (original.call(mob))
            return true;
        // TODO this is a bit more complex
        return RangedFirearmAttackBehavior.isHoldingShootableFirearm(mob);
    }

}
