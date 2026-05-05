package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.buck_and_ball;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.shotgun.RFEShotgunProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;

import java.util.Map;

public class RFEBuckAndBallProjectileType implements RFEProjectileType, RFEProjectileType.HasCombinedProjectiles {

    private final RFEBulletProjectileType ballProjectileType;
    private final RFEShotgunProjectileType buckProjectileType;

    public RFEBuckAndBallProjectileType(RFEBulletProjectileType ballProjectileType, RFEShotgunProjectileType buckProjectileType) {
        this.ballProjectileType = ballProjectileType;
        this.buckProjectileType = buckProjectileType;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack,
                      LivingEntity entity, RFESpreadInstance spread) {
        instance.setRemoved();

        RFEProjectileInstance ballInstance = this.ballProjectileType.createInstance();
        ballInstance.setPosition(instance.position());
        ballInstance.setOwner(entity);
        this.ballProjectileType.shoot(ballInstance, dx, dy, dz, itemStack, entity, spread);
        RFEProjectileManager.queueAddedProjectile(ballInstance, entity.level());

        RFEProjectileInstance buckInstance = this.buckProjectileType.createInstance();
        buckInstance.setPosition(instance.position());
        buckInstance.setOwner(entity);
        this.buckProjectileType.shoot(buckInstance, dx, dy, dz, itemStack, entity, spread);
    }

    @Override public void tick(Level level, RFEProjectileInstance instance) { instance.setRemoved(); }
    @Override public AABB getAABB(Level level, RFEProjectileInstance instance) { return AABB.ofSize(instance.position(), 0, 0, 0); }

    @Override public RFEProjectileType.Serializer<?> getSerializer() { return BuiltInRFEPlugin.ProjectileTypes.BUCK_AND_BALL; }

    @Override
    public Map<String, RFEProjectileType> getSubprojectileTypes() {
        return Map.of("ball", this.ballProjectileType, "buck", this.buckProjectileType);
    }

    public static class Serializer implements RFEProjectileType.Serializer<RFEBuckAndBallProjectileType> {
        public static final MapCodec<RFEBuckAndBallProjectileType> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEBulletProjectileType.Serializer.CODEC.fieldOf("ball_properties").forGetter(t -> t.ballProjectileType),
                RFEShotgunProjectileType.Serializer.CODEC.fieldOf("buck_properties").forGetter(t -> t.buckProjectileType)
        ).apply(o, RFEBuckAndBallProjectileType::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RFEBuckAndBallProjectileType> STREAM_CODEC = StreamCodec.composite(
                RFEBulletProjectileType.Serializer.STREAM_CODEC, t -> t.ballProjectileType,
                RFEShotgunProjectileType.Serializer.STREAM_CODEC, t -> t.buckProjectileType,
                RFEBuckAndBallProjectileType::new);

        @Override public MapCodec<RFEBuckAndBallProjectileType> codec() { return CODEC; }

        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEBuckAndBallProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
