package rbasamoyai.ritchiesfirearmengine.foundation.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFECompatHandlers;

public class SableCompat {

    public static void init() {
        RFECompatHandlers.registerKinetmaticsTransformer(new RFECompatHandlers.KinematicsTransformer() {
            @Override
            public Vec3 transformPosition(Level level, Vec3 root, Vec3 pos) {
                SubLevel sublevel = Sable.HELPER.getContaining(level, root);
                if (sublevel == null)
                    return pos;
                Pose3dc pose = level instanceof LevelPoseProviderExtension extension ? extension.sable$getPose(sublevel) : sublevel.logicalPose();
                return pose.transformPosition(pos);
            }
        });
    }

    private SableCompat() {}

}
