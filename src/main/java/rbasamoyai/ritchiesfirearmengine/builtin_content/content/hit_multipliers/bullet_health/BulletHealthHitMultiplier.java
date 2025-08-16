package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.bullet_health;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record BulletHealthHitMultiplier(float multi) implements RFEHitMultiplier {

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        return damage * Math.max(projectile.health(), 0.05f);
    }

    @Override public Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.BULLET_HEALTH; }

    @Override public float getMultiplier() { return 1; }
}
