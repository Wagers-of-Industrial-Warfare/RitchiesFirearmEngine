package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

public class FoundationDataComponents {

    private static final Map<ResourceLocation, DataComponentType<?>> DATA_COMPONENT_TYPES = new LinkedHashMap<>();

    public static final DataComponentType<UUID> RECOIL_IDENTIFIER = register("recoil_identifier",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DataComponentType<UUID> SPREAD_IDENTIFIER = register("spread_identifier",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    private static <V> DataComponentType<V> register(String id, UnaryOperator<DataComponentType.Builder<V>> builderOp) {
        ResourceLocation loc = RitchiesFirearmEngine.resource(id);
        if (DATA_COMPONENT_TYPES.containsKey(loc))
            throw new IllegalStateException("Already registered data component type " + loc);
        DataComponentType<V> type = builderOp.apply(DataComponentType.builder()).build();
        DATA_COMPONENT_TYPES.put(loc, type);
        return type;
    }

    public static void register(BiConsumer<ResourceLocation, DataComponentType<?>> registry) { DATA_COMPONENT_TYPES.forEach(registry); }

}
