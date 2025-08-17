package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class RFESpreadManager {

    private static final Map<LivingEntity, Map<InteractionHand, TaggedSpreadInstance>> SPREAD_INSTANCES = new WeakHashMap<>();

    public static void clearTrackedSpread() {
        SPREAD_INSTANCES.clear();
    }

    public static void tick(Level level) {
        for (var iter = SPREAD_INSTANCES.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<LivingEntity, Map<InteractionHand, TaggedSpreadInstance>> entry = iter.next();
            if (entry.getKey().isRemoved()) {
                iter.remove();
                continue;
            }
            if (entry.getKey().level() != level)
                continue;
            Map<InteractionHand, TaggedSpreadInstance> instances = entry.getValue();
            for (Iterator<TaggedSpreadInstance> iter1 = instances.values().iterator(); iter1.hasNext(); ) {
                RFESpreadInstance instance = iter1.next().instance();
                instance.tickSpreadBehavior();
                if (instance.isRemoved())
                    iter1.remove();
            }
            if (instances.isEmpty())
                iter.remove();
        }
    }

    @Nullable
    public static RFESpreadInstance getSpreadInstance(LivingEntity entity, ItemStack itemStack) {
        if (!SPREAD_INSTANCES.containsKey(entity))
            return null;
        Map<InteractionHand, TaggedSpreadInstance> instances = SPREAD_INSTANCES.get(entity);

        if (!itemStack.getOrCreateTag().contains("ritchiesfirearmengine:spread_identifier"))
            return null;
        UUID uuid = getOrCreateSpreadId(entity, itemStack);

        for (TaggedSpreadInstance instance : instances.values()) {
            if (instance.uuid.equals(uuid))
                return instance.instance;
        }
        return null;
    }

    /**
     * Will not track "instantaneous" spread instances, that is, where {@link RFESpreadInstance#isRemoved()} is already
     * false.
     * @param instance
     * @param entity
     * @param itemStack
     * @param hand
     */
    public static void trackSpread(RFESpreadInstance instance, RFESpreadProvider provider, LivingEntity entity,
                                   ItemStack itemStack, InteractionHand hand) {
        if (instance.isRemoved())
            return;
        if (!SPREAD_INSTANCES.containsKey(entity))
            SPREAD_INSTANCES.put(entity, new EnumMap<>(InteractionHand.class));
        Map<InteractionHand, TaggedSpreadInstance> instances = SPREAD_INSTANCES.get(entity);

        UUID spreadId = getOrCreateSpreadId(entity, itemStack);
        instances.put(hand, new TaggedSpreadInstance(spreadId, instance, provider));
    }

    @Nullable
    public static RFESpreadProvider getCurrentProvider(LivingEntity entity, InteractionHand hand) {
        if (!SPREAD_INSTANCES.containsKey(entity))
            return null;
        Map<InteractionHand, TaggedSpreadInstance> instances = SPREAD_INSTANCES.get(entity);
        return instances.containsKey(hand) ? instances.get(hand).source : null;
    }

    public static void stopTrackingSpread(LivingEntity entity, ItemStack itemStack) {
        if (!SPREAD_INSTANCES.containsKey(entity))
            return;
        Map<InteractionHand, TaggedSpreadInstance> instances = SPREAD_INSTANCES.get(entity);

        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("ritchiesfirearmengine:spread_identifier"))
            return;
        UUID uuid = tag.getUUID("ritchiesfirearmengine:spread_identifier");
        tag.remove("ritchiesfirearmengine:spread_identifier");
        for (Iterator<TaggedSpreadInstance> iter = instances.values().iterator(); iter.hasNext(); ) {
            TaggedSpreadInstance instance = iter.next();
            if (instance.uuid.equals(uuid))
                iter.remove();
        }
        if (instances.isEmpty())
            SPREAD_INSTANCES.remove(entity);
    }

    public static void stopTrackingEntity(LivingEntity entity) {
        SPREAD_INSTANCES.remove(entity);
    }

    private static UUID getOrCreateSpreadId(LivingEntity entity, ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (!tag.contains("ritchiesfirearmengine:spread_identifier"))
            tag.putUUID("ritchiesfirearmengine:spread_identifier", Mth.createInsecureUUID(entity.getRandom()));
        return tag.getUUID("ritchiesfirearmengine:spread_identifier");
    }

    @Nullable
    public static UUID getSpreadId(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        return tag.contains("ritchiesfirearmengine:spread_identifier") ? tag.getUUID("ritchiesfirearmengine:spread_identifier") : null;
    }

    public static void setSpreadId(ItemStack itemStack, @Nullable UUID uuid) {
        if (uuid == null) {
            itemStack.getOrCreateTag().remove("ritchiesfirearmengine:spread_identifier");
        } else {
            itemStack.getOrCreateTag().putUUID("ritchiesfirearmengine:spread_identifier", uuid);
        }
    }

    private record TaggedSpreadInstance(UUID uuid, RFESpreadInstance instance, RFESpreadProvider source) {
    }

}
