package rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.armor_piercing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

public record ArmorPiercingHitMultiplier(float multiplier) implements RFEHitMultiplier {

    @Override
    public float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage) {
        if (target instanceof LivingEntity living && living.getArmorValue() > 0)
            damage *= this.multiplier;
        return damage;
    }

    @Override public RFEHitMultiplier.Provider getProvider() { return BuiltInRFEPlugin.HitMultipliers.ARMOR_PIERCING; }
    @Override public float getMultiplier() { return this.multiplier; }

}
