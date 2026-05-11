package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
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

    public RFEBulletProjectileType(RFEBaseProjectilePropertiesBuilder baseProperties) {
        this.fullHitscan = baseProperties.fullHitscan;
        this.muzzleVelocity = baseProperties.muzzleVelocity;
        this.drag = baseProperties.drag;
        this.quadraticDrag = baseProperties.quadraticDrag;
        this.gravity = baseProperties.gravity;
        this.maxAge = baseProperties.maxAge;
        this.knockback = baseProperties.knockback;
        this.damageModel = baseProperties.damageModel;
        this.damageTypeKey = baseProperties.damageTypeKey;
        this.hitMultiplierId = baseProperties.hitMultiplierId;
        this.penetrationId = baseProperties.penetrationId;
        this.smoke = baseProperties.smoke;
        this.passSound = baseProperties.passSound;
    }

    @Override
    public void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, float pitchAdjustment,
                      ItemStack itemStack, LivingEntity entity, RFESpreadInstance spread) {
        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, entity.getXRot(), entity.yHeadRot);
        aimAngles = new RFEAimAngles(aimAngles.pitch() - pitchAdjustment, aimAngles.yaw());
        RFEAimAngles spreadAngles = spread.getSpread(itemStack, entity);
        Vec3 finalAimDir = RFEMathUtils.calculateAimVector(aimAngles.pitch() + spreadAngles.pitch(), aimAngles.yaw() + spreadAngles.yaw());
        Vec3 spawnPos = instance.getPosition(1);
        instance.setVelocity(finalAimDir.normalize().scale(this.muzzleVelocity));
        this.tick(entity.level(), instance);
        if (this.fullHitscan)
            instance.setRemoved();

        if (this.smoke > 0 && entity.level() instanceof ServerLevel slevel) {
            RandomSource random = entity.getRandom();
            float itemLength = RFEItemLengths.getItemLength(itemStack, entity);
            RFEAimAngles smokeAimAngles = aimAngles;
            if (itemLength < 0)
                smokeAimAngles = new RFEAimAngles(-smokeAimAngles.pitch(), smokeAimAngles.yaw() + 180f);
            Vec3 smokePos = spawnPos.add(aimDir.scale(itemLength));
            double speed = Math.sqrt(this.smoke);
            double spawnDispersion = Math.min(this.smoke * 0.15, 1);
            ParticleOptions option = new BlackPowderSmokeOptions(this.smoke);
            for (int i = 0; i < 10; ++i) {
                double sx = smokePos.x + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sy = smokePos.y + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sz = smokePos.z + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                Vec3 smokeVelocity = RFEMathUtils.calculateAimVector(smokeAimAngles.pitch() + (random.nextFloat() - random.nextFloat()) * 30f,
                        smokeAimAngles.yaw() + (random.nextFloat() - random.nextFloat()) * 30f);
                double pdx = smokeVelocity.x * speed * (0.9 * 0.1 * random.nextDouble());
                double pdy = smokeVelocity.y * speed * (0.9 * 0.1 * random.nextDouble());
                double pdz = smokeVelocity.z * speed * (0.9 * 0.1 * random.nextDouble());
                for (ServerPlayer splayer : slevel.players())
                    slevel.sendParticles(splayer, option, true, sx, sy, sz, 0, pdx, pdy, pdz, 1);
            }
        }
    }

    @Override
    public void shootWithoutEntity(RFEProjectileInstance instance, double dx, double dy, double dz, Level level) {
        Vec3 aimDir = new Vec3(dx, dy, dz);
        RFEAimAngles aimAngles = RFEMathUtils.getAnglesFromVec(aimDir, 0f, 0f);
        Vec3 spawnPos = instance.getPosition(1);
        instance.setVelocity(aimDir.normalize().scale(this.muzzleVelocity));
        this.tick(level, instance);
        if (this.fullHitscan)
            instance.setRemoved();

        if (this.smoke > 0 && level instanceof ServerLevel slevel) {
            RandomSource random = level.getRandom();
            double speed = Math.sqrt(this.smoke);
            double spawnDispersion = Math.min(this.smoke * 0.15, 1);
            ParticleOptions option = new BlackPowderSmokeOptions(this.smoke);
            for (int i = 0; i < 10; ++i) {
                double sx = spawnPos.x + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sy = spawnPos.y + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
                double sz = spawnPos.z + (random.nextDouble() - random.nextDouble()) * spawnDispersion;
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
        Vec3 remDiff = rem == 0 ? wholeDiff : diff.normalize().scale(rem);
        Vec3 totalDiff = Vec3.ZERO;

        Vec3 rootPos = oldPos;

        AABB baseBox = this.getAABB(level, instance);
        double baseDistance = instance.distanceTravelled();

        for (int i = 0; i < iterations; ++i) {
            double hitboxInflation = this.getHitboxInflation(level, instance, baseDistance);
            double suppressionInflation = hitboxInflation + 3;
            baseDistance += 8;

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

        instance.setOldPosition(oldPos);
        instance.setPosition(newPos);

        instance.setDistanceTravelled(instance.distanceTravelled() + newPos.subtract(oldPos).length());
        // TODO handle velocity when collision

        instance.setVelocity(this.applyAccelerationToVelocity(level, instance, instance.velocity()));
        // TODO effects

        if (instance.age() > this.maxAge)
            this.onExpiry(instance, level);
        if (instance.age() > this.maxAge || instance.health() <= 0f)
            instance.setRemoved();
    }

    /**
     * Apply acceleration to the instance velocity. DO NOT modify the instance velocity! Only modify the passed velocity
     * vector.
     * @param level the level of the projectile instance
     * @param instance the projectile instance being accelerated
     * @param velocity the current velocity of the projectile instance
     * @return the new velocity of the projectile instance
     */
    protected Vec3 applyAccelerationToVelocity(Level level, RFEProjectileInstance instance, Vec3 velocity) {
        if (this.quadraticDrag) {
            double dragMag = this.drag * velocity.lengthSqr();
            velocity = velocity.scale(Math.max(1 - dragMag / velocity.length(), 0));
        } else {
            velocity = velocity.scale(Math.max(1 - this.drag, 0));
        }
        return velocity.add(0, -this.gravity, 0);
    }

    @Override
    public AABB getAABB(Level level, RFEProjectileInstance instance) {
        return AABB.ofSize(instance.position(), 0, 0, 0);
    }

    protected double getHitboxInflation(Level level, RFEProjectileInstance instance, double distance) {
        return instance.age() == 0 && distance < 5 ? 0 : 0.1d;
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

                if (level instanceof ServerLevel slevel)
                    EnchantmentHelper.doPostAttackEffects(slevel, living, damagesource);

                this.doPostHurtEffects(instance, level, living);
                // TODO custom on hit sound effect for players
//                if (living != owner && living instanceof Player && owner instanceof ServerPlayer splayer && !this.isSilent()) {
//                    ((ServerPlayer)owner).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
//                }
            }

            // TODO hit effects
//            this.playSound(this.soundEvent, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

            if (RFEConfig.SERVER.enableEntityPenetration.get()) {
                RFEProjectilePenetrationProperties penetrationProperties = this.getPenetrationProperties();
                RFEProjectilePenetrationProperties.PenetrationStats entityPenetration = penetrationProperties.getEntityPenetrationStats(entity);
                if (level.random.nextFloat() > entityPenetration.chance())
                    instance.setRemoved();
                instance.setHealth(instance.health() - entityPenetration.bulletDamage());
                if (instance.health() <= 0)
                    instance.setRemoved();
            } else {
                instance.setRemoved();
            }
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
        boolean canBreak = RFEConfig.SERVER.enableBlockBreaking.get();
        for (Map.Entry<BlockPos, BlockState> entry : penetratedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            RFEProjectilePenetrationProperties.PenetrationStats blockBreaking = penetrationProperties.getBlockBreakingStats(state);
            if (canBreak && instance.health() >= blockBreaking.bulletDamage() && level.random.nextFloat() < blockBreaking.chance()) {
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

        BlockPos hitPos = pResult.getBlockPos();
        BlockState blockstate = level.getBlockState(hitPos);

        //this.lastState = blockstate;
        //blockstate.onProjectileHit(level, blockstate, pResult, this); TODO fake projectile
        Vec3 projPos = instance.position();
        Vec3 terminalVel = pResult.getLocation().subtract(projPos.x, projPos.y, projPos.z);
        instance.setVelocity(terminalVel);
        instance.setRemoved();
        instance.setForceSync(true);

        if (RFEConfig.SERVER.enableBlockBreaking.get()) {
            RFEProjectilePenetrationProperties penetrationProperties = this.getPenetrationProperties();
            RFEProjectilePenetrationProperties.PenetrationStats blockBreaking = penetrationProperties.getBlockBreakingStats(blockstate);
            if (blockstate.getDestroySpeed(level, hitPos) != -1 && instance.health() >= blockBreaking.bulletDamage()
                    && level.random.nextFloat() < blockBreaking.chance()) {
                level.destroyBlock(hitPos, true, instance.getOwner());
                return;
            }
        }

        Vec3 hitLoc = pResult.getLocation();
        SoundType soundType = blockstate.getSoundType();
        level.playSound(null, hitPos, soundType.getHitSound(), SoundSource.BLOCKS, 1.0F, 1.2F / (level.random.nextFloat() * 0.2F + 0.9F));
        if (level instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), hitLoc.x, hitLoc.y, hitLoc.z, 8, 0, 0, 0, 0);
    }

    protected void onExpiry(RFEProjectileInstance instance, Level level) {}

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
        public static final MapCodec<RFEBulletProjectileType> CODEC = RFEBaseProjectilePropertiesBuilder.CODEC
                .xmap(RFEBulletProjectileType::new, RFEBulletProjectileType::makeProjectileProperties);

        public static final StreamCodec<RegistryFriendlyByteBuf, RFEBulletProjectileType> STREAM_CODEC =
                RFEBaseProjectilePropertiesBuilder.STREAM_CODEC.map(RFEBulletProjectileType::new, RFEBulletProjectileType::makeProjectileProperties);

        @Override public MapCodec<RFEBulletProjectileType> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RFEBulletProjectileType> streamCodec() { return STREAM_CODEC; }
    }

}
