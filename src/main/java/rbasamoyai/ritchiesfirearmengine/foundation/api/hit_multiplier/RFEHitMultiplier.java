package rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface RFEHitMultiplier {

    Codec<List<RFEHitMultiplier>> LIST_CODEC = ExtraCodecs.strictUnboundedMap(
            ResourceLocation.CODEC.flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getHitMultiplierProvider(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving hit multiplier type: " + e.getMessage());
                        }
                    },
                    prov -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getHitMultiplierProviderId(prov));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving hit multiplier id: " + e.getMessage());
                        }
                    }),
            Codec.FLOAT)
            .xmap(map -> map.entrySet().stream().map(e -> e.getKey().apply(e.getValue())).toList(),
                    li -> li.stream().collect(Collectors.toMap(RFEHitMultiplier::getProvider, RFEHitMultiplier::getMultiplier)));

    StreamCodec<RegistryFriendlyByteBuf, RFEHitMultiplier> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.map(RFEContentBuilderRegistry::getHitMultiplierProvider, RFEContentBuilderRegistry::getHitMultiplierProviderId),
            RFEHitMultiplier::getProvider,
            ByteBufCodecs.FLOAT,
            RFEHitMultiplier::getMultiplier,
            Provider::apply);

    float multiplyDamage(Entity target, RFEProjectileInstance projectile, EntityHitResult hitResult, float damage);

    Provider getProvider();
    float getMultiplier();

    @FunctionalInterface
    interface Provider extends Function<Float, RFEHitMultiplier> {
    }

}
