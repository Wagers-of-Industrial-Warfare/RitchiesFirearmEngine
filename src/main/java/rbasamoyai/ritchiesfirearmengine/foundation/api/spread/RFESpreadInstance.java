package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;

public interface RFESpreadInstance {

    RFEAimAngles getSpread(ItemStack itemStack, LivingEntity entity);

    void updateSpread(ItemStack itemStack, LivingEntity entity);
    void tickSpreadBehavior();
    boolean isRemoved();

}
