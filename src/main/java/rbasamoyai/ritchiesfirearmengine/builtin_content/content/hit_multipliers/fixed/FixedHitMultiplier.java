package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.fixed;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record FixedHitMultiplier(float multiplier) implements RFEHitMultiplier {

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        return damage * this.multiplier;
    }

    @Override public RFEHitMultiplier.Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.FIXED; }

    @Override public float getMultiplier() { return this.multiplier; }

}
