package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.RFEHitMultiplierHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEBaseProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEProjectileDamageModel;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMathUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;

public class RFEBulletProjectileType implements RFEProjectileType {

    protected final boolean fullHitscan;
    protected final double muzzleVelocity;
    protected final double drag;
    protected final boolean quadraticDrag;
    protected final double gravity;
    protected final int maxAge;
    protected final float knockback;
    protected final RFEProjectileDamageModel damageModel;
    protected final ResourceKey<DamageType> damageTypeKey;

    public RFEBulletProjectileType(RFEBaseProjectilePropertiesBuilder builder) {
        this.fullHitscan = builder.fullHitscan;
        this.muzzleVelocity = builder.muzzleVelocity;
        this.drag = builder.drag;
        this.quadraticDrag = builder.quadraticDrag;
        this.gravity = builder.gravity;
        this.maxAge = builder.maxAge;
        this.knockback = builder.knockback;
        this.damageModel = builder.damageModel;
        this.damageTypeKey = builder.damageTypeKey;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack,
                      LivingEntity entity, RFESpreadInstance spread) {
        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, entity.getXRot(), entity.yHeadRot);
        RFEAimAngles spreadAngles = spread.getSpread(itemStack, entity);
        Vec3 finalAimDir = RFEMathUtils.calculateAimVector(aimAngles.pitch() + spreadAngles.pitch(), aimAngles.yaw() + spreadAngles.yaw());
        instance.setVelocity(finalAimDir.normalize().scale(this.muzzleVelocity));
        this.tick(entity.level(), instance);
        if (this.fullHitscan)
            instance.setRemoved();
    }

    @Override
    public void tick(Level level, RFEProjectileInstance instance) {
        Vec3 oldPos = instance.position();
        Vec3 velocity = instance.velocity();
        Vec3 newPos = oldPos.add(velocity);

        if (!level.hasChunkAt(new BlockPos((int) newPos.x, (int) newPos.y, (int) newPos.z))) {
            instance.setRemoved();
            return;
        }

        HitResult hitResult = level.clip(new ClipContext(oldPos, newPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hitResult.getType() != HitResult.Type.MISS)
            newPos = hitResult.getLocation();
        AABB searchBox = this.getAABB(level, instance).expandTowards(velocity).inflate(1.0d);

        double hitboxInflation = this.getHitboxInflation(level, instance);
        while (!instance.isRemoved()) {
            EntityHitResult entityHitResult = RFEProjectileUtils.getEntityHitResult(level, oldPos, newPos, searchBox, e -> this.canHitEntity(instance, e), hitboxInflation);
            if (entityHitResult != null)
                hitResult = entityHitResult;

            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                Entity hitEntity = ((EntityHitResult) hitResult).getEntity();
                Entity owner = instance.getOwner();
                if (hitEntity instanceof Player hitPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(hitPlayer)) {
                    hitResult = null;
                    entityHitResult = null;
                }
            }

            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(instance, level, hitResult);
            }

            // TODO overpenetration
            boolean canContinuePenetrating = false;
            if (entityHitResult == null || !canContinuePenetrating)
                break;
            hitResult = null;
        }

        Vec3 newVelocity = instance.velocity();
        instance.setOldPosition(oldPos);
        instance.setPosition(newPos);
        instance.setDistanceTravelled(instance.distanceTravelled() + newPos.subtract(oldPos).length());
        // TODO handle velocity when collision

        if (this.quadraticDrag) {
            double dragMag = this.drag * newVelocity.lengthSqr();
            newVelocity = newVelocity.scale(Math.max(1 - dragMag / newVelocity.length(), 0));
        } else {
            newVelocity = newVelocity.scale(Math.max(1 - this.drag, 0));
        }
        instance.setVelocity(newVelocity.add(0, -this.gravity, 0));
        // TODO effects

        if (instance.age() > this.maxAge)
            instance.setRemoved();
    }

    @Override
    public AABB getAABB(Level level, RFEProjectileInstance instance) {
        return AABB.ofSize(instance.position(), 0, 0, 0);
    }

    protected double getHitboxInflation(Level level, RFEProjectileInstance instance) { return 0.1d; }

    protected boolean canHitEntity(RFEProjectileInstance instance, Entity target) {
        if (!target.canBeHitByProjectile()) {
            return false;
        } else {
            Entity entity = instance.getOwner();
            return entity == null || instance.leftOwner() || !entity.isPassengerOfSameVehicle(target);
        }
        // TODO overpenetration
        //return (this.piercingIgnoreEntityIds == null || !this.piercingIgnoreEntityIds.contains(target.getId())) && !this.ignoredEntities.contains(target.getId());
    }

    protected void onHit(RFEProjectileInstance instance, Level level, HitResult hitResult) {
        HitResult.Type type = hitResult.getType();
        if (type == HitResult.Type.ENTITY) {
            this.onHitEntity(instance, level, (EntityHitResult) hitResult);
            level.gameEvent(GameEvent.PROJECTILE_LAND, hitResult.getLocation(), GameEvent.Context.of(null,null));
        } else if (type == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            this.onHitBlock(instance, level, blockHitResult);
            BlockPos blockpos = blockHitResult.getBlockPos();
            level.gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(null, level.getBlockState(blockpos)));
        }
    }

    protected void onHitEntity(RFEProjectileInstance instance, Level level, EntityHitResult result) {
        Entity entity = result.getEntity();
        float speed = (float) instance.velocity().length();
        double additionalDisplacement = result.getLocation().subtract(instance.position()).length();

        float damage = (float) this.damageModel.getDamage(instance.distanceTravelled() + additionalDisplacement);
        for (RFEHitMultiplier mul : RFEHitMultiplierHandler.getHitMultipliers(this))
            damage = mul.multiplyDamage(entity, instance, result, damage);

        // TODO crits and damage multipliers
        // TODO overpenetration

        Entity owner = instance.getOwner();
        DamageSource damagesource;
        Registry<DamageType> reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        if (owner instanceof LivingEntity livingOwner) {
            damagesource = reg.getHolder(this.damageTypeKey)
                    .map(type -> new DamageSource(type, null, livingOwner))
                    .orElse(level.damageSources().mobAttack(livingOwner));
        } else {
            damagesource = level.damageSources().generic();
        }

        boolean flag = entity.getType() == EntityType.ENDERMAN;

        Vec3 oldVel = entity.getDeltaMovement();
        if (entity.hurt(damagesource, damage)) {
            if (flag)
                return;
            entity.setDeltaMovement(oldVel);

            if (entity instanceof LivingEntity living) {
                if (this.knockback > 0) {
                    double knockbackScale = Math.max(0d, 1d - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    Vec3 vec3 = instance.velocity().multiply(1d, 0d, 1d).normalize().scale(this.knockback * knockbackScale);
                    double verticalVel = oldVel.y < 0.05d ? 0.25 : 0;
                    if (vec3.lengthSqr() > 0d)
                        living.push(vec3.x, verticalVel, vec3.z);
                }

                if (!level.isClientSide && owner instanceof LivingEntity ownerLiving) {
                    EnchantmentHelper.doPostHurtEffects(living, owner);
                    EnchantmentHelper.doPostDamageEffects(ownerLiving, living);
                }

                this.doPostHurtEffects(instance, level, living);
                // TODO custom on hit sound effect for players
//                if (living != owner && living instanceof Player && owner instanceof ServerPlayer splayer && !this.isSilent()) {
//                    ((ServerPlayer)owner).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
//                }

                // TODO overpenetration
//                if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
//                    this.piercedAndKilledEntities.add(livingentity);
//                }

                // TODO stats tracking
            }

            // TODO hit effects
//            this.playSound(this.soundEvent, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

            // TODO overpenetration
            instance.setRemoved();
//            if (this.getPierceLevel() <= 0) {
//                this.discard();
//            }
        } else {
            // TODO entity ricochet if warranted
            instance.setRemoved();
        }

        // TODO syncing removal such that the bullet travels a bit more nicely
    }

    protected void doPostHurtEffects(RFEProjectileInstance instance, Level level, LivingEntity entity) {
    }

    protected void onHitBlock(RFEProjectileInstance instance, Level level, BlockHitResult pResult) {
        // TODO ricochet
        // TODO block breaking

        BlockState blockstate = level.getBlockState(pResult.getBlockPos());
        //this.lastState = blockstate;
        //blockstate.onProjectileHit(level, blockstate, pResult, this); TODO fake projectile
        Vec3 projPos = instance.position();
        Vec3 terminalVel = pResult.getLocation().subtract(projPos.x, projPos.y, projPos.z);
        instance.setVelocity(terminalVel);
        instance.setRemoved();
        instance.setForceSync(true);

        // TODO hit effects
        //this.playSound(this.getHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        //this.inGround = true;
        //this.shakeTime = 7;
        //this.setCritArrow(false);
        //this.setPierceLevel((byte)0);
        //this.setSoundEvent(SoundEvents.ARROW_HIT);
        //this.setShotFromCrossbow(false);
        //this.resetPiercedEntities();
    }

    @Override
    public RFEProjectileType.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.ProjectileTypes.BULLET;
    }
    
    public static RFEBaseProjectilePropertiesBuilder makeProjectileProperties(RFEBulletProjectileType type) {
        RFEBaseProjectilePropertiesBuilder builder = new RFEBaseProjectilePropertiesBuilder();
        builder.fullHitscan = type.fullHitscan;
        builder.muzzleVelocity = type.muzzleVelocity;
        builder.drag = type.drag;
        builder.quadraticDrag = type.quadraticDrag;
        builder.gravity = type.gravity;
        builder.maxAge = type.maxAge;
        builder.knockback = type.knockback;
        builder.damageModel = type.damageModel;
        builder.damageTypeKey = type.damageTypeKey;
        return builder;
    }

    public static class Serializer implements RFEProjectileType.Serializer<RFEBulletProjectileType> {
        @Override
        public RFEBulletProjectileType fromJson(JsonObject obj) {
            return new RFEBulletProjectileType(RFEBaseProjectilePropertiesBuilder.fromJson(obj));
        }

        @Override
        public RFEBulletProjectileType fromNetwork(FriendlyByteBuf buf) {
            return new RFEBulletProjectileType(RFEBaseProjectilePropertiesBuilder.fromNetwork(buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFEBulletProjectileType type) {
            RFEBaseProjectilePropertiesBuilder.toNetwork(buf, makeProjectileProperties(type));
        }
    }

}
