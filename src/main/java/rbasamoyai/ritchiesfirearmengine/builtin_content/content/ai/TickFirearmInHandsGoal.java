package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhaseAccessFilter;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;

public class TickFirearmInHandsGoal extends Goal {

    private final Mob mob;
    private int inCombatFor = 0;
    // TODO dual wielding
    private RFEFirearmItem.Action firearmAction = null;

    public TickFirearmInHandsGoal(Mob mob) {
        this.mob = mob;
    }

    protected boolean isHoldingFirearm() {
        // TODO dual wielding
        //return this.mob.isHolding(s -> s.getItem() instanceof RFEFirearmItem);
        return this.mob.getMainHandItem().getItem() instanceof RFEFirearmItem;
    }

    @Override public boolean canUse() { return this.isHoldingFirearm(); }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean isInterruptable() { return false; }

    @Override
    public void tick() {
        super.tick();

        if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) {
            this.inCombatFor = 60;
        } else if (this.inCombatFor > 0) {
            --this.inCombatFor;
        }

        // TODO dual wielding
        ItemStack mainhandItem = this.mob.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            mainhandItem.inventoryTick(this.mob.level(), this.mob, 0, true);
            RFEFirearmItem.Action currentAction = firearmItem.getCurrentAction(mainhandItem);
            RFEFirearmMode currentMode = firearmItem.getCurrentMode(mainhandItem);
            if (this.inCombatFor <= 0) {
                if (this.firearmAction == null && currentAction == null) {
                    if (currentMode.tryRunningReloadAction(mainhandItem, this.mob, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE)) {
                        currentAction = RFEFirearmItem.Action.RELOAD;
                    } else {
                        inCombatFor = 40;
                    }
                }
            }
            this.firearmAction = currentAction;
        }
        float dAimPitch = 0;
        float dAimYaw = 0;
        RFERecoilInstance mainhandRecoilInstance = RFERecoilManager.getRecoilInstance(this.mob, mainhandItem);
        if (mainhandRecoilInstance != null) {
            RFEAimAngles aimRecoil = mainhandRecoilInstance.getAimRecoil(1);
            dAimPitch += aimRecoil.pitch();
            dAimYaw += aimRecoil.yaw();
        }
        float recoilScale = 1f;
        this.mob.setXRot(this.mob.getXRot() - recoilScale * dAimPitch);
        this.mob.setXRot(Mth.clamp(this.mob.getXRot(), -90.0F, 90.0F));
        this.mob.setYHeadRot(this.mob.getYHeadRot() + recoilScale * dAimYaw);
    }

}
