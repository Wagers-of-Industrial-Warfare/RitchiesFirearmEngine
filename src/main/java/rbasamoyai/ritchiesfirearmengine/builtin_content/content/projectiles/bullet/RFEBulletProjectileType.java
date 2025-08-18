package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.RFEItemLengths;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles.BlackPowderSmokeOptions;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEBaseProjectilePropertiesBuilder;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEProjectileClipContext;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEProjectileDamageModel;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.utils.RFEMathUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEProjectileUtils;

import javax.annotation.Nullable;
import java.util.*;

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
    @Nullable protected final ResourceLocation hitMultiplierId;
    @Nullable protected final ResourceLocation penetrationId;
    protected final float smoke;
    @Nullable protected final SoundEvent passSound;

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
        this.hitMultiplierId = builder.hitMultiplierId;
        this.penetrationId = builder.penetrationId;
        this.smoke = builder.smoke;
        this.passSound = builder.passSound;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack,
                      LivingEntity entity, RFESpreadInstance spread) {
        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, entity.getXRot(), entity.yHeadRot);
        RFEAimAngles spreadAngles = spread.getSpread(itemStack, entity);
        Vec3 finalAimDir = RFEMathUtils.calculateAimVector(aimAngles.pitch() + spreadAngles.pitch(), aimAngles.yaw() + spreadAngles.yaw());
        Vec3 spawnPos = instance.getPosition(1);
        instance.setVelocity(finalAimDir.normalize().scale(this.muzzleVelocity));
        this.tick(entity.level(), instance);
        if (this.fullHitscan)
            instance.setRemoved();

        if (this.smoke > 0 && entity.level() instanceof ServerLevel slevel) {
            RandomSource random = entity.getRandom();
            Vec3 smokePos = spawnPos.add(aimDir.scale(RFEItemLengths.getItemLength(itemStack, entity)));
            double speed = Math.sqrt(this.smoke);
            double spawnDispersion = Math.max(this.smoke * 0.15, 1);
            ParticleOptions option = new BlackPowderSmokeOptions(this.smoke);
            for (int i = 0; i < 10; ++i) {
                double sx = smokePos.x + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sy = smokePos.y + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sz = smokePos.z + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                Vec3 smokeVelocity = RFEMathUtils.calculateAimVector(aimAngles.pitch() + (random.nextFloat() - random.nextFloat()) * 30f,
                        aimAngles.yaw() + (random.nextFloat() - random.nextFloat()) * 30f);
                double pdx = smokeVelocity.x * speed * (0.9 * 0.1 * random.nextDouble());
                double pdy = smokeVelocity.y * speed * (0.9 * 0.1 * random.nextDouble());
                double pdz = smokeVelocity.z * speed * (0.9 * 0.1 * random.nextDouble());
                for (ServerPlayer splayer : slevel.players())
                    slevel.sendParticles(splayer, option, true, sx, sy, sz, 0, pdx, pdy, pdz, 1);
            }
        }
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

        Vec3 diff = newPos.subtract(oldPos);
        double length = diff.length();
        double rem = length % 8d;
        int iterations = Math.max(Mth.ceil(length / 8d), 1);
        Vec3 wholeDiff = diff.normalize().scale(8);
        Vec3 remDiff = diff.normalize().scale(rem);
        Vec3 totalDiff = Vec3.ZERO;

        Vec3 rootPos = oldPos;

        AABB baseBox = this.getAABB(level, instance);
        double hitboxInflation = this.getHitboxInflation(level, instance);
        double suppressionInflation = hitboxInflation + 3;

        for (int i = 0; i < iterations; ++i) {
            Vec3 nextDiff = i == iterations - 1 ? remDiff : wholeDiff;
            AABB searchBox = baseBox.move(totalDiff).expandTowards(nextDiff).inflate(1.0d);
            Vec3 nextRoot = rootPos.add(nextDiff);
            Vec3 endPos = nextRoot;

            RFEProjectileClipContext context = new RFEProjectileClipContext(this, instance, rootPos, endPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, level.random);
            HitResult hitResult = level.clip(context);
            if (hitResult.getType() != HitResult.Type.MISS)
                endPos = hitResult.getLocation();

            Set<Entity> hitEntities = new HashSet<>();
            while (!instance.isRemoved()) {
                EntityHitResult entityHitResult = RFEProjectileUtils.getEntityHitResult(level, rootPos, endPos, searchBox,
                        e -> this.canHitEntity(instance, e), hitboxInflation);
                if (entityHitResult != null)
                    hitResult = entityHitResult;

                if (entityHitResult != null) {
                    Entity hitEntity = entityHitResult.getEntity();
                    Entity owner = instance.getOwner();
                    if (hitEntity instanceof Player hitPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(hitPlayer)) {
                        instance.ignoreEntity(hitEntity);
                        hitResult = null;
                        entityHitResult = null;
                    }
                }
                if (entityHitResult != null)
                    hitEntities.add(entityHitResult.getEntity());

                if (hitResult != null && hitResult.getType() != HitResult.Type.MISS)
                    this.onHit(instance, level, hitResult, context.penetratedBlocks);
                this.onPenetratedHitBlocks(instance, level, context.penetratedBlocks);

                boolean canContinuePenetrating = instance.health() > 0;
                if (entityHitResult == null || !canContinuePenetrating)
                    break;
                hitResult = null;
            }

            List<Entity> entities = level.getEntities(instance.getOwner(), searchBox.inflate(suppressionInflation), e -> true);
            for (Entity entity1 : entities) {
                if (hitEntities.contains(entity1))
                    continue;
                AABB aabb = entity1.getBoundingBox().inflate(suppressionInflation);
                Optional<Vec3> optional = aabb.clip(rootPos, endPos);
                if (aabb.contains(rootPos) || optional.isPresent()) {
                    Vec3 passPos = optional.orElse(rootPos);
                    double alignment = entity1.getEyePosition().subtract(rootPos).normalize().dot(velocity.normalize());
                    boolean passBy = 0.15 < alignment && alignment < 0.995;
                    if (passBy && this.passSound != null && !level.isClientSide && entity1 instanceof ServerPlayer splayer) {
                        splayer.connection.send(new ClientboundSoundPacket(Holder.direct(this.passSound), SoundSource.NEUTRAL,
                                passPos.x, passPos.y, passPos.z, 1, 1, 42L));
                    }
                }
            }

            if (instance.health() <= 0) {
                newPos = endPos;
                break;
            }

            rootPos = nextRoot;
            totalDiff = totalDiff.add(nextDiff);
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

        if (instance.age() > this.maxAge || instance.health() <= 0f)
            instance.setRemoved();
    }

    @Override
    public AABB getAABB(Level level, RFEProjectileInstance instance) {
        return AABB.ofSize(instance.position(), 0, 0, 0);
    }

    protected double getHitboxInflation(Level level, RFEProjectileInstance instance) {
        return instance.age() == 0 ? 0 : 0.1d;
    }

    protected boolean canHitEntity(RFEProjectileInstance instance, Entity target) {
        if (!target.canBeHitByProjectile() || this.canIgnoreEntity(instance, target)) {
            return false;
        } else {
            Entity entity = instance.getOwner();
            return entity == null || instance.leftOwner() || !entity.isPassengerOfSameVehicle(target);
        }
    }

    protected boolean canIgnoreEntity(RFEProjectileInstance instance, Entity target) { return instance.canIgnoreEntity(target); }

    protected void onHit(RFEProjectileInstance instance, Level level, HitResult hitResult, Map<BlockPos, BlockState> penetratedBlocks) {
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
        for (RFEHitMultiplier mul : RFEHitMultiplierHandler.getHitMultipliers(this.getHitMultiplierId()))
            damage = mul.multiplyDamage(entity, instance, result, damage);

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
            instance.ignoreEntity(entity);

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
            }

            // TODO hit effects
//            this.playSound(this.soundEvent, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

            RFEProjectilePenetrationProperties penetrationProperties = this.getPenetrationProperties();
            RFEProjectilePenetrationProperties.PenetrationStats entityPenetration = penetrationProperties.getEntityPenetrationStats(entity);
            if (level.random.nextFloat() > entityPenetration.chance())
                instance.setRemoved();
            instance.setHealth(instance.health() - entityPenetration.bulletDamage());

            if (instance.health() <= 0)
                instance.setRemoved();
        } else {
            // TODO entity ricochet if warranted
            instance.setRemoved();
        }
    }

    protected ResourceLocation getHitMultiplierId() {
        return this.hitMultiplierId != null ? this.hitMultiplierId : RFEProjectileTypeHandler.getProjectileTypeId(this);
    }

    protected void doPostHurtEffects(RFEProjectileInstance instance, Level level, LivingEntity entity) {
    }

    protected void onPenetratedHitBlocks(RFEProjectileInstance instance, Level level, Map<BlockPos, BlockState> penetratedBlocks) {
        RFEProjectilePenetrationProperties penetrationProperties = this.getPenetrationProperties();
        for (Map.Entry<BlockPos, BlockState> entry : penetratedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            RFEProjectilePenetrationProperties.PenetrationStats blockBreaking = penetrationProperties.getBlockBreakingStats(state);
            if (instance.health() >= blockBreaking.bulletDamage() && level.random.nextFloat() < blockBreaking.chance()) {
                level.destroyBlock(pos, true, instance.getOwner());
                instance.setHealth(instance.health() - blockBreaking.bulletDamage());
            } else {
                SoundType soundType = state.getSoundType();
                level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS, 1.0F, 1.2F / (level.random.nextFloat() * 0.2F + 0.9F));
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 center = pos.getCenter();
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 16, 0.25, 0.25, 0.25, 0);
                }
            }
        }
    }

    protected void onHitBlock(RFEProjectileInstance instance, Level level, BlockHitResult pResult) {
        // TODO ricochet
        // TODO block breaking

        BlockPos hitPos = pResult.getBlockPos();
        BlockState blockstate = level.getBlockState(hitPos);

        //this.lastState = blockstate;
        //blockstate.onProjectileHit(level, blockstate, pResult, this); TODO fake projectile
        Vec3 projPos = instance.position();
        Vec3 terminalVel = pResult.getLocation().subtract(projPos.x, projPos.y, projPos.z);
        instance.setVelocity(terminalVel);
        instance.setRemoved();
        instance.setForceSync(true);

        RFEProjectilePenetrationProperties penetrationProperties = this.getPenetrationProperties();
        RFEProjectilePenetrationProperties.PenetrationStats blockBreaking = penetrationProperties.getBlockBreakingStats(blockstate);
        if (instance.health() >= blockBreaking.bulletDamage() && level.random.nextFloat() < blockBreaking.chance()) {
            level.destroyBlock(hitPos, true, instance.getOwner());
            return;
        }

        // TODO hit effects
        Vec3 hitLoc = pResult.getLocation();
        SoundType soundType = blockstate.getSoundType();
        level.playSound(null, hitPos, soundType.getHitSound(), SoundSource.BLOCKS, 1.0F, 1.2F / (level.random.nextFloat() * 0.2F + 0.9F));
        if (level instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), hitLoc.x, hitLoc.y, hitLoc.z, 8, 0, 0, 0, 0);

        //this.shakeTime = 7;
        //this.setPierceLevel((byte)0);
        //this.setSoundEvent(SoundEvents.ARROW_HIT);
        //this.setShotFromCrossbow(false);
        //this.resetPiercedEntities();
    }

    public RFEProjectilePenetrationProperties getPenetrationProperties() {
        ResourceLocation id = this.penetrationId != null ? this.penetrationId : RFEProjectileTypeHandler.getProjectileTypeId(this);
        return RFEProjectilePenetrationHandler.getPenetrationProperties(id);
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
        builder.hitMultiplierId = type.hitMultiplierId;
        builder.penetrationId = type.penetrationId;
        builder.smoke = type.smoke;
        builder.passSound = type.passSound;
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
