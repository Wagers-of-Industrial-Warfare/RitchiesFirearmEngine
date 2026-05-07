package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;

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
            Codec.doubleRange(0d, 4d).optionalFieldOf("size", 0.3d).forGetter(t -> t.size),
            Codec.floatRange(0f, 1f).fieldOf("detonation_threshold_damage").forGetter(t -> t.detonationThresholdDamage),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("arming_time", 0).forGetter(t -> t.armingTime),
            RFEBulletProjectileType.Serializer.CODEC.codec().optionalFieldOf("penetrator_properties")
                        .xmap(op -> op.orElse(null), Optional::ofNullable).forGetter(t -> t.penetratorProjectileType)
    ).apply(o, RFEExplosiveProjectilePropertiesBuilder::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveProjectilePropertiesBuilder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, t -> t.blockExplosionPower,
            ByteBufCodecs.FLOAT, t -> t.entityExplosionPower,
            ByteBufCodecs.DOUBLE, t -> t.size,
            ByteBufCodecs.FLOAT, t -> t.detonationThresholdDamage,
            ByteBufCodecs.VAR_INT, t -> t.armingTime,
            ByteBufCodecs.optional(RFEBulletProjectileType.Serializer.STREAM_CODEC)
                    .map(op -> op.orElse(null), Optional::ofNullable), t -> t.penetratorProjectileType,
            RFEExplosiveProjectilePropertiesBuilder::fromStreamCodec);

    public float blockExplosionPower;
    public float entityExplosionPower;
    public double size = 0.3d;
    public float detonationThresholdDamage;
    public int armingTime;
    @Nullable public RFEBulletProjectileType penetratorProjectileType = null;

    public RFEExplosiveProjectilePropertiesBuilder() {}

    private static RFEExplosiveProjectilePropertiesBuilder fromCodec(Pair<Float, Float> explosionPower, double size,
            float detonationThresholdDamage, int armingTime, @Nullable RFEBulletProjectileType penetratorProjectileType) {
        RFEExplosiveProjectilePropertiesBuilder properties = new RFEExplosiveProjectilePropertiesBuilder();
        properties.blockExplosionPower = explosionPower.getFirst();
        properties.entityExplosionPower = explosionPower.getSecond();
        properties.size = size;
        properties.detonationThresholdDamage = detonationThresholdDamage;
        properties.armingTime = armingTime;
        properties.penetratorProjectileType = penetratorProjectileType;
        return properties;
    }

    private static RFEExplosiveProjectilePropertiesBuilder fromStreamCodec(float blockExplosionPower, float entityExplosionPower,
            double size, float detonationThresholdDamage, int armingTime, @Nullable RFEBulletProjectileType penetratorProjectileType) {
        RFEExplosiveProjectilePropertiesBuilder properties = new RFEExplosiveProjectilePropertiesBuilder();
        properties.blockExplosionPower = blockExplosionPower;
        properties.entityExplosionPower = entityExplosionPower;
        properties.size = size;
        properties.detonationThresholdDamage = detonationThresholdDamage;
        properties.armingTime = armingTime;
        properties.penetratorProjectileType = penetratorProjectileType;
        return properties;
    }

}
