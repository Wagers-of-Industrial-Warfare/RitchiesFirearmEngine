package rbasamoyai.ritchiesfirearmengine.content.projectiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import rbasamoyai.ritchiesfirearmengine.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;

public class RFEBulletProjectileType implements RFEProjectileType {

    private final double muzzleVelocity;
    private final double drag;
    private final double gravity;
    private final boolean ignoresInvulnerability;
    private final boolean rendersInvulnerable;
    private final RFEProjectileDamageModel damageModel;

    public RFEBulletProjectileType(double muzzleVelocity, double drag, double gravity, boolean ignoresInvulnerability,
                                   boolean rendersInvulnerable, RFEProjectileDamageModel damageModel) {
        this.muzzleVelocity = muzzleVelocity;
        this.drag = drag;
        this.gravity = gravity;
        this.ignoresInvulnerability = ignoresInvulnerability;
        this.rendersInvulnerable = rendersInvulnerable;
        this.damageModel = damageModel;
    }

    @Override
    public void tick(Level level, RFEProjectileInstance instance) {
        Vec3 oldPos = instance.position();
        Vec3 velocity = instance.velocity();
        Vec3 newPos = oldPos.add(velocity);

        if (!level.hasChunkAt(new BlockPos((int) newPos.x, (int) newPos.y   , (int) newPos.z))) {
            instance.setRemoved();
            return;
        }

        HitResult hitResult = level.clip(new ClipContext(oldPos, newPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hitResult.getType() != HitResult.Type.MISS)
            newPos = hitResult.getLocation();
        AABB searchBox = this.getBoundingBox(oldPos).expandTowards(velocity).inflate(1.0d);

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
        instance.setPosition(newPos);
        instance.setDistanceTravelled(instance.distanceTravelled() + newPos.subtract(oldPos).length());
        // TODO handle velocity when collision

        instance.setVelocity(newVelocity.scale(1 - this.drag).add(0, -this.gravity, 0));
        // TODO effects
    }

    public AABB getBoundingBox(Vec3 center) {
        return AABB.ofSize(center, 0, 0, 0);
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
        double damage = this.damageModel.getDamage(instance.distanceTravelled());
        // TODO crits and damage multipliers
        // TODO overpenetration

        // TODO custom damage source
        Entity owner = instance.getOwner();
        DamageSource damagesource;
        if (owner == null) {
            damagesource = level.damageSources().generic();
        } else {
            damagesource = level.damageSources().generic();
            if (owner instanceof LivingEntity livingOwner)
                livingOwner.setLastHurtMob(entity);
        }

        boolean flag = entity.getType() == EntityType.ENDERMAN;

        if (entity.hurt(damagesource, (float) damage)) {
            if (flag)
                return;

            if (entity instanceof LivingEntity living) {
                // TODO knockback
//                if (this.knockback > 0) {
//                    double d0 = Math.max(0.0D, 1.0D - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
//                    Vec3 vec3 = instance.velocity().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)this.knockback * 0.6D * d0);
//                    if (vec3.lengthSqr() > 0.0D) {
//                        livingentity.push(vec3.x, 0.1D, vec3.z);
//                    }
//                }

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
            double drag = GsonHelper.getAsDouble(obj, "drag");
            double gravity = GsonHelper.getAsDouble(obj, "gravity");
            boolean ignoresInvulnerability = GsonHelper.getAsBoolean(obj, "ignores_invulnerabilty");
            boolean rendersInvulnerable = GsonHelper.getAsBoolean(obj, "renders_invulnerable");

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

            return new RFEBulletProjectileType(muzzleVelocity, drag, gravity, ignoresInvulnerability, rendersInvulnerable, damageModel);
        }

        @Override
        public RFEBulletProjectileType fromNetwork(FriendlyByteBuf buf) {
            double muzzleVelocity = buf.readDouble();
            double drag = buf.readDouble();
            double gravity = buf.readDouble();
            boolean ignoresInvulnerability = buf.readBoolean();
            boolean rendersInvulnerable = buf.readBoolean();
            RFEProjectileDamageModel damageModel = RFEProjectileDamageModel.fromNetwork(buf);
            return new RFEBulletProjectileType(muzzleVelocity, drag, gravity, ignoresInvulnerability, rendersInvulnerable, damageModel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFEBulletProjectileType type) {
            buf.writeDouble(type.muzzleVelocity)
                    .writeDouble(type.drag)
                    .writeDouble(type.gravity)
                    .writeBoolean(type.ignoresInvulnerability)
                    .writeBoolean(type.rendersInvulnerable);
            RFEProjectileDamageModel.toNetwork(buf, type.damageModel);
        }
    }

}
