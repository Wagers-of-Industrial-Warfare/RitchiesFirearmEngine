package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class RFERecoilManager {

    private static final Map<LivingEntity, Map<InteractionHand, TaggedRecoilInstance>> RECOIL_INSTANCES = new WeakHashMap<>();

    public static void clearTrackedRecoil() {
        RECOIL_INSTANCES.clear();
    }

    public static void tick(Level level) {
        for (var iter = RECOIL_INSTANCES.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<LivingEntity, Map<InteractionHand, TaggedRecoilInstance>> entry = iter.next();
            if (entry.getKey().isRemoved()) {
                iter.remove();
                continue;
            }
            if (entry.getKey().level() != level)
                continue;
            var hands = entry.getValue();
            for (Iterator<TaggedRecoilInstance> iter1 = hands.values().iterator(); iter1.hasNext(); ) {
                RFERecoilInstance instance = iter1.next().instance();
                instance.tickRecoilBehavior();
                if (instance.isRemoved())
                    iter1.remove();
            }
            if (hands.isEmpty())
                iter.remove();
        }
    }

    @Nullable
    public static RFERecoilInstance getRecoilInstance(LivingEntity entity, ItemStack itemStack) {
        if (!RECOIL_INSTANCES.containsKey(entity))
            return null;
        Map<InteractionHand, TaggedRecoilInstance> instances = RECOIL_INSTANCES.get(entity);

        if (!itemStack.getOrCreateTag().contains("ritchiesfirearmengine:recoil_identifier"))
            return null;
        UUID uuid = getOrCreateRecoilId(entity, itemStack);

        for (TaggedRecoilInstance instance : instances.values()) {
            if (instance.uuid.equals(uuid))
                return instance.instance;
        }
        return null;
    }

    /**
     * Will not track "instantaneous" recoil instances, that is, where {@link RFERecoilInstance#isRemoved()} is already
     * false.
     * @param instance
     * @param entity
     * @param itemStack
     * @param hand
     */
    public static void trackRecoil(RFERecoilInstance instance, RFERecoilProvider provider, LivingEntity entity,
                                   ItemStack itemStack, InteractionHand hand) {
        if (instance.isRemoved())
            return;
        if (!RECOIL_INSTANCES.containsKey(entity))
            RECOIL_INSTANCES.put(entity, new EnumMap<>(InteractionHand.class));
        Map<InteractionHand, TaggedRecoilInstance> instances = RECOIL_INSTANCES.get(entity);

        UUID recoilId = getOrCreateRecoilId(entity, itemStack);
        instances.put(hand, new TaggedRecoilInstance(recoilId, instance, provider));
    }

    @Nullable
    public static RFERecoilProvider getCurrentProvider(LivingEntity entity, InteractionHand hand) {
        if (!RECOIL_INSTANCES.containsKey(entity))
            return null;
        Map<InteractionHand, TaggedRecoilInstance> instances = RECOIL_INSTANCES.get(entity);
        return instances.containsKey(hand) ? instances.get(hand).source : null;
    }

    public static void stopTrackingRecoil(LivingEntity entity, ItemStack itemStack) {
        if (!RECOIL_INSTANCES.containsKey(entity))
            return;
        Map<InteractionHand, TaggedRecoilInstance> instances = RECOIL_INSTANCES.get(entity);

        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("ritchiesfirearmengine:recoil_identifier"))
            return;
        UUID uuid = tag.getUUID("ritchiesfirearmengine:recoil_identifier");
        tag.remove("ritchiesfirearmengine:recoil_identifier");
        for (Iterator<TaggedRecoilInstance> iter = instances.values().iterator(); iter.hasNext(); ) {
            TaggedRecoilInstance instance = iter.next();
            if (instance.uuid.equals(uuid))
                iter.remove();
        }
        if (instances.isEmpty())
            RECOIL_INSTANCES.remove(entity);
    }

    public static void stopTrackingEntity(LivingEntity entity) {
        RECOIL_INSTANCES.remove(entity);
    }

    private static UUID getOrCreateRecoilId(LivingEntity entity, ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("ritchiesfirearmengine:recoil_identifier"))
            tag.putUUID("ritchiesfirearmengine:recoil_identifier", Mth.createInsecureUUID(entity.getRandom()));
        return tag.getUUID("ritchiesfirearmengine:recoil_identifier");
    }

    @Nullable
    public static UUID getRecoilId(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        return tag.contains("ritchiesfirearmengine:recoil_identifier") ? tag.getUUID("ritchiesfirearmengine:recoil_identifier") : null;
    }

    public static void setRecoilId(ItemStack itemStack, @Nullable UUID uuid) {
        if (uuid == null) {
            itemStack.getOrCreateTag().remove("ritchiesfirearmengine:recoil_identifier");
        } else {
            itemStack.getOrCreateTag().putUUID("ritchiesfirearmengine:recoil_identifier", uuid);
        }
    }

    private record TaggedRecoilInstance(UUID uuid, RFERecoilInstance instance, RFERecoilProvider source) {
    }

}
