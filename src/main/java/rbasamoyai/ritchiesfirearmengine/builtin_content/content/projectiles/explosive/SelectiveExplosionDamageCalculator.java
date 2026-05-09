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
    private final float entityDamageScale;
    private final float entityKnockbackScale;

    public SelectiveExplosionDamageCalculator(boolean enableBlockDamage, boolean enableEntityDamage, float entityDamageScale, float entityKnockbackScale) {
        this.enableBlockDamage = enableBlockDamage;
        this.enableEntityDamage = enableEntityDamage;
        this.entityDamageScale = entityDamageScale;
        this.entityKnockbackScale = entityKnockbackScale;
    }

    public static SelectiveExplosionDamageCalculator blockDamage() { return new SelectiveExplosionDamageCalculator(true, false, 1.0f, 1.0f); }

    public static SelectiveExplosionDamageCalculator entityDamage(float damageScale, float knockbackScale) {
        return new SelectiveExplosionDamageCalculator(false, true, damageScale, knockbackScale);
    }

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
        return this.enableEntityDamage ? super.getKnockbackMultiplier(entity) * this.entityKnockbackScale : 0f;
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity) {
        return super.getEntityDamageAmount(explosion, entity) * this.entityDamageScale;
    }

}
