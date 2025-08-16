package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.buck_and_ball;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
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
        @Override
        public RFEBuckAndBallProjectileType fromJson(JsonObject obj) {
            RFEBulletProjectileType ballType = BuiltInRFEPlugin.ProjectileTypes.BULLET.fromJson(GsonHelper.getAsJsonObject(obj, "ball_properties"));
            RFEShotgunProjectileType buckType = BuiltInRFEPlugin.ProjectileTypes.SHOTGUN.fromJson(GsonHelper.getAsJsonObject(obj, "buck_properties"));
            return new RFEBuckAndBallProjectileType(ballType, buckType);
        }

        @Override
        public RFEBuckAndBallProjectileType fromNetwork(FriendlyByteBuf buf) {
            RFEBulletProjectileType ballType = BuiltInRFEPlugin.ProjectileTypes.BULLET.fromNetwork(buf);
            RFEShotgunProjectileType buckType = BuiltInRFEPlugin.ProjectileTypes.SHOTGUN.fromNetwork(buf);
            return new RFEBuckAndBallProjectileType(ballType, buckType);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFEBuckAndBallProjectileType type) {
            BuiltInRFEPlugin.ProjectileTypes.BULLET.toNetwork(buf, type.ballProjectileType);
            BuiltInRFEPlugin.ProjectileTypes.SHOTGUN.toNetwork(buf, type.buckProjectileType);
        }
    }

}
