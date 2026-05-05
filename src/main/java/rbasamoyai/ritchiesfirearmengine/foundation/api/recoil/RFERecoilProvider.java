package rbasamoyai.ritchiesfirearmengine.foundation.api.recoil;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;

public interface RFERecoilProvider {

    MapCodec<RFERecoilProvider> CODEC = ResourceLocation.CODEC
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getRecoilProviderSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving recoil provider type: " + e.getMessage());
                        }
                    },
                    prov -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getRecoilProviderSerializerId(prov));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving recoil provider type id: " + e.getMessage());
                        }
                    })
            .dispatchMap(RFERecoilProvider::getSerializer, RFERecoilProvider.Serializer::codec);

    StreamCodec<RegistryFriendlyByteBuf, RFERecoilProvider> STREAM_CODEC = ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
            .<RFERecoilProvider.Serializer<?>>map(RFEContentBuilderRegistry::getRecoilProviderSerializer, RFEContentBuilderRegistry::getRecoilProviderSerializerId)
            .dispatch(RFERecoilProvider::getSerializer, RFERecoilProvider.Serializer::streamCodec);
    
    RFERecoilInstance createRecoilInstance(ItemStack itemStack, LivingEntity entity, RandomSource random);

    Serializer<?> getSerializer();

    interface Serializer<T extends RFERecoilProvider> {
        MapCodec<T> codec();
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
    }

}
