package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface CompareValueSource {

    float getValue(ItemStack itemStack, LivingEntity entity);

}
