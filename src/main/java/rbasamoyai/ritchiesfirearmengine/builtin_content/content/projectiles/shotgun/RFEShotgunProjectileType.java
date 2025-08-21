package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.shotgun;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
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

public class RFEShotgunProjectileType extends RFEBulletProjectileType {

    private final int count;
    private final float verticalDispersion;
    private final float horizontalDispersion;
    private final double size;

    public RFEShotgunProjectileType(RFEBaseProjectilePropertiesBuilder builder, int count, float verticalDispersion,
                                    float horizontalDispersion, double size) {
        super(builder);
        this.count = count;
        this.verticalDispersion = verticalDispersion;
        this.horizontalDispersion = horizontalDispersion;
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
        @Override
        public RFEShotgunProjectileType fromJson(JsonObject obj) {
            RFEBaseProjectilePropertiesBuilder builder = RFEBaseProjectilePropertiesBuilder.fromJson(obj);
            int count = GsonHelper.getAsInt(obj, "subprojectile_count");
            if (count < 1)
                throw new IllegalStateException("Cannot have less than 1 shotgun sub-projectile");
            float horizontalDispersion;
            float verticalDispersion;
            if (obj.has("dispersion")) {
                horizontalDispersion = verticalDispersion = GsonHelper.getAsFloat(obj, "dispersion");
            } else {
                horizontalDispersion = GsonHelper.getAsFloat(obj, "horizontal_dispersion");
                verticalDispersion = GsonHelper.getAsFloat(obj, "vertical_dispersion");
            }
            double size = GsonHelper.getAsDouble(obj, "size", 0.05d);
            horizontalDispersion = Math.max(horizontalDispersion, 0);
            verticalDispersion = Math.max(verticalDispersion, 0);
            size = Math.max(size, 0);
            return new RFEShotgunProjectileType(builder, count, horizontalDispersion, verticalDispersion, size);
        }

        @Override
        public RFEShotgunProjectileType fromNetwork(FriendlyByteBuf buf) {
            RFEBaseProjectilePropertiesBuilder builder = RFEBaseProjectilePropertiesBuilder.fromNetwork(buf);
            int count = buf.readVarInt();
            float horizontalDispersion = buf.readFloat();
            float verticalDispersion = buf.readFloat();
            double size = buf.readDouble();
            return new RFEShotgunProjectileType(builder, count, horizontalDispersion, verticalDispersion, size);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFEShotgunProjectileType type) {
            RFEBaseProjectilePropertiesBuilder.toNetwork(buf, RFEBulletProjectileType.makeProjectileProperties(type));
            buf.writeVarInt(type.count)
                    .writeFloat(type.horizontalDispersion)
                    .writeFloat(type.verticalDispersion)
                    .writeDouble(type.size);
        }
    }

}
