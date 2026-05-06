package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;

public class SelectiveExplosionDamageCalculator extends ExplosionDamageCalculator {

    private final boolean enableBlockDamage;
    private final boolean enableEntityDamage;

    public SelectiveExplosionDamageCalculator(boolean enableBlockDamage, boolean enableEntityDamage) {
        this.enableBlockDamage = enableBlockDamage;
        this.enableEntityDamage = enableEntityDamage;
    }

    public static SelectiveExplosionDamageCalculator blockDamage() { return new SelectiveExplosionDamageCalculator(true, false); }

    public static SelectiveExplosionDamageCalculator entityDamage() { return new SelectiveExplosionDamageCalculator(false, true); }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, float power) {
        return this.enableBlockDamage && super.shouldBlockExplode(explosion, reader, pos, state, power);
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        return this.enableEntityDamage && super.shouldDamageEntity(explosion, entity);
    }

    @Override
    public float getKnockbackMultiplier(Entity entity) {
        return this.enableEntityDamage ? super.getKnockbackMultiplier(entity) : 0f;
    }

}
