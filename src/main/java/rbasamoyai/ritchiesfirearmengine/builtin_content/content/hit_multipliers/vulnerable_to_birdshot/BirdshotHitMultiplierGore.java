package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.vulnerable_to_birdshot;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record BirdshotHitMultiplierGore(float multiplier) implements RFEHitMultiplier {

    private static final TagKey<EntityType<?>> VULNERABLE_TO_BIRDSHOT = TagKey.create(Registries.ENTITY_TYPE, RitchiesFirearmEngine.resource("vulnerable_to_birdshot"));

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        if (!target.getType().is(VULNERABLE_TO_BIRDSHOT))
            return damage;
        damage *= this.multiplier;
        if (target instanceof LivingEntity living && damage >= living.getHealth()) {
            Level level = target.level();
            ParticleOptions pinkMist = new DustParticleOptions(new Vector3f(0.6f, 0.05f, 0.1f), 2f);
            Vec3 dir = projectile.velocity().normalize();
            Vec3 exitWound = dir.scale(target.getBbWidth() * 1.2).add(hitResult.getLocation());
            RandomSource random = living.getRandom();
            if (level instanceof ServerLevel slevel) {
                for (ServerPlayer splayer : slevel.players()) {
                    slevel.sendParticles(splayer, pinkMist, true, exitWound.x + random.nextGaussian() * 0.05d,
                            exitWound.y + random.nextGaussian() * 0.05d,
                            exitWound.z + random.nextGaussian() * 0.05d,
                            20, 0.25d, 0.25d, 0.25d, 1);
                }
            }
            level.playSound(null, exitWound.x, exitWound.y, exitWound.z, SoundEvents.GRASS_BREAK, SoundSource.NEUTRAL, 1, 0.3f);
        }
        return damage;
    }

    @Override public Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.VULNERABLE_TO_BIRDSHOT_GORE; }
    @Override public float getMultiplier() { return this.multiplier; }

}
