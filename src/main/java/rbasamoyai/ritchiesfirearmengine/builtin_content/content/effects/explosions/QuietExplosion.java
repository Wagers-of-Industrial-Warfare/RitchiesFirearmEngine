package rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.explosions;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class QuietExplosion extends Explosion implements RFECustomExplosion {

    public QuietExplosion(Level level, @Nullable Entity source, @Nullable DamageSource damageSource,
                          @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius,
                          boolean fire, BlockInteraction blockInteraction, ParticleOptions smallExplosionParticles,
                          ParticleOptions largeExplosionParticles) {
        super(level, source, damageSource, damageCalculator, x, y, z, radius, fire, blockInteraction, smallExplosionParticles,
                largeExplosionParticles, SoundEvents.GENERIC_EXPLODE);
    }

    @Override public void playLocalSound(Level level, double x, double y, double z) {}

}
