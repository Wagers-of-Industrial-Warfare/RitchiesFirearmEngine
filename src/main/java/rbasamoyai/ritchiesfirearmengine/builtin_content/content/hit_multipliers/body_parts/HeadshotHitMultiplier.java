package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.body_parts;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record HeadshotHitMultiplier(float multiplier) implements RFEHitMultiplier {

    private static final TagKey<EntityType<?>> HUMANOID = TagKey.create(Registries.ENTITY_TYPE, RitchiesFirearmEngine.resource("humanoid"));

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        if (!target.getType().is(HUMANOID))
            return damage;
        double yHit = hitResult.getLocation().y;
        double headHalf = Math.abs(target.getBbHeight() - target.getEyeHeight());
        if (target.getEyeY() - yHit <= headHalf || yHit - target.getEyeY() < headHalf + 0.1f)
            damage *= this.multiplier;
        return damage;
    }

    @Override public RFEHitMultiplier.Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.HEADSHOT; }
    @Override public float getMultiplier() { return this.multiplier; }

}
