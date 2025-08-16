package rbasamoyai.ritchiesfirearmengine.foundation.api.misfires;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public interface RFEMisfire {

    boolean canMisfire(ItemStack itemStack, LivingEntity entity);
    float getChance();
    Provider getMisfireProvider();

    @FunctionalInterface
    interface Provider extends Function<Float, RFEMisfire> {
    }

}
