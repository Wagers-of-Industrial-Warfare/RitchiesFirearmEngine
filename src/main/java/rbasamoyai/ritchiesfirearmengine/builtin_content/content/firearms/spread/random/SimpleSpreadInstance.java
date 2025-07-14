package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.random;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;

public class SimpleSpreadInstance implements RFESpreadInstance {

    private final float radius;
    private final float unaimedRadius;
    private final boolean tighten;

    public SimpleSpreadInstance(float radius, float unaimedRadius, boolean tighten) {
        this.radius = radius;
        this.unaimedRadius = unaimedRadius;
        this.tighten = tighten;
    }

    @Override
    public Tuple<Float, Float> getSpread(ItemStack itemStack, LivingEntity entity) {
        float radius = FirearmDataUtils.isAiming(itemStack) ? this.radius : this.unaimedRadius;
        return RFEProjectileUtils.standardSpreadAngles(radius, this.tighten, entity.getRandom());
    }

    @Override public void updateSpread(ItemStack itemStack, LivingEntity entity) {}

    @Override public void tickSpreadBehavior() {}

    @Override public boolean isRemoved() { return true; }

}
