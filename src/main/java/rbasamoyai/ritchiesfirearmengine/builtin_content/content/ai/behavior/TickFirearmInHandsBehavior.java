package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhaseAccessFilter;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilManager;

public class TickFirearmInHandsBehavior extends Behavior<Mob> {

    private int inCombatFor = 0;
    // TODO dual wielding
    private RFEFirearmItem.Action firearmAction = null;

    public TickFirearmInHandsBehavior() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED));
    }

    protected boolean isHoldingFirearm(Mob owner) {
        // TODO dual wielding
        //return this.mob.isHolding(s -> s.getItem() instanceof RFEFirearmItem);
        return owner.getMainHandItem().getItem() instanceof RFEFirearmItem;
    }

    @Override protected boolean checkExtraStartConditions(ServerLevel level, Mob owner) { return this.isHoldingFirearm(owner); }

    @Override protected boolean canStillUse(ServerLevel level, Mob entity, long gameTime) { return this.isHoldingFirearm(entity); }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, Mob owner, long gameTime) {
        super.tick(level, owner, gameTime);

        LivingEntity target = getAttackTarget(owner);
        if (target != null && target.isAlive()) {
            this.inCombatFor = 60;
        } else if (this.inCombatFor > 0) {
            --this.inCombatFor;
        }

        // TODO dual wielding
        ItemStack mainhandItem = owner.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            mainhandItem.inventoryTick(owner.level(), owner, 0, true);
            RFEFirearmItem.Action currentAction = firearmItem.getCurrentAction(mainhandItem);
            RFEFirearmMode currentMode = firearmItem.getCurrentMode(mainhandItem);
            if (this.inCombatFor <= 0) {
                if (this.firearmAction == null && currentAction == null) {
                    if (currentMode.tryRunningReloadAction(mainhandItem, owner, ReloadPhase.PhaseType.PREPARE, true, ReloadPhaseAccessFilter.IncludeAll.INSTANCE)) {
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
        RFERecoilInstance mainhandRecoilInstance = RFERecoilManager.getRecoilInstance(owner, mainhandItem);
        if (mainhandRecoilInstance != null) {
            RFEAimAngles aimRecoil = mainhandRecoilInstance.getAimRecoil(1);
            dAimPitch += aimRecoil.pitch();
            dAimYaw += aimRecoil.yaw();
        }
        float recoilScale = 1f;
        owner.setXRot(owner.getXRot() - recoilScale * dAimPitch);
        owner.setXRot(Mth.clamp(owner.getXRot(), -90.0F, 90.0F));
        owner.setYHeadRot(owner.getYHeadRot() + recoilScale * dAimYaw);
    }

    private static LivingEntity getAttackTarget(LivingEntity shooter) {
        return shooter.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }
    
}
