package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.misfires;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;

public record SubmergedMisfire(float chance) implements RFEMisfire {

    public static SubmergedMisfire of(float chance) { return new SubmergedMisfire(Mth.clamp(chance, 0, 1)); }

    @Override
    public boolean canMisfire(ItemStack itemStack, LivingEntity entity) {
        return entity.level().getFluidState(BlockPos.containing(entity.getEyePosition())).getType().isSame(Fluids.WATER)
                && entity.getRandom().nextFloat() < this.chance;
    }

    @Override public float getChance() { return this.chance; }

    @Override public Provider getMisfireProvider() { return BuiltInRFEPlugin.Misfires.WHEN_SUBMERGED; }
}
