package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.vulnerable_to_birdshot;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record BirdshotHitMultiplier(float multiplier) implements RFEHitMultiplier {

    private static final TagKey<EntityType<?>> VULNERABLE_TO_BIRDSHOT = TagKey.create(Registries.ENTITY_TYPE, RitchiesFirearmEngine.resource("vulnerable_to_birdshot"));

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        if (target.getType().is(VULNERABLE_TO_BIRDSHOT))
            damage *= this.multiplier;
        return damage;
    }

    @Override public Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.VULNERABLE_TO_BIRDSHOT; }
    @Override public float getMultiplier() { return this.multiplier; }

}
