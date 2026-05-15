package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags;

import java.util.Optional;

public class RangedFirearmAttackBehavior extends Behavior<Mob> {

    private final MemoryModuleType<Boolean> shotIndicator;
    private final float attackRange;
    private final float inaccuracyDegrees;
    private int attackDelay;
    private int shotsFirable;

    // TODO dual wielding
    private RFEFirearmItem.Action firearmAction = null;

    public RangedFirearmAttackBehavior(MemoryModuleType<Boolean> shotIndicator, float attackRange, float inaccuracyDegrees) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, shotIndicator, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        this.shotIndicator = shotIndicator;
        this.attackRange = attackRange;
        this.inaccuracyDegrees = inaccuracyDegrees;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob owner) {
        LivingEntity livingentity = getAttackTarget(owner);
        return this.isHoldingFirearm(owner)
                && BehaviorUtils.canSee(owner, livingentity)
                && owner.closerThan(livingentity, this.attackRange);
    }

    protected boolean isHoldingFirearm(Mob owner) {
        // TODO dual wielding
        //return entity.isHolding(s -> s.getItem() instanceof RFEFirearmItem);
        return owner.getMainHandItem().getItem() instanceof RFEFirearmItem;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob entity, long gameTime) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && this.checkExtraStartConditions(level, entity);
    }

    @Override
    protected void start(ServerLevel level, Mob entity, long gameTime) {
        this.attackDelay = 20 + entity.getRandom().nextInt(21); // TODO modify
    }

    @Override
    protected void stop(ServerLevel level, Mob entity, long gameTime) {
        super.stop(level, entity, gameTime);
        entity.setAggressive(false);
        entity.setTarget(null);
        if (entity.isUsingItem())
            entity.stopUsingItem();
        ItemStack mainhandItem = entity.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            firearmItem.onReleaseAttackKey(mainhandItem, entity);
            firearmItem.stopAiming(mainhandItem, entity);
        }
        // TODO firearm actions? can let them finish
    }

    @Override
    protected void tick(ServerLevel level, Mob owner, long gameTime) {
        LivingEntity target = getAttackTarget(owner);
        this.lookAtTarget(owner, target);
        this.tickAttack(owner, target);
    }

    protected void lookAtTarget(Mob shooter, LivingEntity target) {
        shooter.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        RandomSource random = shooter.getRandom();
        float dPitch = random.nextFloat() * 2f - 1f;
        float dYaw = random.nextFloat() * 2f - 1f;
        shooter.turn(this.inaccuracyDegrees * dYaw / 0.15f, this.inaccuracyDegrees * dPitch / 0.15f);
    }
    
    protected void tickAttack(Mob shooter, LivingEntity target) {
        if (shooter.getBrain().hasMemoryValue(this.shotIndicator)) {
            if (this.shotsFirable > 0)
                --this.shotsFirable;
            shooter.getBrain().setMemory(this.shotIndicator, Optional.empty());
        }

        // TODO dual wielding
        ItemStack mainhandStack = shooter.getMainHandItem();
        if (!(mainhandStack.getItem() instanceof RFEFirearmItem firearmItem))
            return;
        RFEFirearmItem.Action currentAction = firearmItem.getCurrentAction(mainhandStack);
        if (isBusyAction(this.firearmAction) && !isBusyAction(currentAction)) {
            // TODO modify these parameters into ranges
            this.attackDelay = switch (this.firearmAction) {
                case RELOAD, UNLOAD, COOLDOWN -> 30 + shooter.getRandom().nextInt(21);
                case DRAW -> 40 + shooter.getRandom().nextInt(21);
                case CHARGING, SWITCH_MODE -> 10 + shooter.getRandom().nextInt(6);
                default -> 0;
            };
        }
        this.firearmAction = currentAction;
        if (this.shotsFirable <= 0 && this.attackDelay <= 0) {
            firearmItem.onReleaseAttackKey(mainhandStack, shooter);
            this.attackDelay = 20 + shooter.getRandom().nextInt(21); // TODO modify
        }
        switch (this.firearmAction) {
            case RELOAD, UNLOAD, DRAW, COOLDOWN -> {
                if (shooter.isUsingItem()) {
                    shooter.stopUsingItem();
                    firearmItem.stopAiming(mainhandStack, shooter);
                }
            }
            case null -> {
                if (this.attackDelay > 0) {
                    --this.attackDelay;
                    if (this.attackDelay == 0)
                        this.shotsFirable = this.getShotsFirable(shooter);
                } else if (this.shotsFirable > 0) {
                    if (!shooter.isUsingItem())
                        shooter.startUsingItem(InteractionHand.MAIN_HAND);
                    firearmItem.tryAiming(mainhandStack, shooter);
                    firearmItem.onEntityTryAttackOption(mainhandStack, shooter);
                } else {
                    if (shooter.isUsingItem()) {
                        shooter.stopUsingItem();
                        firearmItem.stopAiming(mainhandStack, shooter);
                    }
                    this.shotsFirable = 0;
                }
            }
            default -> {}
        }
    }

    protected int getShotsFirable(Mob shooter) {
        return 2 + shooter.getRandom().nextInt(3); // TODO add modifier
    }

    protected static boolean isBusyAction(RFEFirearmItem.Action action) {
        return action == RFEFirearmItem.Action.RELOAD || action == RFEFirearmItem.Action.UNLOAD || action == RFEFirearmItem.Action.CHARGING
                || action == RFEFirearmItem.Action.SWITCH_MODE || action == RFEFirearmItem.Action.DRAW || action == RFEFirearmItem.Action.COOLDOWN;
    }

    private static LivingEntity getAttackTarget(LivingEntity shooter) {
        return shooter.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    public static boolean isHoldingShootableFirearm(LivingEntity entity) {
        if (!entity.getType().is(RFETags.RFEEntityTypeTags.CAN_SHOOT_FIREARMS.tag))
            return false;
        // TODO a bit more complex
        return entity.isHolding(item -> item.getItem() instanceof RFEFirearmItem);
    }

}
