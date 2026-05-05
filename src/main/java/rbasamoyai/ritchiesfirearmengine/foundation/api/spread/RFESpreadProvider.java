package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;

public interface RFESpreadProvider {

    MapCodec<RFESpreadProvider> CODEC = ResourceLocation.CODEC.fieldOf("type").codec()
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getSpreadProviderSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving spread provider type: " + e.getMessage());
                        }
                    },
                    prov -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getSpreadProviderSerializerId(prov));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving spread provider type id: " + e.getMessage());
                        }
                    })
            .dispatchMap(RFESpreadProvider::getSerializer, RFESpreadProvider.Serializer::codec);

    StreamCodec<RegistryFriendlyByteBuf, RFESpreadProvider> STREAM_CODEC = ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
            .<Serializer<?>>map(RFEContentBuilderRegistry::getSpreadProviderSerializer, RFEContentBuilderRegistry::getSpreadProviderSerializerId)
            .dispatch(RFESpreadProvider::getSerializer, Serializer::streamCodec);

    RFESpreadInstance createSpreadInstance(ItemStack itemStack, LivingEntity entity, RandomSource random);

    Serializer<?> getSerializer();

    interface Serializer<T extends RFESpreadProvider> {
        MapCodec<T> codec();
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
    }

}
