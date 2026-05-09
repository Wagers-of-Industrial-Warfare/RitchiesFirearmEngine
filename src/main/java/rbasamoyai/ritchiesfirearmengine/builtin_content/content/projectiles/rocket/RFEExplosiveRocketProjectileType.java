package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.rocket;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEBaseProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive.RFEExplosiveProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive.RFEExplosiveProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;

import javax.annotation.Nullable;

public class RFEExplosiveRocketProjectileType extends RFEExplosiveProjectileType {

    protected final int rocketActivationTime;
    protected final double acceleration;
    protected final double terminalVelocity;
    @Nullable protected final SoundEvent rocketSound;

    public RFEExplosiveRocketProjectileType(RFEBaseProjectilePropertiesBuilder baseProperties,
                                            RFEExplosiveProjectilePropertiesBuilder explosiveProperties,
                                            RFERocketPropertiesBuilder rocketProperties) {
        super(baseProperties, explosiveProperties);
        this.rocketActivationTime = rocketProperties.rocketActivationTime;
        this.acceleration = rocketProperties.acceleration;
        this.terminalVelocity = rocketProperties.terminalVelocity;
        this.rocketSound = rocketProperties.rocketSound;
        int deltaVelSgn = Mth.sign(this.terminalVelocity - this.muzzleVelocity);
        if (Mth.sign(this.acceleration) != deltaVelSgn && deltaVelSgn != 0)
            throw new IllegalStateException("acceleration goes in opposite direction of change between muzzle and terminal velocity of rocket");
    }

    @Override
    public void tick(Level level, RFEProjectileInstance instance) {
        super.tick(level, instance);
        if (!instance.isRemoved() && this.rocketSound != null && !level.isClientSide) {
            Vec3 pos = instance.position();
            level.playSound(null, pos.x, pos.y, pos.z, this.rocketSound, SoundSource.NEUTRAL);
        }
    }

    @Override
    protected Vec3 applyAccelerationToVelocity(Level level, RFEProjectileInstance instance, Vec3 velocity) {
        if (instance.age() >= this.rocketActivationTime && velocity.length() < this.terminalVelocity) {
            Vec3 direction = velocity.normalize();
            if (direction.lengthSqr() < 1e-4d)
                direction = new Vec3(0, 1, 0); // Accelerate upwards as default
            velocity = velocity.add(direction.scale(this.acceleration));
            if (velocity.length() > this.terminalVelocity)
                velocity = velocity.normalize().scale(this.terminalVelocity);
        }
        return super.applyAccelerationToVelocity(level, instance, velocity);
    }

    @Override public RFEProjectileType.Serializer<?> getSerializer() { return BuiltInRFEPlugin.ProjectileTypes.EXPLOSIVE_ROCKET; }

    public static RFERocketPropertiesBuilder makeRocketProperties(RFEExplosiveRocketProjectileType type) {
        RFERocketPropertiesBuilder properties = new RFERocketPropertiesBuilder();
        properties.rocketActivationTime = type.rocketActivationTime;
        properties.acceleration = type.acceleration;
        properties.terminalVelocity = type.terminalVelocity;
        properties.rocketSound = type.rocketSound;
        return properties;
    }

    public static class Serializer implements RFEProjectileType.Serializer<RFEExplosiveRocketProjectileType> {
        private static final MapCodec<RFEExplosiveRocketProjectileType> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEBaseProjectilePropertiesBuilder.CODEC.forGetter(RFEExplosiveRocketProjectileType::makeProjectileProperties),
                RFEExplosiveProjectilePropertiesBuilder.CODEC.forGetter(RFEExplosiveRocketProjectileType::makeExplosiveProperties),
                RFERocketPropertiesBuilder.CODEC.forGetter(RFEExplosiveRocketProjectileType::makeRocketProperties)
        ).apply(o, RFEExplosiveRocketProjectileType::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveRocketProjectileType> STREAM_CODEC = StreamCodec.composite(
                RFEBaseProjectilePropertiesBuilder.STREAM_CODEC, RFEExplosiveRocketProjectileType::makeProjectileProperties,
                RFEExplosiveProjectilePropertiesBuilder.STREAM_CODEC, RFEExplosiveRocketProjectileType::makeExplosiveProperties,
                RFERocketPropertiesBuilder.STREAM_CODEC, RFEExplosiveRocketProjectileType::makeRocketProperties,
                RFEExplosiveRocketProjectileType::new);

        @Override public MapCodec<RFEExplosiveRocketProjectileType> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEExplosiveRocketProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
