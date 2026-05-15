package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.goal;

import net.minecraft.util.RandomSource;
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
    private final float closeInDistance;
    private final float firingCutoffRange;
    private final float inaccuracyDegrees;
    private int seeTime;
    private int attackDelay;
    private int shotsFirable;
    private int updatePathDelay;

    // TODO dual wielding
    private RFEFirearmItem.Action firearmAction = null;

    // TODO more parameters for accuracy/control
    public RangedFirearmAttackGoal(Mob mob, double baseSpeedModifier, float closeInDistance, float firingCutoffRange, float inaccuracyDegrees) {
        this.mob = mob;
        this.baseSpeedModifier = baseSpeedModifier;
        this.firingCutoffRange = firingCutoffRange;
        this.closeInDistance = closeInDistance * closeInDistance;
        this.inaccuracyDegrees = inaccuracyDegrees;
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
    public void start() {
        super.start();
        this.attackDelay = 60 + this.mob.getRandom().nextInt(21);
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
            firearmItem.stopAiming(mainhandItem, this.mob);
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
        boolean outOfFiringRange = sqrDist > this.firingCutoffRange * this.firingCutoffRange;
        boolean outOfRangeOrCantReach = (sqrDist > (double) this.closeInDistance || this.seeTime < 5 || outOfFiringRange) && this.attackDelay == 0;
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
        boolean isFiring = this.firearmAction == RFEFirearmItem.Action.FIRING || hasLineOfSight && this.shotsFirable > 0;
        float lookSpeed = isFiring ? 10.0F : 30.0F;
        this.mob.getLookControl().setLookAt(livingentity, lookSpeed, lookSpeed);
        RandomSource random = this.mob.getRandom();
        float dPitch = random.nextFloat() * 2f - 1f;
        float dYaw = random.nextFloat() * 2f - 1f;
        this.mob.turn(this.inaccuracyDegrees * dYaw / 0.15f, this.inaccuracyDegrees * dPitch / 0.15f);

        // TODO dual wielding
        ItemStack mainhandStack = this.mob.getMainHandItem();
        if (!(mainhandStack.getItem() instanceof RFEFirearmItem firearmItem))
            return;
        RFEFirearmItem.Action currentAction = firearmItem.getCurrentAction(mainhandStack);
        if (isBusyAction(this.firearmAction) && !isBusyAction(currentAction)) {
            // TODO modify these parameters into ranges
            this.attackDelay = switch (this.firearmAction) {
                case RELOAD, UNLOAD, COOLDOWN -> 30 + this.mob.getRandom().nextInt(21);
                case DRAW -> 40 + this.mob.getRandom().nextInt(21);
                case CHARGING, SWITCH_MODE -> 10 + this.mob.getRandom().nextInt(6);
                default -> 0;
            };
        }
        this.firearmAction = currentAction;
        if (outOfFiringRange) {
            firearmItem.onReleaseAttackKey(mainhandStack, this.mob);
            this.shotsFirable = 0;
        }
        if (hasLineOfSight && !outOfFiringRange && this.shotsFirable <= 0 && this.attackDelay <= 0) {
            firearmItem.onReleaseAttackKey(mainhandStack, this.mob);
            this.attackDelay = 20 + this.mob.getRandom().nextInt(21); // TODO modify
        }
        switch (this.firearmAction) {
            case RELOAD, UNLOAD, DRAW, COOLDOWN -> {
                if (this.mob.isUsingItem()) {
                    this.mob.stopUsingItem();
                    firearmItem.stopAiming(mainhandStack, this.mob);
                }
            }
            case FIRING -> {
                if (!hasLineOfSight || outOfFiringRange) {
                    if (this.mob.isUsingItem()) {
                        this.mob.stopUsingItem();
                        firearmItem.stopAiming(mainhandStack, this.mob);
                    }
                    firearmItem.onReleaseAttackKey(mainhandStack, this.mob);
                }
            }
            case null -> {
                if (this.attackDelay > 0) {
                    --this.attackDelay;
                    if (this.attackDelay == 0)
                        this.shotsFirable = this.getShotsFirable();
                } else if (hasLineOfSight && !outOfFiringRange && this.shotsFirable > 0) {
                    if (!this.mob.isUsingItem())
                        this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                    firearmItem.tryAiming(mainhandStack, this.mob);
                    firearmItem.onEntityTryAttackOption(mainhandStack, this.mob);
                } else {
                    if (this.mob.isUsingItem()) {
                        this.mob.stopUsingItem();
                        firearmItem.stopAiming(mainhandStack, this.mob);
                    }
                    this.shotsFirable = 0;
                }
            }
            default -> {}
        }
    }

    protected int getShotsFirable() {
        return 2 + this.mob.getRandom().nextInt(3); // TODO add modifier
    }

    protected boolean canRun() {
        boolean canShootAndRun = false;
        return this.firearmAction == null
                || this.firearmAction == RFEFirearmItem.Action.COOLDOWN
                || canShootAndRun && this.firearmAction == RFEFirearmItem.Action.FIRING;
    }

    protected static boolean isBusyAction(RFEFirearmItem.Action action) {
        return action == RFEFirearmItem.Action.RELOAD || action == RFEFirearmItem.Action.UNLOAD || action == RFEFirearmItem.Action.CHARGING
                || action == RFEFirearmItem.Action.SWITCH_MODE || action == RFEFirearmItem.Action.DRAW || action == RFEFirearmItem.Action.COOLDOWN;
    }

    public void decrementShot() {
        if (this.shotsFirable > 0)
            --this.shotsFirable;
    }

}
