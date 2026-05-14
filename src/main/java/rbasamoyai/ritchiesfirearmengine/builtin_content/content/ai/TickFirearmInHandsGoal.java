package rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;

public class TickFirearmInHandsGoal extends Goal {

    private final Mob mob;

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
        // TODO dual wielding
        ItemStack mainhandItem = this.mob.getMainHandItem();
        if (mainhandItem.getItem() instanceof RFEFirearmItem)
            mainhandItem.inventoryTick(this.mob.level(), this.mob, 0, true);
    }

}
