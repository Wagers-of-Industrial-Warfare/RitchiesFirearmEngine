package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;

public interface RFEItemAttachmentProperties {

    MapCodec<RFEItemAttachmentProperties> CODEC = ResourceLocation.CODEC
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getItemAttachmentSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment type: " + e.getMessage());
                        }
                    },
                    ser -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getItemAttachmentSerializerId(ser));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment type id: " + e.getMessage());
                        }
                    })
            .dispatchMap(RFEItemAttachmentProperties::getSerializer, Serializer::codec);

    StreamCodec<RegistryFriendlyByteBuf, RFEItemAttachmentProperties> STREAM_CODEC = ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
            .<Serializer<?>>map(RFEContentBuilderRegistry::getItemAttachmentSerializer, RFEContentBuilderRegistry::getItemAttachmentSerializerId)
            .dispatch(RFEItemAttachmentProperties::getSerializer, Serializer::streamCodec);
    
    Serializer<?> getSerializer();

    interface Serializer<T extends RFEItemAttachmentProperties> {
        MapCodec<T> codec();
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
    }

}
