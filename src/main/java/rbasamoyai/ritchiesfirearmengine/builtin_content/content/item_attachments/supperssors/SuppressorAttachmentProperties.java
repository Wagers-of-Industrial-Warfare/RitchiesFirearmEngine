package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.supperssors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;

import javax.annotation.Nullable;
import java.util.Optional;

public record SuppressorAttachmentProperties(RFEFirearmProperties<SuppressedSound> soundProperties)
    implements RFEItemAttachmentProperties {

    public SuppressedSound getSound(String mode) {
        return this.soundProperties.getProperties(mode);
    }

    @Override public boolean overridesDefaults() { return true; }

    @Override
    public RFEItemAttachmentProperties.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.AttachmentSlots.SUPPRESSOR;
    }

    public record SuppressedSound(@Nullable SoundEvent firingSound, float audibleRange) {
        public static final MapCodec<SuppressedSound> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                ResourceLocation.CODEC.xmap(SoundEvent::createVariableRangeEvent, SoundEvent::getLocation)
                        .optionalFieldOf("firing_sound").forGetter(p -> Optional.ofNullable(p.firingSound)),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("range", 0f).forGetter(SuppressedSound::audibleRange)
        ).apply(o, (opSound, range) -> new SuppressedSound(opSound.orElse(null), range)));

        public static final StreamCodec<RegistryFriendlyByteBuf, SuppressedSound> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(SoundEvent.DIRECT_STREAM_CODEC).map(o -> o.orElse(null), Optional::ofNullable), SuppressedSound::firingSound,
                ByteBufCodecs.FLOAT, SuppressedSound::audibleRange,
                SuppressedSound::new);
    }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<SuppressorAttachmentProperties> {
        private static final MapCodec<SuppressorAttachmentProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEFirearmProperties.makeCodec(SuppressedSound.CODEC).fieldOf("sound").forGetter(SuppressorAttachmentProperties::soundProperties)
        ).apply(o, SuppressorAttachmentProperties::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SuppressorAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                RFEFirearmProperties.makeStreamCodec(SuppressedSound.STREAM_CODEC), SuppressorAttachmentProperties::soundProperties,
                SuppressorAttachmentProperties::new);

        @Override public MapCodec<SuppressorAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SuppressorAttachmentProperties> streamCodec() { return STREAM_CODEC; }
    }

}
