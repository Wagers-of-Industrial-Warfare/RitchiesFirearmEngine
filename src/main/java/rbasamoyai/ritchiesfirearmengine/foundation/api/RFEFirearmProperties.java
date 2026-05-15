package rbasamoyai.ritchiesfirearmengine.foundation.api;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

import java.util.LinkedHashMap;

public record RFEFirearmProperties<T>(T defaultProperties, ImmutableMap<String, T> propertiesByMode) {

    public T getProperties(String mode) { return this.propertiesByMode.getOrDefault(mode, this.defaultProperties); }

    public static <T> Codec<RFEFirearmProperties<T>> makeCodec(MapCodec<T> codec) {
        return RecordCodecBuilder.create(o -> o.group(
                codec.forGetter(RFEFirearmProperties::defaultProperties),
                ExtraCodecs.strictUnboundedMap(Codec.STRING, codec.codec())
                        .xmap(ImmutableMap::copyOf, LinkedHashMap::new)
                        .optionalFieldOf("modes", ImmutableMap.of()).forGetter(RFEFirearmProperties::propertiesByMode)
        ).apply(o, RFEFirearmProperties::new));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, RFEFirearmProperties<T>> makeStreamCodec(StreamCodec<RegistryFriendlyByteBuf, T> propertiesStreamCodec) {
        return StreamCodec.composite(
                propertiesStreamCodec, RFEFirearmProperties::defaultProperties,
                RFEByteBufCodecUtils.immutableMap(ByteBufCodecs.STRING_UTF8, propertiesStreamCodec), RFEFirearmProperties::propertiesByMode,
                RFEFirearmProperties::new);
    }

}
