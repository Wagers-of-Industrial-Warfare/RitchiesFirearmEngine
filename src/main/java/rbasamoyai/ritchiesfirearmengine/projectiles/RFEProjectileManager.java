package rbasamoyai.ritchiesfirearmengine.projectiles;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.ritchiesfirearmengine.network.RFEClientNetworkHandlers;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Manager for built-in projectile types.
 */
public class RFEProjectileManager {

    private static final Map<Level, Map<UUID, RFEProjectileInstance>> PROJECTILES_BY_LEVEL = new WeakHashMap<>();
    private static final Map<Level, Map<UUID, RFEProjectileInstance>> PROJECTILES_TO_ADD = new WeakHashMap<>();

    public static void tick(Level level) {
        if (PROJECTILES_TO_ADD.containsKey(level)) {
            if (!PROJECTILES_BY_LEVEL.containsKey(level))
                PROJECTILES_BY_LEVEL.put(level, new HashMap<>());
            PROJECTILES_BY_LEVEL.get(level).putAll(PROJECTILES_TO_ADD.get(level));
            PROJECTILES_TO_ADD.remove(level);
        }
        if (!PROJECTILES_BY_LEVEL.containsKey(level))
            return;
        Collection<RFEProjectileInstance> projectiles = PROJECTILES_BY_LEVEL.get(level).values();
        boolean syncAll = level.getLevelData().getGameTime() % 20 == 0;
        for (Iterator<RFEProjectileInstance> iter = projectiles.iterator(); iter.hasNext(); ) {
            RFEProjectileInstance projectile = iter.next();
            projectile.tick(level);
            if (!level.isClientSide) {
                if (projectile.isRemoved()) {
                    iter.remove();
                    RFENetwork.sendToAllInDimension(new ClientboundRemoveRFEProjectilePacket(projectile.uuid(), level.dimension()), level);
                } else if (syncAll || projectile.forceSync()) {
                    projectile.setForceSync(false);
                    RFENetwork.sendToAllInDimension(ClientboundUpdateRFEProjectilePacket.fromProjectile(projectile, level), level);
                }
            }
        }
        if (projectiles.isEmpty())
            PROJECTILES_BY_LEVEL.remove(level);
    }

    public static void onLevelUnload(LevelAccessor level) {
        if (level instanceof Level trueLevel) {
            PROJECTILES_BY_LEVEL.remove(trueLevel);
            PROJECTILES_TO_ADD.remove(trueLevel);
        }
    }

    public static void clearAllProjectiles() {
        PROJECTILES_BY_LEVEL.clear();
        PROJECTILES_TO_ADD.clear();
    }

    public static void queueAddedProjectile(RFEProjectileInstance instance, Level level) {
        if (!PROJECTILES_TO_ADD.containsKey(level))
            PROJECTILES_TO_ADD.put(level, new HashMap<>());
        Map<UUID, RFEProjectileInstance> map = PROJECTILES_TO_ADD.get(level);
        map.put(instance.uuid(), instance);
        if (!level.isClientSide)
            RFENetwork.sendToAllInDimension(ClientboundSpawnRFEProjectilePacket.fromProjectile(instance, level), level);
    }

    public static void syncAllProjectilesToPlayer(ServerPlayer player, Level level) {
        if (!level.isClientSide && PROJECTILES_BY_LEVEL.containsKey(level)) {
            for (RFEProjectileInstance projectile : PROJECTILES_BY_LEVEL.get(level).values())
                RFENetwork.sendToPlayer(ClientboundSpawnRFEProjectilePacket.fromProjectile(projectile, level), player);
        }
    }

    public static void updateProjectile(UUID uuid, Vec3 position, Vec3 velocity, boolean leftOwner, double distanceTravelled, Level level) {
        if (!PROJECTILES_BY_LEVEL.containsKey(level))
            return;
        Map<UUID, RFEProjectileInstance> map = PROJECTILES_BY_LEVEL.get(level);
        if (!map.containsKey(uuid))
            return;
        RFEProjectileInstance instance = map.get(uuid);
        instance.setPosition(position);
        instance.setVelocity(velocity);
        instance.setLeftOwner(leftOwner);
        instance.setDistanceTravelled(distanceTravelled);
    }

    public static void removeProjectile(UUID uuid, Level level) {
        if (!PROJECTILES_BY_LEVEL.containsKey(level))
            return;
        Map<UUID, RFEProjectileInstance> map = PROJECTILES_BY_LEVEL.get(level);
        map.remove(uuid);
    }

    public static Collection<RFEProjectileInstance> getProjectiles(Level level) {
        return PROJECTILES_BY_LEVEL.containsKey(level) ? PROJECTILES_BY_LEVEL.get(level).values() : List.of();
    }

    public record ClientboundSpawnRFEProjectilePacket(RFEProjectileInstance instance, @Nullable Integer ownerId, ResourceKey<Level> level) implements RFEPacket {
        public static ClientboundSpawnRFEProjectilePacket fromProjectile(RFEProjectileInstance instance, Level level) {
            return new ClientboundSpawnRFEProjectilePacket(instance, instance.getOwner() == null ? null : instance.getOwner().getId(), level.dimension());
        }

        public static ClientboundSpawnRFEProjectilePacket decode(FriendlyByteBuf buf) {
            ResourceLocation typeId = buf.readResourceLocation();
            UUID uuid = buf.readUUID();
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            boolean leftOwner = buf.readBoolean();
            double distanceTravelled = buf.readDouble();
            Integer ownerId = null;
            if (buf.readBoolean())
                ownerId = buf.readVarInt();
            ResourceKey<Level> level = buf.readResourceKey(Registries.DIMENSION);
            RFEProjectileType type = Objects.requireNonNull(RFEProjectileTypeHandler.getProjectileType(typeId));
            RFEProjectileInstance instance = new RFEProjectileInstance(type, position, velocity);
            instance.setUUID(uuid);
            instance.setLeftOwner(leftOwner);
            instance.setDistanceTravelled(distanceTravelled);
            return new ClientboundSpawnRFEProjectilePacket(instance, ownerId, level);
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            ResourceLocation id = Objects.requireNonNull(RFEProjectileTypeHandler.getProjectileTypeId(this.instance.projectileType()));
            Vec3 position = this.instance.position();
            Vec3 velocity = this.instance.velocity();
            buf.writeResourceLocation(id)
                    .writeUUID(this.instance.uuid())
                    .writeDouble(position.x)
                    .writeDouble(position.y)
                    .writeDouble(position.z)
                    .writeDouble(velocity.x)
                    .writeDouble(velocity.y)
                    .writeDouble(velocity.z);
            buf.writeBoolean(this.instance.leftOwner())
                    .writeDouble(this.instance.distanceTravelled());
            buf.writeBoolean(this.instance.getOwner() != null);
            if (this.instance.getOwner() != null)
                buf.writeVarInt(this.instance.getOwner().getId());
            buf.writeResourceKey(this.level);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.spawnRFEProjectile(this));
        }
    }

    public record ClientboundUpdateRFEProjectilePacket(UUID uuid, Vec3 position, Vec3 velocity, boolean leftOwner,
                                                       double distanceTravelled, ResourceKey<Level> level) implements RFEPacket {
        public static ClientboundUpdateRFEProjectilePacket fromProjectile(RFEProjectileInstance instance, Level level) {
            return new ClientboundUpdateRFEProjectilePacket(instance.uuid(), instance.position(), instance.velocity(),
                    instance.leftOwner(), instance.distanceTravelled(), level.dimension());
        }

        public static ClientboundUpdateRFEProjectilePacket decode(FriendlyByteBuf buf) {
            UUID uuid = buf.readUUID();
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            boolean leftOwner = buf.readBoolean();
            double distanceTravelled = buf.readDouble();
            ResourceKey<Level> level = buf.readResourceKey(Registries.DIMENSION);
            return new ClientboundUpdateRFEProjectilePacket(uuid, position, velocity, leftOwner, distanceTravelled, level);
        }
        
        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeUUID(this.uuid)
                    .writeDouble(this.position.x)
                    .writeDouble(this.position.y)
                    .writeDouble(this.position.z)
                    .writeDouble(this.velocity.x)
                    .writeDouble(this.velocity.y)
                    .writeDouble(this.velocity.z);
            buf.writeBoolean(this.leftOwner);
            buf.writeDouble(this.distanceTravelled);
            buf.writeResourceKey(this.level);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.updateRFEProjectile(this));
        }
    }

    public record ClientboundRemoveRFEProjectilePacket(UUID uuid, ResourceKey<Level> level) implements RFEPacket {
        public static ClientboundRemoveRFEProjectilePacket decode(FriendlyByteBuf buf) {
            return new ClientboundRemoveRFEProjectilePacket(buf.readUUID(), buf.readResourceKey(Registries.DIMENSION));
        }

        @Override
        public void rootEncode(FriendlyByteBuf buf) {
            buf.writeUUID(this.uuid).writeResourceKey(this.level);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.removeRFEProjectile(this));
        }
    }

    public record ClientboundRemoveAllProjectilesPacket() implements RFEPacket {
        public static ClientboundRemoveAllProjectilesPacket decode(FriendlyByteBuf buf) {
            return new ClientboundRemoveAllProjectilesPacket();
        }

        @Override public void rootEncode(FriendlyByteBuf buf) {}

        @Override
        public void handle(Executor exec, PacketListener listener, @Nullable ServerPlayer sender) {
            clearAllProjectiles();
        }
    }

}
