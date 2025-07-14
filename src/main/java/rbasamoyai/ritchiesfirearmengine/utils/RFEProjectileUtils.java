package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEEntityHitResult;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class RFEProjectileUtils {

    @Nullable
    public static RFEEntityHitResult getEntityHitResult(Level level, Vec3 start, Vec3 end, AABB boundingBox,
                                                        Predicate<Entity> filter, double inflation) {
        double d0 = Double.MAX_VALUE;
        Entity entity = null;
        Vec3 pos = null;

        for (Entity entity1 : level.getEntities((Entity) null, boundingBox, filter)) {
            AABB aabb = entity1.getBoundingBox().inflate(inflation);
            Optional<Vec3> optional = aabb.clip(start, end);
            if (optional.isEmpty())
                continue;
            Vec3 pos1 = optional.get();
            double d1 = start.distanceToSqr(pos1);
            if (d1 > d0)
                continue;
            entity = entity1;
            pos = pos1;
            d0 = d1;
        }

        return entity == null ? null : new RFEEntityHitResult(entity, pos);
    }

    /**
     *
     * @param radius radius of spread, in degrees
     * @param tighten if the spread should be concentrated in the center
     * @param random random source for generating spread
     * @return the aim deviation, in degrees; left entry is pitch, right entry is yaw
     */
    public static Tuple<Float, Float> standardSpreadAngles(float radius, boolean tighten, RandomSource random) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        float radMul = tighten ? random.nextFloat() : Mth.sqrt(random.nextFloat()); // Taken from sym.gg
        radMul *= radius;
        return new Tuple<>(radMul * Mth.sin(angle), radMul * Mth.cos(angle));
    }

}
