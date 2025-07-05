package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class RFEProjectileInstance {

    private final RandomSource random = RandomSource.create();
    private final RFEProjectileType projectileType;
    private Vec3 position = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;
    private UUID uuid = Mth.createInsecureUUID(this.random);
    private boolean removed = false;
    @Nullable private Entity owner;
    private boolean leftOwner = false;
    private int age = 0;
    private double distanceTravelled = 0;
    private boolean forceSync = false;

    public RFEProjectileInstance(RFEProjectileType projectileType) {
        this.projectileType = projectileType;
    }

    public RFEProjectileType projectileType() { return this.projectileType; }

    public Vec3 position() { return this.position; }
    public void setPosition(Vec3 position) { this.position = position; }

    public Vec3 velocity() { return this.velocity; }
    public void setVelocity(Vec3 velocity) { this.velocity = velocity; }

    public boolean isRemoved() { return this.removed; }
    public void setRemoved() { this.removed = true; }

    public UUID uuid() { return this.uuid; }
    public void setUUID(UUID uuid) { this.uuid = uuid; }

    @Nullable public Entity getOwner() { return this.owner; }
    public void setOwner(@Nullable Entity owner) { this.owner = owner; }

    public boolean leftOwner() { return this.leftOwner; }
    public void setLeftOwner(boolean leftOwner) { this.leftOwner = leftOwner; }

    private boolean checkLeftOwner(Level level) {
        Entity owner = this.getOwner();
        if (owner == null)
            return true;
        AABB checkBox = this.projectileType.getAABB(level, this).expandTowards(this.velocity).inflate(1d);
        for (Entity entity : level.getEntities((Entity) null, checkBox, e -> !e.isSpectator() && e.isPickable())) {
            if (entity.getRootVehicle() == owner.getRootVehicle())
                return false;
        }
        return true;
    }

    public int age() { return this.age; }
    public void setAge(int age) { this.age = age; }

    public double distanceTravelled() { return this.distanceTravelled; }
    public void setDistanceTravelled(double distanceTravelled) { this.distanceTravelled = distanceTravelled; }

    public boolean forceSync() { return this.forceSync; }
    public void setForceSync(boolean forceSync) { this.forceSync = forceSync; }

    public void tick(Level level) {
        if (!this.leftOwner)
            this.leftOwner = this.checkLeftOwner(level);
        this.projectileType.tick(level, this);
        ++this.age;
    }

    public void shoot(double dx, double dy, double dz /* TODO spread provider */) {
        this.projectileType.shoot(this, dx, dy, dz);
    }

}
