package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;

import java.util.function.Function;

public record ScopeAttachmentProperties(boolean overrideScopeDefaults, ImmutableList<Float> zoomLevels, boolean blocksSpeedloaders) implements RFEItemAttachmentProperties {

    public static final ScopeAttachmentProperties DONT_OVERRIDE_DEFAULTS = new ScopeAttachmentProperties(false, ImmutableList.of(), false);

    @Override
    public RFEItemAttachmentProperties.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.AttachmentSlots.SCOPE;
    }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<ScopeAttachmentProperties> {
        private static final MapCodec<ScopeAttachmentProperties> OVERRIDE_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.mapEither(Codec.FLOAT.fieldOf("zoom"),
                                Codec.FLOAT.listOf(1, Integer.MAX_VALUE).xmap(ImmutableList::copyOf, Function.identity()).fieldOf("zoom_levels"))
                        .xmap(either -> Either.unwrap(either.mapLeft(ImmutableList::of)), Either::right)
                        .forGetter(ScopeAttachmentProperties::zoomLevels),
                Codec.BOOL.optionalFieldOf("blocks_speedloaders", true).forGetter(ScopeAttachmentProperties::blocksSpeedloaders)
        ).apply(o, (zoomLevels, blocksSpeedloaders) -> new ScopeAttachmentProperties(true, zoomLevels, blocksSpeedloaders)));

        private static final MapCodec<ScopeAttachmentProperties> CODEC = Codec.mapEither(OVERRIDE_CODEC, MapCodec.unit(DONT_OVERRIDE_DEFAULTS)).xmap(Either::unwrap, Either::left);

        private static final StreamCodec<RegistryFriendlyByteBuf, ScopeAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ScopeAttachmentProperties::overrideScopeDefaults,
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).map(ImmutableList::copyOf, Function.identity()), ScopeAttachmentProperties::zoomLevels,
                ByteBufCodecs.BOOL, ScopeAttachmentProperties::blocksSpeedloaders,
                ScopeAttachmentProperties::new);

        @Override public MapCodec<ScopeAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ScopeAttachmentProperties> streamCodec() { return STREAM_CODEC; }

    }

}
