package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.network.RFEClientNetworkHandlers;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.EnvExecute;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import javax.annotation.Nullable;
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
        for (Iterator<RFEProjectileInstance> iter = projectiles.iterator(); iter.hasNext(); ) {
            RFEProjectileInstance projectile = iter.next();
            boolean isFalseProjectile = projectile.isFalseProjectile();
            projectile.tick(level);
            if (!isFalseProjectile && projectile.isRemoved() || isFalseProjectile && projectile.age() > 1) {
                iter.remove();
                if (level instanceof ServerLevel slevel)
                    RFENetwork.sendToAllInDimension(new ClientboundRemoveRFEProjectilePacket(projectile.uuid(), level.dimension()), slevel);
            } else if (projectile.forceSync()) {
                projectile.setForceSync(false);
                if (level instanceof ServerLevel slevel)
                    RFENetwork.sendToAllInDimension(ClientboundUpdateRFEProjectilePacket.fromProjectile(projectile, level), slevel);
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
        if (instance.isRemoved())
            return;
        if (!PROJECTILES_TO_ADD.containsKey(level))
            PROJECTILES_TO_ADD.put(level, new HashMap<>());
        Map<UUID, RFEProjectileInstance> map = PROJECTILES_TO_ADD.get(level);
        map.put(instance.uuid(), instance);
        if (level instanceof ServerLevel slevel)
            RFENetwork.sendToAllInDimension(ClientboundSpawnRFEProjectilePacket.fromProjectile(instance, level), slevel);
    }

    public static void syncAllProjectilesToPlayer(ServerPlayer player, Level level) {
        if (!level.isClientSide && PROJECTILES_BY_LEVEL.containsKey(level)) {
            for (RFEProjectileInstance projectile : PROJECTILES_BY_LEVEL.get(level).values())
                RFENetwork.sendToPlayer(ClientboundSpawnRFEProjectilePacket.fromProjectile(projectile, level), player);
        }
    }

    public static void updateProjectile(UUID uuid, Vec3 position, Vec3 oldPosition, Vec3 velocity, boolean leftOwner,
                                        double distanceTravelled, int age, Level level) {
        if (!PROJECTILES_BY_LEVEL.containsKey(level))
            return;
        Map<UUID, RFEProjectileInstance> map = PROJECTILES_BY_LEVEL.get(level);
        if (!map.containsKey(uuid))
            return;
        RFEProjectileInstance instance = map.get(uuid);
        instance.setPosition(position);
        instance.setOldPosition(oldPosition);
        instance.setVelocity(velocity);
        instance.setLeftOwner(leftOwner);
        instance.setDistanceTravelled(distanceTravelled);
        instance.setAge(age);
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

        private static final StreamCodec<RegistryFriendlyByteBuf, RFEProjectileInstance> INSTANCE_SPAWN_STREAM_CODEC = RFEByteBufCodecUtils.composite7(
                RFEProjectileTypeHandler.LOADED_TYPE_STREAM_CODEC, RFEProjectileInstance::projectileType,
                UUIDUtil.STREAM_CODEC, RFEProjectileInstance::uuid,
                RFEByteBufCodecUtils.VEC3_STREAM_CODEC, RFEProjectileInstance::position,
                RFEByteBufCodecUtils.VEC3_STREAM_CODEC, RFEProjectileInstance::velocity,
                ByteBufCodecs.BOOL, RFEProjectileInstance::leftOwner,
                ByteBufCodecs.DOUBLE, RFEProjectileInstance::distanceTravelled,
                ByteBufCodecs.BOOL, RFEProjectileInstance::isFalseProjectile,
                (type, uuid, pos, vel, leftOwner, distanceTravelled, isFalseProjectile) -> {
                    RFEProjectileInstance instance = new RFEProjectileInstance(type);
                    instance.setPosition(pos);
                    instance.setOldPosition(pos);
                    instance.setVelocity(vel);
                    instance.setUUID(uuid);
                    instance.setLeftOwner(leftOwner);
                    instance.setDistanceTravelled(distanceTravelled);
                    instance.setFalseProjectile(isFalseProjectile);
                    return instance;
                });

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSpawnRFEProjectilePacket> STREAM_CODEC = StreamCodec.composite(
                INSTANCE_SPAWN_STREAM_CODEC, ClientboundSpawnRFEProjectilePacket::instance,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).map(o -> o.orElse(null), Optional::ofNullable), ClientboundSpawnRFEProjectilePacket::ownerId,
                ResourceKey.streamCodec(Registries.DIMENSION), ClientboundSpawnRFEProjectilePacket::level,
                ClientboundSpawnRFEProjectilePacket::new);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.spawnRFEProjectile(this));
        }
    }

    public record ClientboundUpdateRFEProjectilePacket(UUID uuid, Vec3 position, Vec3 oldPosition, Vec3 velocity, boolean leftOwner,
                                                       double distanceTravelled, int age, ResourceKey<Level> level) implements RFEPacket {
        public static ClientboundUpdateRFEProjectilePacket fromProjectile(RFEProjectileInstance instance, Level level) {
            return new ClientboundUpdateRFEProjectilePacket(instance.uuid(), instance.position(), instance.oldPosition(), instance.velocity(),
                    instance.leftOwner(), instance.distanceTravelled(), instance.age(), level.dimension());
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateRFEProjectilePacket> STREAM_CODEC =
                StreamCodec.of(ClientboundUpdateRFEProjectilePacket::rootEncode, ClientboundUpdateRFEProjectilePacket::decode);

        public static ClientboundUpdateRFEProjectilePacket decode(FriendlyByteBuf buf) {
            UUID uuid = buf.readUUID();
            Vec3 position = RFEByteBufCodecUtils.VEC3_STREAM_CODEC.decode(buf);
            Vec3 oldPosition = RFEByteBufCodecUtils.VEC3_STREAM_CODEC.decode(buf);
            Vec3 velocity = RFEByteBufCodecUtils.VEC3_STREAM_CODEC.decode(buf);
            boolean leftOwner = buf.readBoolean();
            double distanceTravelled = buf.readDouble();
            int age = buf.readVarInt();
            ResourceKey<Level> level = buf.readResourceKey(Registries.DIMENSION);
            return new ClientboundUpdateRFEProjectilePacket(uuid, position, oldPosition, velocity, leftOwner, distanceTravelled, age, level);
        }

        public static void rootEncode(FriendlyByteBuf buf, ClientboundUpdateRFEProjectilePacket pkt) {
            buf.writeUUID(pkt.uuid);
            RFEByteBufCodecUtils.VEC3_STREAM_CODEC.encode(buf, pkt.position);
            RFEByteBufCodecUtils.VEC3_STREAM_CODEC.encode(buf, pkt.oldPosition);
            RFEByteBufCodecUtils.VEC3_STREAM_CODEC.encode(buf, pkt.velocity);
            buf.writeBoolean(pkt.leftOwner)
                    .writeDouble(pkt.distanceTravelled);
            buf.writeVarInt(pkt.age)
                    .writeResourceKey(pkt.level);
        }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.updateRFEProjectile(this));
        }
    }

    public record ClientboundRemoveRFEProjectilePacket(UUID uuid, ResourceKey<Level> level) implements RFEPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRemoveRFEProjectilePacket> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, ClientboundRemoveRFEProjectilePacket::uuid,
                ResourceKey.streamCodec(Registries.DIMENSION), ClientboundRemoveRFEProjectilePacket::level,
                ClientboundRemoveRFEProjectilePacket::new);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            EnvExecute.runOnClient(() -> () -> RFEClientNetworkHandlers.removeRFEProjectile(this));
        }
    }

    public static class ClientboundRemoveAllProjectilesPacket implements RFEPacket {
        public static final ClientboundRemoveAllProjectilesPacket INSTANCE = new ClientboundRemoveAllProjectilesPacket();

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRemoveAllProjectilesPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            clearAllProjectiles();
        }

        private ClientboundRemoveAllProjectilesPacket() {}
    }

}
