package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEProjectileDamageModel;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMathUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

public class RFEBulletProjectileType implements RFEProjectileType {

    private final double muzzleVelocity;
    private final double drag;
    private final boolean quadraticDrag;
    private final double gravity;
    private final int maxAge;
    private final float knockback;
    private final RFEProjectileDamageModel damageModel;
    private final ResourceKey<DamageType> damageTypeKey;

    public RFEBulletProjectileType(double muzzleVelocity, double drag, boolean quadraticDrag, double gravity, int maxAge, float knockback,
                                   RFEProjectileDamageModel damageModel, ResourceKey<DamageType> damageTypeKey) {
        this.muzzleVelocity = muzzleVelocity;
        this.drag = drag;
        this.quadraticDrag = quadraticDrag;
        this.gravity = gravity;
        this.maxAge = maxAge;
        this.knockback = knockback;
        this.damageModel = damageModel;
        this.damageTypeKey = damageTypeKey;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack,
                      LivingEntity entity, RFESpreadInstance spread) {
        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, entity.getXRot(), entity.yHeadRot);
        RFEAimAngles spreadAngles = spread.getSpread(itemStack, entity);
        Vec3 finalAimDir = RFEMathUtils.calculateAimVector(aimAngles.pitch() + spreadAngles.pitch(), aimAngles.yaw() + spreadAngles.yaw());
        instance.setVelocity(finalAimDir.normalize().scale(this.muzzleVelocity));
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

        while (!instance.isRemoved()) {
            EntityHitResult entityHitResult = RFEProjectileUtils.getEntityHitResult(level, oldPos, newPos, searchBox, e -> this.canHitEntity(instance, e), 0.1d);
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
        double damage = this.damageModel.getDamage(instance.distanceTravelled() + additionalDisplacement);
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
        if (entity.hurt(damagesource, (float) damage)) {
            if (flag)
                return;
            entity.setDeltaMovement(oldVel);

            if (entity instanceof LivingEntity living) {
                if (this.knockback > 0) {
                    double knockbackScale = Math.max(0d, 1d - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    Vec3 vec3 = instance.velocity().multiply(1d, 0d, 1d).normalize().scale(this.knockback * knockbackScale);
                    if (vec3.lengthSqr() > 0d)
                        living.push(vec3.x, 0.25, vec3.z);
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

    public static class Serializer implements RFEProjectileType.Serializer<RFEBulletProjectileType> {
        @Override
        public RFEBulletProjectileType fromJson(JsonObject obj) {
            double muzzleVelocity = GsonHelper.getAsDouble(obj, "muzzle_velocity");
            double drag = Mth.clamp(GsonHelper.getAsDouble(obj, "drag"), 0, 1);
            boolean quadraticDrag = GsonHelper.getAsBoolean(obj, "quadratic_drag", true);
            double gravity = GsonHelper.getAsDouble(obj, "gravity");
            int maxAge = GsonHelper.getAsInt(obj, "max_age", 400);
            float knockback = Math.max(GsonHelper.getAsFloat(obj, "knockback", 0), 0);

            RFEProjectileDamageModel damageModel = new RFEProjectileDamageModel();
            JsonArray dmgModelArr = GsonHelper.getAsJsonArray(obj, "damage_model");
            for (JsonElement el : dmgModelArr) {
                if (!el.isJsonObject())
                    throw new JsonParseException("Expected JSON object for projectile damage model point");
                JsonObject pointObj = el.getAsJsonObject();
                double distance = GsonHelper.getAsDouble(pointObj, "distance");
                double damage = GsonHelper.getAsDouble(pointObj, "damage");
                damageModel.addPoint(distance, damage);
            }
            damageModel.validateDamageModel();

            ResourceKey<DamageType> damageType = ResourceKey.create(Registries.DAMAGE_TYPE, RFEUtils.location(GsonHelper.getAsString(obj, "damage_type")));

            return new RFEBulletProjectileType(muzzleVelocity, drag, quadraticDrag, gravity, maxAge, knockback, damageModel, damageType);
        }

        @Override
        public RFEBulletProjectileType fromNetwork(FriendlyByteBuf buf) {
            double muzzleVelocity = buf.readDouble();
            double drag = buf.readDouble();
            boolean quadraticDrag = buf.readBoolean();
            double gravity = buf.readDouble();
            int maxAge = buf.readVarInt();
            float knockback = buf.readFloat();
            RFEProjectileDamageModel damageModel = RFEProjectileDamageModel.fromNetwork(buf);
            ResourceKey<DamageType> damageType = buf.readResourceKey(Registries.DAMAGE_TYPE);
            return new RFEBulletProjectileType(muzzleVelocity, drag, quadraticDrag, gravity, maxAge, knockback, damageModel, damageType);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFEBulletProjectileType type) {
            buf.writeDouble(type.muzzleVelocity)
                    .writeDouble(type.drag)
                    .writeBoolean(type.quadraticDrag)
                    .writeDouble(type.gravity);
            buf.writeVarInt(type.maxAge)
                    .writeFloat(type.knockback);
            RFEProjectileDamageModel.toNetwork(buf, type.damageModel);
            buf.writeResourceKey(type.damageTypeKey);
        }
    }

}
