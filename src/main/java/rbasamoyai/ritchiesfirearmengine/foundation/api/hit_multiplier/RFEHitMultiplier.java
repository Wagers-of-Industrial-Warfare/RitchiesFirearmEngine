package rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.function.Function;

public interface RFEHitMultiplier {

    float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage);

    Provider getProvider();
    float getMultiplier();

    @FunctionalInterface
    interface Provider extends Function<Float, RFEHitMultiplier> {
    }

}
