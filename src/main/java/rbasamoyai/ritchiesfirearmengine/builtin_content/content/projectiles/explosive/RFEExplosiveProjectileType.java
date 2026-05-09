package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEBaseProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.Map;

public class RFEExplosiveProjectileType extends RFEBulletProjectileType implements RFEProjectileType.HasCombinedProjectiles {

    protected final float blockExplosionPower;
    protected final float entityExplosionPower;
    protected final float entityDamageScale;
    protected final float entityKnockbackScale;
    protected final double size;
    protected final float detonationThresholdDamage;
    protected final int armingTime;
    protected final ResourceKey<DamageType> explosionDamageTypeKey;
    @Nullable protected final RFEBulletProjectileType penetratorProjectileType;

    public RFEExplosiveProjectileType(RFEBaseProjectilePropertiesBuilder baseProperties,
                                      RFEExplosiveProjectilePropertiesBuilder explosiveProperties) {
        super(baseProperties);
        this.blockExplosionPower = explosiveProperties.blockExplosionPower;
        this.entityExplosionPower = explosiveProperties.entityExplosionPower;
        this.entityDamageScale = explosiveProperties.entityDamageScale;
        this.entityKnockbackScale = explosiveProperties.entityKnockbackScale;
        this.size = explosiveProperties.size;
        this.detonationThresholdDamage = explosiveProperties.detonationThresholdDamage;
        this.armingTime = explosiveProperties.armingTime;
        this.explosionDamageTypeKey = explosiveProperties.explosionDamageTypeKey;
        this.penetratorProjectileType = explosiveProperties.penetratorProjectileType;
        if (this.armingTime > this.maxAge)
            throw new IllegalStateException("arming_time cannot be greater than max_age");
    }

    @Override
    protected void onHit(RFEProjectileInstance instance, Level level, HitResult hitResult, Map<BlockPos, BlockState> penetratedBlocks) {
        super.onHit(instance, level, hitResult, penetratedBlocks);
        if (instance.age() >= this.armingTime)
            this.explode(instance, hitResult.getLocation(), level);
    }

    @Override
    protected void onExpiry(RFEProjectileInstance instance, Level level) {
        super.onExpiry(instance, level);
        this.explode(instance, instance.position(), level); // Explode regardless of arming time
    }

    @Override
    public void tick(Level level, RFEProjectileInstance instance) {
        super.tick(level, instance);
        if (instance.health() <= this.detonationThresholdDamage && instance.age() >= this.armingTime)
            this.explode(instance, instance.position(), level);
    }

    protected void explode(RFEProjectileInstance instance, Vec3 position, Level level) {
        // Block explosion first
        Registry<DamageType> reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        LivingEntity livingOwner = instance.getOwner() instanceof LivingEntity living ? living : null;
        DamageSource damageSource = reg.getHolder(this.explosionDamageTypeKey)
                .map(type -> new DamageSource(type, null, livingOwner))
                .orElseGet(() -> level.damageSources().explosion(null, livingOwner));

        // TODO config block explosion, also possibly effects
        Explosion.BlockInteraction interaction = Explosion.BlockInteraction.DESTROY_WITH_DECAY;
        Explosion blockExplosion = new Explosion(level, null, damageSource, SelectiveExplosionDamageCalculator.blockDamage(),
                position.x, position.y, position.z, this.blockExplosionPower, false, interaction, ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
        RFEUtils.explode(level, blockExplosion, level.isClientSide);

        level.explode(null, damageSource, SelectiveExplosionDamageCalculator.entityDamage(this.entityDamageScale, this.entityKnockbackScale),
                position.x, position.y, position.z, this.entityExplosionPower, false, Level.ExplosionInteraction.NONE,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
        instance.setRemoved();

        if (this.penetratorProjectileType != null) {
            RFEProjectileInstance penetratorInstance = this.penetratorProjectileType.createInstance();
            penetratorInstance.setPosition(position);
            penetratorInstance.setOwner(instance.getOwner());
            Vec3 vel = instance.velocity();
            this.penetratorProjectileType.shootWithoutEntity(penetratorInstance, vel.x, vel.y, vel.z, level);
            RFEProjectileManager.queueAddedProjectile(penetratorInstance, level);
        }
    }

    @Override
    public Map<String, RFEProjectileType> getSubprojectileTypes() {
        return this.penetratorProjectileType == null ? Map.of() : Map.of("penetrator", this.penetratorProjectileType);
    }

    @Override protected double getHitboxInflation(Level level, RFEProjectileInstance instance, double distance) { return this.size; }

    @Override public RFEProjectileType.Serializer<?> getSerializer() { return BuiltInRFEPlugin.ProjectileTypes.EXPLOSIVE; }

    public static RFEExplosiveProjectilePropertiesBuilder makeExplosiveProperties(RFEExplosiveProjectileType type) {
        RFEExplosiveProjectilePropertiesBuilder properties = new RFEExplosiveProjectilePropertiesBuilder();
        properties.blockExplosionPower = type.blockExplosionPower;
        properties.entityExplosionPower = type.entityExplosionPower;
        properties.entityDamageScale = type.entityDamageScale;
        properties.entityKnockbackScale = type.entityKnockbackScale;
        properties.size = type.size;
        properties.detonationThresholdDamage = type.detonationThresholdDamage;
        properties.armingTime = type.armingTime;
        properties.explosionDamageTypeKey = type.explosionDamageTypeKey;
        properties.penetratorProjectileType = type.penetratorProjectileType;
        return properties;
    }

    public static class Serializer implements RFEProjectileType.Serializer<RFEExplosiveProjectileType> {
        private static final MapCodec<RFEExplosiveProjectileType> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEBaseProjectilePropertiesBuilder.CODEC.forGetter(RFEExplosiveProjectileType::makeProjectileProperties),
                RFEExplosiveProjectilePropertiesBuilder.CODEC.forGetter(RFEExplosiveProjectileType::makeExplosiveProperties)
        ).apply(o, RFEExplosiveProjectileType::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectileType> STREAM_CODEC = StreamCodec.composite(
                RFEBaseProjectilePropertiesBuilder.STREAM_CODEC, RFEExplosiveProjectileType::makeProjectileProperties,
                RFEExplosiveProjectilePropertiesBuilder.STREAM_CODEC, RFEExplosiveProjectileType::makeExplosiveProperties,
                RFEExplosiveProjectileType::new);

        @Override public MapCodec<RFEExplosiveProjectileType> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
