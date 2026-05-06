package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import java.util.Optional;

public class RFEExplosiveProjectileType extends RFEBulletProjectileType implements RFEProjectileType.HasCombinedProjectiles {

    protected final float blockExplosionPower;
    protected final float entityExplosionPower;
    protected final double size;
    protected final float detonationThresholdDamage;
    @Nullable protected final RFEBulletProjectileType penetratorProjectileType;

    public RFEExplosiveProjectileType(RFEBaseProjectilePropertiesBuilder builder, float blockExplosionPower,
                                      float entityExplosionPower, double size, float detonationThresholdDamage,
                                      @Nullable RFEBulletProjectileType penetratorProjectileType) {
        super(builder);
        this.blockExplosionPower = blockExplosionPower;
        this.entityExplosionPower = entityExplosionPower;
        this.size = size;
        this.detonationThresholdDamage = detonationThresholdDamage;
        this.penetratorProjectileType = penetratorProjectileType;
    }

    @Override
    protected void onHit(RFEProjectileInstance instance, Level level, HitResult hitResult, Map<BlockPos, BlockState> penetratedBlocks) {
        super.onHit(instance, level, hitResult, penetratedBlocks);
        this.explode(instance, hitResult.getLocation(), level);
    }

    @Override
    protected void onExpiry(RFEProjectileInstance instance, Level level) {
        super.onExpiry(instance, level);
        this.explode(instance, instance.position(), level);
    }

    protected void explode(RFEProjectileInstance instance, Vec3 position, Level level) {
        // Block explosion first
        Registry<DamageType> reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        LivingEntity livingOwner = instance.getOwner() instanceof LivingEntity living ? living : null;
        DamageSource damageSource = reg.getHolder(this.damageTypeKey)
                .map(type -> new DamageSource(type, null, livingOwner))
                .orElseGet(() -> level.damageSources().explosion(null, livingOwner));

        // TODO config block explosion, also possibly effects
        Explosion.BlockInteraction interaction = Explosion.BlockInteraction.DESTROY_WITH_DECAY;
        Explosion blockExplosion = new Explosion(level, null, damageSource, SelectiveExplosionDamageCalculator.blockDamage(),
                position.x, position.y, position.z, this.blockExplosionPower, false, interaction, ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
        RFEUtils.explode(level, blockExplosion, level.isClientSide);

        level.explode(null, damageSource, SelectiveExplosionDamageCalculator.entityDamage(), position.x, position.y,
                position.z, this.entityExplosionPower, false, Level.ExplosionInteraction.NONE, ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
        instance.setRemoved();

        if (this.penetratorProjectileType != null) {
            RFEProjectileInstance penetratorInstance = this.penetratorProjectileType.createInstance();
            penetratorInstance.setPosition(instance.position());
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

    public static class Serializer implements RFEProjectileType.Serializer<RFEExplosiveProjectileType> {
        private static final MapCodec<Pair<Float, Float>> EXPLOSION_POWER_CODEC = Codec.mapEither(
                RecordCodecBuilder.<Pair<Float, Float>>mapCodec(o -> o.group(
                        Codec.floatRange(0f, 100f).fieldOf("block_explosion_power").forGetter(Pair::getFirst),
                        Codec.floatRange(0f, 100f).fieldOf("entity_explosion_power").forGetter(Pair::getSecond)
                ).apply(o, Pair::of)),
                Codec.floatRange(0f, 100f).fieldOf("explosion_power")
        ).xmap(either -> Either.unwrap(either.mapRight(f -> new Pair<>(f, f))), Either::left);

        private static final MapCodec<RFEExplosiveProjectileType> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEBaseProjectilePropertiesBuilder.CODEC.forGetter(RFEExplosiveProjectileType::makeProjectileProperties),
                EXPLOSION_POWER_CODEC.forGetter(t -> new Pair<>(t.blockExplosionPower, t.entityExplosionPower)),
                Codec.doubleRange(0d, 4d).optionalFieldOf("size", 0.3d).forGetter(t -> t.size),
                Codec.floatRange(0f, 1f).fieldOf("detonation_threshold_damage").forGetter(t -> t.detonationThresholdDamage),
                RFEBulletProjectileType.Serializer.CODEC.codec().optionalFieldOf("penetrator_properties")
                        .xmap(op -> op.orElse(null), Optional::ofNullable).forGetter(t -> t.penetratorProjectileType)
        ).apply(o, (prop, power, size, threshold, pen) -> new RFEExplosiveProjectileType(prop, power.getFirst(), power.getSecond(), size, threshold, pen)));

        private static final StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectileType> STREAM_CODEC = StreamCodec.composite(
                RFEBaseProjectilePropertiesBuilder.STREAM_CODEC, RFEExplosiveProjectileType::makeProjectileProperties,
                ByteBufCodecs.FLOAT, t -> t.blockExplosionPower,
                ByteBufCodecs.FLOAT, t -> t.entityExplosionPower,
                ByteBufCodecs.DOUBLE, t -> t.size,
                ByteBufCodecs.FLOAT, t -> t.detonationThresholdDamage,
                ByteBufCodecs.optional(RFEBulletProjectileType.Serializer.STREAM_CODEC), t -> Optional.ofNullable(t.penetratorProjectileType),
                (prop, blockPower, entityPower, size, threshold, oPen) ->
                        new RFEExplosiveProjectileType(prop, blockPower, entityPower, size, threshold, oPen.orElse(null)));

        @Override public MapCodec<RFEExplosiveProjectileType> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
