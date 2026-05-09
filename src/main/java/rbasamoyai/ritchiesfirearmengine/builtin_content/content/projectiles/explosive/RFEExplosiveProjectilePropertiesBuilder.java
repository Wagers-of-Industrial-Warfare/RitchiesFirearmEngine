package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import javax.annotation.Nullable;
import java.util.Optional;

public class RFEExplosiveProjectilePropertiesBuilder {

    private static final MapCodec<Pair<Float, Float>> EXPLOSION_POWER_CODEC = Codec.mapEither(
            RecordCodecBuilder.<Pair<Float, Float>>mapCodec(o -> o.group(
                    Codec.floatRange(0f, 100f).fieldOf("block_explosion_power").forGetter(Pair::getFirst),
                    Codec.floatRange(0f, 100f).fieldOf("entity_explosion_power").forGetter(Pair::getSecond)
            ).apply(o, Pair::of)),
            Codec.floatRange(0f, 100f).fieldOf("explosion_power")
    ).xmap(either -> Either.unwrap(either.mapRight(f -> new Pair<>(f, f))), Either::left);

    public static final MapCodec<RFEExplosiveProjectilePropertiesBuilder> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
            EXPLOSION_POWER_CODEC.forGetter(t -> new Pair<>(t.blockExplosionPower, t.entityExplosionPower)),
            Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("entity_explosion_damage_scale", 1f).forGetter(t -> t.entityDamageScale),
            Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("entity_explosion_knockback_scale", 1f).forGetter(t -> t.entityKnockbackScale),
            Codec.doubleRange(0d, 4d).optionalFieldOf("size", 0.3d).forGetter(t -> t.size),
            Codec.floatRange(0f, 1f).fieldOf("detonation_threshold_damage").forGetter(t -> t.detonationThresholdDamage),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("arming_time", 0).forGetter(t -> t.armingTime),
            ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf("explosion_damage_type").forGetter(b -> b.explosionDamageTypeKey),
            RFEBulletProjectileType.Serializer.CODEC.codec().optionalFieldOf("penetrator_properties").forGetter(t -> Optional.ofNullable(t.penetratorProjectileType))
    ).apply(o, RFEExplosiveProjectilePropertiesBuilder::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectilePropertiesBuilder> STREAM_CODEC = RFEByteBufCodecUtils.composite9(
            ByteBufCodecs.FLOAT, t -> t.blockExplosionPower,
            ByteBufCodecs.FLOAT, t -> t.entityExplosionPower,
            ByteBufCodecs.FLOAT, t -> t.entityDamageScale,
            ByteBufCodecs.FLOAT, t -> t.entityKnockbackScale,
            ByteBufCodecs.DOUBLE, t -> t.size,
            ByteBufCodecs.FLOAT, t -> t.detonationThresholdDamage,
            ByteBufCodecs.VAR_INT, t -> t.armingTime,
            ResourceKey.streamCodec(Registries.DAMAGE_TYPE), t -> t.explosionDamageTypeKey,
            ByteBufCodecs.optional(RFEBulletProjectileType.Serializer.STREAM_CODEC)
                    .map(op -> op.orElse(null), Optional::ofNullable), t -> t.penetratorProjectileType,
            RFEExplosiveProjectilePropertiesBuilder::fromStreamCodec);

    public float blockExplosionPower;
    public float entityExplosionPower;
    public float entityDamageScale;
    public float entityKnockbackScale;
    public double size = 0.3d;
    public float detonationThresholdDamage;
    public int armingTime;
    public ResourceKey<DamageType> explosionDamageTypeKey;
    @Nullable public RFEBulletProjectileType penetratorProjectileType = null;

    public RFEExplosiveProjectilePropertiesBuilder() {}

    private static RFEExplosiveProjectilePropertiesBuilder fromCodec(Pair<Float, Float> explosionPower, float entityDamageScale,
            float entityKnockbackScale, double size, float detonationThresholdDamage, int armingTime,
            ResourceKey<DamageType> explosionDamageTypeKey,
            Optional<RFEBulletProjectileType> penetratorProjectileType) {
        RFEExplosiveProjectilePropertiesBuilder properties = new RFEExplosiveProjectilePropertiesBuilder();
        properties.blockExplosionPower = explosionPower.getFirst();
        properties.entityExplosionPower = explosionPower.getSecond();
        properties.entityDamageScale = entityDamageScale;
        properties.entityKnockbackScale = entityKnockbackScale;
        properties.size = size;
        properties.detonationThresholdDamage = detonationThresholdDamage;
        properties.armingTime = armingTime;
        properties.explosionDamageTypeKey = explosionDamageTypeKey;
        properties.penetratorProjectileType = penetratorProjectileType.orElse(null);
        return properties;
    }

    private static RFEExplosiveProjectilePropertiesBuilder fromStreamCodec(float blockExplosionPower, float entityExplosionPower,
            float entityDamageScale, float entityKnockbackScale, double size, float detonationThresholdDamage, int armingTime,
            ResourceKey<DamageType> explosionDamageTypeKey, @Nullable RFEBulletProjectileType penetratorProjectileType) {
        RFEExplosiveProjectilePropertiesBuilder properties = new RFEExplosiveProjectilePropertiesBuilder();
        properties.blockExplosionPower = blockExplosionPower;
        properties.entityExplosionPower = entityExplosionPower;
        properties.entityDamageScale = entityDamageScale;
        properties.entityKnockbackScale = entityKnockbackScale;
        properties.size = size;
        properties.detonationThresholdDamage = detonationThresholdDamage;
        properties.armingTime = armingTime;
        properties.explosionDamageTypeKey = explosionDamageTypeKey;
        properties.penetratorProjectileType = penetratorProjectileType;
        return properties;
    }

}
