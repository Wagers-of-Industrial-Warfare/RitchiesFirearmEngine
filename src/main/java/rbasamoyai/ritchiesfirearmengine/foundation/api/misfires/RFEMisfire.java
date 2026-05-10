package rbasamoyai.ritchiesfirearmengine.foundation.api.misfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface RFEMisfire {

    Codec<List<RFEMisfire>> LIST_CODEC = ExtraCodecs.strictUnboundedMap(
            ResourceLocation.CODEC.flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getMisfireProvider(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving misfire provider type: " + e.getMessage());
                        }
                    },
                    prov -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getMisfireProviderId(prov));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving misfire provider type id: " + e.getMessage());
                        }
                    }),
            Codec.FLOAT)
            .xmap(map -> map.entrySet().stream().map(e -> e.getKey().apply(e.getValue())).toList(),
                    li -> li.stream().collect(Collectors.toMap(RFEMisfire::getMisfireProvider, RFEMisfire::getChance)));

    StreamCodec<RegistryFriendlyByteBuf, RFEMisfire> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.map(RFEContentBuilderRegistry::getMisfireProvider, RFEContentBuilderRegistry::getMisfireProviderId), RFEMisfire::getMisfireProvider,
            ByteBufCodecs.FLOAT, RFEMisfire::getChance,
            Provider::apply);

    boolean canMisfire(ItemStack itemStack, LivingEntity entity);
    float getChance();
    Provider getMisfireProvider();

    @FunctionalInterface
    interface Provider extends Function<Float, RFEMisfire> {
    }

}
