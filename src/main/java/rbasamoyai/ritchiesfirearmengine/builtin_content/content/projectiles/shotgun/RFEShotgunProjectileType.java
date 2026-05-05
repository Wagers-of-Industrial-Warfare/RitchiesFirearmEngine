package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.shotgun;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEBaseProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMathUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;

import java.util.Objects;

public class RFEShotgunProjectileType extends RFEBulletProjectileType {

    private final int count;
    private final float horizontalDispersion;
    private final float verticalDispersion;
    private final double size;

    public RFEShotgunProjectileType(RFEBaseProjectilePropertiesBuilder builder, int count, float horizontalDispersion,
                                    float verticalDispersion, double size) {
        super(builder);
        this.count = count;
        this.horizontalDispersion = horizontalDispersion;
        this.verticalDispersion = verticalDispersion;
        this.size = size;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack,
                      LivingEntity entity, RFESpreadInstance spread) {
        instance.setRemoved();

        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, entity.getXRot(), entity.yHeadRot);
        RFEAimAngles spreadAngles = spread.getSpread(itemStack, entity);
        float basePitch = aimAngles.pitch() + spreadAngles.pitch();
        float baseYaw = aimAngles.yaw() + spreadAngles.yaw();
        Vec3 sourcePos = instance.getPosition(1);
        RandomSource random = entity.getRandom();
        Level level = entity.level();

        for (int i = 0; i < this.count; ++i) {
            RFEAimAngles dispersion = RFEProjectileUtils.standardSpreadAngles(this.horizontalDispersion, this.verticalDispersion, false, entity.getRandom());
            Vec3 finalShootDir = RFEMathUtils.calculateAimVector(basePitch + dispersion.pitch(), baseYaw + dispersion.yaw()).normalize();
            RFEProjectileInstance actualInstance = this.createInstance();
            actualInstance.setOwner(entity);
            double randomPosOffset = 0.1d + 0.05d * random.nextDouble();
            actualInstance.setPosition(sourcePos.add(finalShootDir.scale(randomPosOffset)));
            actualInstance.setVelocity(finalShootDir.scale(this.muzzleVelocity));
            this.tick(entity.level(), actualInstance);
            if (this.fullHitscan) {
                actualInstance.setRemoved();
            } else {
                RFEProjectileManager.queueAddedProjectile(actualInstance, level);
            }
        }
    }

    @Override protected double getHitboxInflation(Level level, RFEProjectileInstance instance, double distance) { return this.size; }

    @Override public RFEProjectileType.Serializer<?> getSerializer() { return BuiltInRFEPlugin.ProjectileTypes.SHOTGUN; }

    public static class Serializer implements RFEProjectileType.Serializer<RFEShotgunProjectileType> {
        private static final MapCodec<Pair<Float, Float>> HORIZONTAL_VERTICAL_DISPERSION_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("horizontal_dispersion").forGetter(Pair::getFirst),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("vertical_dispersion").forGetter(Pair::getSecond)
        ).apply(o, Pair::new));

        private static final MapCodec<Pair<Float, Float>> DISPERSION_CODEC = Codec.mapEither(
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("dispersion"), HORIZONTAL_VERTICAL_DISPERSION_CODEC)
                .xmap(either -> Either.unwrap(either.mapLeft(d -> new Pair<>(d, d))),
                        pair -> Objects.equals(pair.getFirst(), pair.getSecond()) ? Either.left(pair.getFirst()) : Either.right(pair));

        public static final MapCodec<RFEShotgunProjectileType> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEBaseProjectilePropertiesBuilder.CODEC.forGetter(RFEShotgunProjectileType::makeProjectileProperties),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("subprojectile_count").forGetter(type -> type.count),
                DISPERSION_CODEC.forGetter(type -> new Pair<>(type.horizontalDispersion, type.verticalDispersion)),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("size", 0.05d).forGetter(type -> type.size)
        ).apply(o, (prop, count, disp, size) -> new RFEShotgunProjectileType(prop, count, disp.getFirst(), disp.getSecond(), size)));

        public static final StreamCodec<RegistryFriendlyByteBuf, RFEShotgunProjectileType> STREAM_CODEC = StreamCodec.composite(
                RFEBaseProjectilePropertiesBuilder.STREAM_CODEC, RFEShotgunProjectileType::makeProjectileProperties,
                ByteBufCodecs.VAR_INT, type -> type.count,
                ByteBufCodecs.FLOAT, type -> type.horizontalDispersion,
                ByteBufCodecs.FLOAT, type -> type.verticalDispersion,
                ByteBufCodecs.DOUBLE, type -> type.size,
                RFEShotgunProjectileType::new);

        @Override public MapCodec<RFEShotgunProjectileType> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEShotgunProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
