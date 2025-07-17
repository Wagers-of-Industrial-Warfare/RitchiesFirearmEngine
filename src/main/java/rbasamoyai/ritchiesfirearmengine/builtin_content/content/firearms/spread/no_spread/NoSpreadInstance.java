package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;

public class NoSpreadInstance implements RFESpreadInstance {

    public static final NoSpreadInstance INSTANCE = new NoSpreadInstance();

    private NoSpreadInstance() {}

    @Override public RFEAimAngles getSpread(ItemStack itemStack, LivingEntity entity) { return RFEAimAngles.ZERO_ANGLES; }

    @Override public void updateSpread(ItemStack itemStack, LivingEntity entity) {}

    @Override public void tickSpreadBehavior() {}

    @Override public boolean isRemoved() { return true; }

}
