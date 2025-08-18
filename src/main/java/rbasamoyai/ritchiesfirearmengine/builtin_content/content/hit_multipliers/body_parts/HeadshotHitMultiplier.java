package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.body_parts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record HeadshotHitMultiplier(float multiplier) implements RFEHitMultiplier {

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        if (!target.getType().is(RFETags.RFEEntityTypeTags.HUMANOID.tag))
            return damage;
        double diff = hitResult.getLocation().y - target.getEyeY();
        double headHalf = Math.abs(target.getBbHeight() - target.getEyeHeight());
        if (diff < 0 && -diff <= headHalf || diff >= 0 && diff < headHalf + 0.1f)
            damage *= this.multiplier;
        return damage;
    }

    @Override public RFEHitMultiplier.Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.HEADSHOT; }
    @Override public float getMultiplier() { return this.multiplier; }

}
