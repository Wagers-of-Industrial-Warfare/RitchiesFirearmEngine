package rbasamoyai.ritchiesfirearmengine.mixin.ai;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.ICanFireRFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.RangedFirearmAttackGoal;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.TickFirearmInHandsGoal;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEDefaultFirearmItem;

@Mixin(Pillager.class)
public abstract class PillagerMixin extends AbstractIllager implements ICanFireRFEFirearmItem {

    @Unique private RangedFirearmAttackGoal ritchiesfirearmengine$firearmattackgoal;

    PillagerMixin(EntityType<? extends AbstractIllager> entityType, Level level) { super(entityType, level); }

    @WrapMethod(method = "registerGoals")
    private void ritchiesfirearmengine$registerGoals(Operation<Void> original) {
        original.call();
        this.goalSelector.addGoal(0, new TickFirearmInHandsGoal(this));
        this.goalSelector.addGoal(3, this.ritchiesfirearmengine$firearmattackgoal = new RangedFirearmAttackGoal(this, 1.0d, 8f));
    }

    @WrapMethod(method = "getArmPose")
    private IllagerArmPose ritchiesfirearmengine$getArmPose(Operation<IllagerArmPose> original) {
        IllagerArmPose ret = original.call();
        if (this.getMainHandItem().getItem() instanceof RFEDefaultFirearmItem)
            return IllagerArmPose.CROSSBOW_HOLD; // TODO other poses
        return ret;
    }

    @Override
    public void ritchiesfirearmengine$onShotFired(ItemStack itemStack) {
        if (this.ritchiesfirearmengine$firearmattackgoal != null)
            this.ritchiesfirearmengine$firearmattackgoal.decrementShot();
    }

}
