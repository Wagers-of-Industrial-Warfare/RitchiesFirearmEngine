package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEBlockPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEEntityTypePredicate;

import java.util.Map;

public record RFEProjectilePenetrationProperties(PenetrationStats defaultEntityPenetration,
                                                 ImmutableMap<RFEEntityTypePredicate, PenetrationStats> entityPenetration,
                                                 PenetrationStats defaultBlockPenetration,
                                                 Map<RFEBlockPredicate, PenetrationStats> blockPenetration,
                                                 PenetrationStats defaultBlockBreaking,
                                                 Map<RFEBlockPredicate, PenetrationStats> blockBreaking) {

    public PenetrationStats getEntityPenetrationStats(Entity entity) { return this.getEntityPenetrationStats(entity.getType()); }

    public PenetrationStats getEntityPenetrationStats(EntityType<?> type) {
        for (Map.Entry<RFEEntityTypePredicate, PenetrationStats> entry : this.entityPenetration.entrySet()) {
            if (entry.getKey().test(type))
                return entry.getValue();
        }
        return this.defaultEntityPenetration;
    }

    public PenetrationStats getBlockPenetrationStats(BlockState state) { return this.getBlockPenetrationStats(state.getBlock()); }

    public PenetrationStats getBlockPenetrationStats(Block block) {
        for (Map.Entry<RFEBlockPredicate, PenetrationStats> entry : this.blockPenetration.entrySet()) {
            if (entry.getKey().test(block))
                return entry.getValue();
        }
        return this.defaultBlockPenetration;
    }

    public PenetrationStats getBlockBreakingStats(BlockState state) { return this.getBlockBreakingStats(state.getBlock()); }

    public PenetrationStats getBlockBreakingStats(Block block) {
        for (Map.Entry<RFEBlockPredicate, PenetrationStats> entry : this.blockBreaking.entrySet()) {
            if (entry.getKey().test(block))
                return entry.getValue();
        }
        return this.defaultBlockBreaking;
    }

    public record PenetrationStats(float chance, float bulletDamage) {
        public static void toNetwork(FriendlyByteBuf buf, PenetrationStats stats) {
            buf.writeFloat(stats.chance).writeFloat(stats.bulletDamage);
        }
        
        public static PenetrationStats fromNetwork(FriendlyByteBuf buf) {
            return new PenetrationStats(buf.readFloat(), buf.readFloat());
        }
    }
    
    public static void toNetwork(FriendlyByteBuf buf, RFEProjectilePenetrationProperties properties) {
        PenetrationStats.toNetwork(buf, properties.defaultEntityPenetration);
        buf.writeVarInt(properties.entityPenetration.size());
        for (Map.Entry<RFEEntityTypePredicate, PenetrationStats> entry : properties.entityPenetration.entrySet()) {
            RFEEntityTypePredicate.toNetwork(buf, entry.getKey());
            PenetrationStats.toNetwork(buf, entry.getValue());
        }
        PenetrationStats.toNetwork(buf, properties.defaultBlockPenetration);
        buf.writeVarInt(properties.blockPenetration.size());
        for (Map.Entry<RFEBlockPredicate, PenetrationStats> entry : properties.blockPenetration.entrySet()) {
            RFEBlockPredicate.toNetwork(buf, entry.getKey());
            PenetrationStats.toNetwork(buf, entry.getValue());
        }
        PenetrationStats.toNetwork(buf, properties.defaultBlockBreaking);
        buf.writeVarInt(properties.blockBreaking.size());
        for (Map.Entry<RFEBlockPredicate, PenetrationStats> entry : properties.blockBreaking.entrySet()) {
            RFEBlockPredicate.toNetwork(buf, entry.getKey());
            PenetrationStats.toNetwork(buf, entry.getValue());
        }
    }
    
    public static RFEProjectilePenetrationProperties fromNetwork(FriendlyByteBuf buf) {
        PenetrationStats defaultEntityPenetration = PenetrationStats.fromNetwork(buf);
        int epsz = buf.readVarInt();
        ImmutableMap.Builder<RFEEntityTypePredicate, PenetrationStats> entityPenetration = ImmutableMap.builder();
        for (int i = 0; i < epsz; ++i) {
            RFEEntityTypePredicate pred = RFEEntityTypePredicate.fromNetwork(buf);
            PenetrationStats stats = PenetrationStats.fromNetwork(buf);
            entityPenetration.put(pred, stats);
        }
        PenetrationStats defaultBlockPenetration = PenetrationStats.fromNetwork(buf);
        int bpsz = buf.readVarInt();
        ImmutableMap.Builder<RFEBlockPredicate, PenetrationStats> blockPenetration = ImmutableMap.builder();
        for (int i = 0; i < bpsz; ++i) {
            RFEBlockPredicate pred = RFEBlockPredicate.fromNetwork(buf);
            PenetrationStats stats = PenetrationStats.fromNetwork(buf);
            blockPenetration.put(pred, stats);
        }
        PenetrationStats defaultBlockBreaking = PenetrationStats.fromNetwork(buf);
        int bbsz = buf.readVarInt();
        ImmutableMap.Builder<RFEBlockPredicate, PenetrationStats> blockBreaking = ImmutableMap.builder();
        for (int i = 0; i < bbsz; ++i) {
            RFEBlockPredicate pred = RFEBlockPredicate.fromNetwork(buf);
            PenetrationStats stats = PenetrationStats.fromNetwork(buf);
            blockBreaking.put(pred, stats);
        }
        return new RFEProjectilePenetrationProperties(defaultEntityPenetration, entityPenetration.build(),
                defaultBlockPenetration, blockPenetration.build(), defaultBlockBreaking, blockBreaking.build());
    }

}
