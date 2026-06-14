package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.creative_mode_tab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;

public record RFECreativeModeTabMetadata(List<ResourceLocation> order, int priority) {

    public static Codec<RFECreativeModeTabMetadata> codec(String namespace) {
        if (!ResourceLocation.isValidNamespace(namespace))
            throw new IllegalStateException("Not a valid namespace: " + namespace);
        return RecordCodecBuilder.create(o -> o.group(
                Codec.STRING.comapFlatMap(s -> {
                    try {
                        return DataResult.success(ResourceLocation.fromNamespaceAndPath(namespace, s));
                    } catch (Exception e) {
                        return DataResult.error(() -> "Error processing RFE creative tab metadata order: " + e.getMessage());
                    }
                }, ResourceLocation::getPath).listOf()
                .comapFlatMap(li -> {
                    Set<ResourceLocation> set = new LinkedHashSet<>(li);
                    if (set.size() < li.size())
                        return DataResult.error(() -> "Duplicate entries in RFE creative tab metadata order");
                    return DataResult.success(li);
                }, Function.identity()).fieldOf("order").forGetter(RFECreativeModeTabMetadata::order),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(RFECreativeModeTabMetadata::priority)
        ).apply(o, RFECreativeModeTabMetadata::new));
    }

    public static RFECreativeModeTabMetadata createDefault(String namespace, Collection<String> tabNames) {
        List<ResourceLocation> order = new ArrayList<>();
        for (String tabName : tabNames)
            order.add(ResourceLocation.fromNamespaceAndPath(namespace, tabName));
        return new RFECreativeModeTabMetadata(order, 0);
    }

}
