package rbasamoyai.ritchiesfirearmengine.foundation.effects.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.network.RFEExplosionPacket;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class QuietExplosion extends Explosion implements RFECustomExplosion {

    public QuietExplosion(Level level, @Nullable Entity source, @Nullable DamageSource damageSource,
                          @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius,
                          boolean fire, BlockInteraction blockInteraction, ParticleOptions smallExplosionParticles,
                          ParticleOptions largeExplosionParticles) {
        super(level, source, damageSource, damageCalculator, x, y, z, radius, fire, blockInteraction, smallExplosionParticles,
                largeExplosionParticles, SoundEvents.GENERIC_EXPLODE);
    }

    @Override public void playLocalSound(Level level, double x, double y, double z) {}

    @Override
    public void sendToServerPlayer(ServerPlayer player) {
        Vec3 explosionPos = this.center();
        if (player.distanceToSqr(explosionPos.x, explosionPos.y, explosionPos.z) > 4096.0)
            return;
        RFENetwork.sendToPlayer(new ClientboundExplosionPacket(explosionPos.x, explosionPos.y, explosionPos.z,
                this.radius(), this.getToBlow(), this.getHitPlayers().get(player), this.getBlockInteraction(),
                this.getSmallExplosionParticles(), this.getLargeExplosionParticles()), player);
    }

    public record ClientboundExplosionPacket(double x, double y, double z, float radius, List<BlockPos> toBlow,
                                             @Nullable Vec3 knockback, BlockInteraction blockInteraction,
                                             ParticleOptions smallExplosionParticles, ParticleOptions largeExplosionParticles)
            implements RFEExplosionPacket<QuietExplosion> {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundExplosionPacket> STREAM_CODEC = RFEByteBufCodecUtils.composite9(
                ByteBufCodecs.DOUBLE, ClientboundExplosionPacket::x,
                ByteBufCodecs.DOUBLE, ClientboundExplosionPacket::y,
                ByteBufCodecs.DOUBLE, ClientboundExplosionPacket::z,
                ByteBufCodecs.FLOAT, ClientboundExplosionPacket::radius,
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundExplosionPacket::toBlow,
                ByteBufCodecs.optional(RFEByteBufCodecUtils.VEC3_STREAM_CODEC).map(op -> op.orElse(null), Optional::ofNullable), ClientboundExplosionPacket::knockback,
                NeoForgeStreamCodecs.enumCodec(BlockInteraction.class), ClientboundExplosionPacket::blockInteraction,
                ParticleTypes.STREAM_CODEC, ClientboundExplosionPacket::smallExplosionParticles,
                ParticleTypes.STREAM_CODEC, ClientboundExplosionPacket::largeExplosionParticles,
                ClientboundExplosionPacket::new);

        @Override
        public QuietExplosion getExplosion(Level level) {
            return new QuietExplosion(level, null, null, null, this.x, this.y, this.z, this.radius, false, this.blockInteraction,
                    this.smallExplosionParticles, this.largeExplosionParticles);
        }

        @Override public Vec3 getKnockback() { return this.knockback == null ? Vec3.ZERO : this.knockback; }
    }

}
