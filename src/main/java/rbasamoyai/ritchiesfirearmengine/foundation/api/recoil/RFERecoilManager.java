package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public class RFERecoilManager {

    private static final Map<LivingEntity, RFERecoilInstance> RECOIL_INSTANCES = new WeakHashMap<>();

    public static void clearTrackedRecoil() {
        RECOIL_INSTANCES.clear();
    }

    public static void tick(Level level) {
        for (var iter = RECOIL_INSTANCES.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<LivingEntity, RFERecoilInstance> entry = iter.next();
            if (entry.getKey().isRemoved()) {
                iter.remove();
                continue;
            }
            if (entry.getKey().level() != level)
                continue;
            RFERecoilInstance instance = entry.getValue();
            instance.tickRecoilBehavior();
            if (instance.isRemoved())
                iter.remove();
        }
    }

    @Nullable
    public static RFERecoilInstance getRecoilInstance(LivingEntity entity) {
        return RECOIL_INSTANCES.get(entity);
    }

    /**
     * Will not track "instantaneous" recoil instances, that is, where {@link RFERecoilInstance#isRemoved()} is already
     * false.
     * @param instance
     * @param entity
     */
    public static void trackRecoil(RFERecoilInstance instance, LivingEntity entity) {
        if (!instance.isRemoved())
            RECOIL_INSTANCES.put(entity, instance);
    }

    public static void stopTrackingEntity(LivingEntity entity) {
        RECOIL_INSTANCES.remove(entity);
    }

}
