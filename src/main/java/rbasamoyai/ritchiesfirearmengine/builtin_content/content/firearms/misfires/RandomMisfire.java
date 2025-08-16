package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.misfires;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;

public record RandomMisfire(float chance) implements RFEMisfire {

    public static RandomMisfire of(float chance) { return new RandomMisfire(Mth.clamp(chance, 0, 1)); }

    @Override
    public boolean canMisfire(ItemStack itemStack, LivingEntity entity) {
        return entity.getRandom().nextFloat() < this.chance;
    }

    @Override public float getChance() { return this.chance; }

    @Override public Provider getMisfireProvider() { return BuiltInRFEPlugin.Misfires.RANDOM; }

}
