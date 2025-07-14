package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;

public class NoSpreadInstance implements RFESpreadInstance {

    public static final NoSpreadInstance INSTANCE = new NoSpreadInstance();

    private static final Tuple<Float, Float> ZERO_SPREAD = new Tuple<>(0f, 0f);

    private NoSpreadInstance() {}

    @Override public Tuple<Float, Float> getSpread(ItemStack itemStack, LivingEntity entity) { return ZERO_SPREAD; }

    @Override public void updateSpread(ItemStack itemStack, LivingEntity entity) {}

    @Override public void tickSpreadBehavior() {}

    @Override public boolean isRemoved() { return true; }

}
