package rbasamoyai.ritchiesfirearmengine.content.projectiles;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class RFEEntityHitResult extends EntityHitResult {

    private final Vec3 hitPos;

    public RFEEntityHitResult(Entity entity, Vec3 hitPos) {
        super(entity);
        this.hitPos = hitPos;
    }

    @Override public Vec3 getLocation() { return this.hitPos; }

}
