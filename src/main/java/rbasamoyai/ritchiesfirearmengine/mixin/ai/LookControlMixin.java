package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;

@Mixin(LookControl.class)
public class LookControlMixin {

    @Shadow @Final protected Mob mob;

    @WrapMethod(method = "resetXRotOnTick")
    private boolean ritchiesfirearmengine$resetXRotOnTick(Operation<Boolean> original) {
        if (RFERecoilManager.getRecoilInstance(this.mob, this.mob.getMainHandItem()) != null ||
                RFERecoilManager.getRecoilInstance(this.mob, this.mob.getOffhandItem()) != null)
            return false;
        return original.call();
    }

}
