package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai;

import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.IFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

import java.util.EnumSet;

public class RangedFirearmAttackGoal extends Goal {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);

    private final Mob mob;
    private final double baseSpeedModifier;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackDelay;
    private int shotsFirable;
    private int updatePathDelay;

    // TODO dual wielding
    private RFEFirearmItem.Action firearmAction = null;

    public RangedFirearmAttackGoal(Mob mob, double baseSpeedModifier, float attackRadius) {
        this.mob = mob;
        this.baseSpeedModifier = baseSpeedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        // TODO? dual wielding when we get there
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingFirearm();
    }

    protected boolean isValidTarget() { return this.mob.getTarget() != null && this.mob.getTarget().isAlive(); }

    protected boolean isHoldingFirearm() {
        // TODO dual wielding
        //return this.mob.isHolding(s -> s.getItem() instanceof IFirearmItem);
        return this.mob.getMainHandItem().getItem() instanceof IFirearmItem;
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setTarget(null);
        this.seeTime = 0;
        if (this.mob.isUsingItem())
            this.mob.stopUsingItem();
        ItemStack mainhandItem = this.mob.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem firearmItem) {
            if (firearmItem.getCurrentAction(mainhandItem) == RFEFirearmItem.Action.FIRING)
                firearmItem.onReleaseAttackKey(mainhandItem, this.mob);
        }
        // TODO firearm actions? can let them finish
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        // Adapated from RangedCrossbowAttackGoal
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null)
            return;
        boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(livingentity);
        boolean hasSeen = this.seeTime > 0;
        if (hasLineOfSight != hasSeen)
            this.seeTime = 0;

        if (hasLineOfSight) {
            this.seeTime++;
        } else {
            this.seeTime--;
        }

        double sqrDist = this.mob.distanceToSqr(livingentity);
        boolean outOfRangeOrCantReach = (sqrDist > (double) this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
        if (outOfRangeOrCantReach) {
            this.updatePathDelay--;
            if (this.updatePathDelay <= 0) {
                this.mob.getNavigation().moveTo(livingentity, this.canRun() ? this.baseSpeedModifier : this.baseSpeedModifier * 0.5);
                this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
            }
        } else {
            this.updatePathDelay = 0;
            this.mob.getNavigation().stop();
        }

        // TODO config this and also just make this better
        //float lookSpeed = this.firearmAction == RFEFirearmItem.Action.FIRING ? 1.0F : 10.0F;
        float lookSpeed = 30.0f;
        this.mob.getLookControl().setLookAt(livingentity, lookSpeed, lookSpeed);
        // TODO dual wielding
        ItemStack mainhandStack = this.mob.getMainHandItem();
        if (!(mainhandStack.getItem() instanceof RFEFirearmItem firearmItem))
            return;
        RFEFirearmItem.Action currentAction = firearmItem.getCurrentAction(mainhandStack);
        if (isBusyAction(this.firearmAction) && !isBusyAction(currentAction)) {
            this.attackDelay = switch (this.firearmAction) {
                case RELOAD, UNLOAD, COOLDOWN -> 30 + this.mob.getRandom().nextInt(21);
                case DRAW -> 40 + this.mob.getRandom().nextInt(21);
                case CHARGING, SWITCH_MODE -> 10 + this.mob.getRandom().nextInt(6);
                default -> 0;
            };
        }
        this.firearmAction = currentAction;
        if (this.shotsFirable <= 0 && this.attackDelay <= 0) {
            this.attackDelay = 20 + this.mob.getRandom().nextInt(21);
        }
        switch (this.firearmAction) {
            case RELOAD, UNLOAD, DRAW, COOLDOWN -> {
                this.mob.stopUsingItem();
            }
            case FIRING -> {
                if (!hasLineOfSight) {
                    this.mob.stopUsingItem();
                    firearmItem.onReleaseAttackKey(mainhandStack, this.mob);
                }
            }
            case null -> {
                if (this.attackDelay > 0) {
                    --this.attackDelay;
                    if (this.attackDelay == 0)
                        this.shotsFirable = this.getShotsFirable();
                } else if (hasLineOfSight) {
                    this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                    firearmItem.onEntityTryAttackOption(mainhandStack, this.mob);
                } else {
                    this.mob.stopUsingItem();
                    this.shotsFirable = 0;
                }
            }
            default -> {}
        }
    }

    private int getShotsFirable() {
        return 2 + this.mob.getRandom().nextInt(3);
    }

    private boolean canRun() {
        boolean canShootAndRun = false;
        return this.firearmAction == null
                || this.firearmAction == RFEFirearmItem.Action.COOLDOWN
                || canShootAndRun && this.firearmAction == RFEFirearmItem.Action.FIRING;
    }

    private static boolean isBusyAction(RFEFirearmItem.Action action) {
        return action == RFEFirearmItem.Action.RELOAD || action == RFEFirearmItem.Action.UNLOAD || action == RFEFirearmItem.Action.CHARGING
                || action == RFEFirearmItem.Action.SWITCH_MODE || action == RFEFirearmItem.Action.DRAW || action == RFEFirearmItem.Action.COOLDOWN;
    }

    public void decrementShot() {
        if (this.shotsFirable > 0)
            --this.shotsFirable;
    }

}
