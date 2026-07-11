package rbasamoyai.ritchiesfirearmengine.foundation.api;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RFECompatHandlers {

    // Kinematic transforms

    private static final List<KinematicsTransformer> KINEMATICS_TRANSFORMERS = new ReferenceArrayList<>();

    public static void registerKinetmaticsTransformer(KinematicsTransformer transformer) { KINEMATICS_TRANSFORMERS.add(transformer); }

    public static Vec3 transformPosition(Level level, Vec3 root, Vec3 pos) {
        for (KinematicsTransformer t : KINEMATICS_TRANSFORMERS)
            pos = t.transformPosition(level, root, pos);
        return pos;
    }

    public static Vec3 transformPosition(Level level, Vec3 pos) { return transformPosition(level, pos, pos); }

    public interface KinematicsTransformer {
        default Vec3 transformPosition(Level level, Vec3 root, Vec3 pos) { return pos; }
    }

}
