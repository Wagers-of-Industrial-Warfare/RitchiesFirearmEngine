package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface RFESpreadInstance {

    /**
     *
     * @param itemStack
     * @param entity
     * @return Tuple with left entry affecting pitch spread (around the x-axis) and right entry affecting yaw spread
     *         (around the y-axis)
     */
    Tuple<Float, Float> getSpread(ItemStack itemStack, LivingEntity entity);

    void updateSpread(ItemStack itemStack, LivingEntity entity);
    void tickSpreadBehavior();
    boolean isRemoved();

}
