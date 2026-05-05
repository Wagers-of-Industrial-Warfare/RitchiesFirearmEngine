package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEBlockPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEEntityTypePredicate;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import java.util.Map;

public record RFEProjectilePenetrationProperties(PenetrationStats defaultEntityPenetration,
                                                 ImmutableMap<RFEEntityTypePredicate, PenetrationStats> entityPenetration,
                                                 PenetrationStats defaultBlockPenetration,
                                                 ImmutableMap<RFEBlockPredicate, PenetrationStats> blockPenetration,
                                                 PenetrationStats defaultBlockBreaking,
                                                 ImmutableMap<RFEBlockPredicate, PenetrationStats> blockBreaking) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEProjectilePenetrationProperties> STREAM_CODEC = StreamCodec.composite(
            PenetrationStats.STREAM_CODEC, RFEProjectilePenetrationProperties::defaultEntityPenetration,
            RFEByteBufCodecUtils.immutableMap(RFEEntityTypePredicate.STREAM_CODEC, PenetrationStats.STREAM_CODEC), RFEProjectilePenetrationProperties::entityPenetration,
            PenetrationStats.STREAM_CODEC, RFEProjectilePenetrationProperties::defaultBlockPenetration,
            RFEByteBufCodecUtils.immutableMap(RFEBlockPredicate.STREAM_CODEC, PenetrationStats.STREAM_CODEC), RFEProjectilePenetrationProperties::blockPenetration,
            PenetrationStats.STREAM_CODEC, RFEProjectilePenetrationProperties::defaultBlockBreaking,
            RFEByteBufCodecUtils.immutableMap(RFEBlockPredicate.STREAM_CODEC, PenetrationStats.STREAM_CODEC), RFEProjectilePenetrationProperties::blockBreaking,
            RFEProjectilePenetrationProperties::new);

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
        public static final MapCodec<PenetrationStats> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.floatRange(0f, 1f).optionalFieldOf("chance", 1f).forGetter(PenetrationStats::chance),
                Codec.floatRange(0f, 1f).optionalFieldOf("damage_to_projectile", 0.25f).forGetter(PenetrationStats::bulletDamage)
        ).apply(o, PenetrationStats::new));

        public static final StreamCodec<ByteBuf, PenetrationStats> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, PenetrationStats::chance, ByteBufCodecs.FLOAT, PenetrationStats::bulletDamage, PenetrationStats::new);
    }

}
