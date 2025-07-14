package rbasamoyai.ritchiesfirearmengine.foundation.api;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record RFEFirearmProperties<T>(T defaultProperties, ImmutableMap<String, T> propertiesByMode) {

    public T getProperties(String mode) { return this.propertiesByMode.getOrDefault(mode, this.defaultProperties); }

    public static <T> void toNetwork(FriendlyByteBuf buf, RFEFirearmProperties<T> properties, BiConsumer<FriendlyByteBuf, T> encoder) {
        encoder.accept(buf, properties.defaultProperties);
        buf.writeVarInt(properties.propertiesByMode.size());
        for (Map.Entry<String, T> entry : properties.propertiesByMode.entrySet()) {
            buf.writeUtf(entry.getKey());
            encoder.accept(buf, entry.getValue());
        }
    }

    public static <T> RFEFirearmProperties<T> fromNetwork(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> decoder) {
        T defaultProperties = decoder.apply(buf);
        int sz = buf.readVarInt();
        ImmutableMap.Builder<String, T> propertiesByMode = ImmutableMap.builder();
        for (int i = 0; i < sz; ++i) {
            String mode = buf.readUtf();
            T properties = decoder.apply(buf);
            propertiesByMode.put(mode, properties);
        }
        return new RFEFirearmProperties<>(defaultProperties, propertiesByMode.build());
    }

}
