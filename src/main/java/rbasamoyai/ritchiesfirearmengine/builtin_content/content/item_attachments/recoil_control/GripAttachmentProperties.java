package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.recoil_control;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

import java.util.Optional;

public record GripAttachmentProperties(Optional<RFEFirearmProperties<RFERecoilProvider>> recoilProperties,
                                       Optional<RFEFirearmProperties<RFESpreadProvider>> spreadProperties) implements RFEItemAttachmentProperties {

    @Override public boolean overridesDefaults() { return true; }

    @Override public RFEItemAttachmentProperties.Serializer<?> getSerializer() { return BuiltInRFEPlugin.AttachmentSlots.GRIP; }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<GripAttachmentProperties> {
        private static final MapCodec<GripAttachmentProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEFirearmProperties.makeCodec(RFERecoilProvider.CODEC).optionalFieldOf("recoil").forGetter(GripAttachmentProperties::recoilProperties),
                RFEFirearmProperties.makeCodec(RFESpreadProvider.CODEC).optionalFieldOf("spread").forGetter(GripAttachmentProperties::spreadProperties)
        ).apply(o, GripAttachmentProperties::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GripAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(RFEFirearmProperties.makeStreamCodec(RFERecoilProvider.STREAM_CODEC)), GripAttachmentProperties::recoilProperties,
                ByteBufCodecs.optional(RFEFirearmProperties.makeStreamCodec(RFESpreadProvider.STREAM_CODEC)), GripAttachmentProperties::spreadProperties,
                GripAttachmentProperties::new);

        @Override public MapCodec<GripAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GripAttachmentProperties> streamCodec() { return STREAM_CODEC; }
    }

}
