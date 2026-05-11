package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

public record RFEFirearmModeHandlingProperties(float movementSpeedModifier, ChargingBehavior chargingBehavior,
                                               RFEFirearmHeatProperties heatProperties, int maxShots, float pitchAdjustment,
                                               ImmutableList<RFEMisfire> misfires) {

    public static final Codec<RFEFirearmModeHandlingProperties> CODEC = RecordCodecBuilder.create(o -> o.group(
            Codec.floatRange(-1f, 10f).optionalFieldOf("movement_speed_modifier", 0f).forGetter(RFEFirearmModeHandlingProperties::movementSpeedModifier),
            StringRepresentable.fromEnum(ChargingBehavior::values).optionalFieldOf("charging_behavior", ChargingBehavior.HOLD).forGetter(RFEFirearmModeHandlingProperties::chargingBehavior),
            RFEFirearmHeatProperties.CODEC.forGetter(RFEFirearmModeHandlingProperties::heatProperties),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max_shots", 0).forGetter(RFEFirearmModeHandlingProperties::maxShots),
            Codec.floatRange(-90f, 90f).optionalFieldOf("pitch_adjustment", 0f).forGetter(RFEFirearmModeHandlingProperties::pitchAdjustment),
            RFEMisfire.LIST_CODEC.xmap(li -> ImmutableList.<RFEMisfire>builder().addAll(li).build(), Lists::newArrayList)
                    .optionalFieldOf("misfire_chances", ImmutableList.of()).forGetter(RFEFirearmModeHandlingProperties::misfires)
    ).apply(o, RFEFirearmModeHandlingProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEFirearmModeHandlingProperties> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::movementSpeedModifier,
        NeoForgeStreamCodecs.enumCodec(ChargingBehavior.class), RFEFirearmModeHandlingProperties::chargingBehavior,
        RFEFirearmHeatProperties.STREAM_CODEC, RFEFirearmModeHandlingProperties::heatProperties,
        ByteBufCodecs.VAR_INT, RFEFirearmModeHandlingProperties::maxShots,
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::pitchAdjustment,
        RFEMisfire.STREAM_CODEC.apply(RFEByteBufCodecUtils.immutableList()), RFEFirearmModeHandlingProperties::misfires,
        RFEFirearmModeHandlingProperties::new);

    public static RFEFirearmModeHandlingProperties fromItemDefinition(RFEFirearmModeBuilder builder) {
        return new RFEFirearmModeHandlingProperties(builder.movementSpeedModifier, builder.chargingBehavior,
                new RFEFirearmHeatProperties(builder.heatCapacity, builder.heatRemovedPerTick, builder.heatRemovedOnCharge,
                        builder.heatAddedOnFiring, builder.coolingDelayTime),
                builder.maxShots, builder.pitchAdjustment, ImmutableList.<RFEMisfire>builder().addAll(builder.misfires).build());
    }

    public record RFEFirearmHeatProperties(float heatCapacity, float heatRemovedPerTick, float heatRemovedOnCharge,
                                           float heatAddedOnFiring, int coolingDelayTime) {
        private static final MapCodec<RFEFirearmHeatProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_capacity", 0f).forGetter(RFEFirearmHeatProperties::heatCapacity),
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_per_tick", 0f).forGetter(RFEFirearmHeatProperties::heatRemovedPerTick),
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_on_charge", 0f).forGetter(RFEFirearmHeatProperties::heatRemovedOnCharge),
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_added_on_firing", 0f).forGetter(RFEFirearmHeatProperties::heatAddedOnFiring),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("cooling_delay_time", 0).forGetter(RFEFirearmHeatProperties::coolingDelayTime)
        ).apply(o, RFEFirearmHeatProperties::new));

        private static final StreamCodec<ByteBuf, RFEFirearmHeatProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, RFEFirearmHeatProperties::heatCapacity,
                ByteBufCodecs.FLOAT, RFEFirearmHeatProperties::heatRemovedPerTick,
                ByteBufCodecs.FLOAT, RFEFirearmHeatProperties::heatRemovedOnCharge,
                ByteBufCodecs.FLOAT, RFEFirearmHeatProperties::heatAddedOnFiring,
                ByteBufCodecs.VAR_INT, RFEFirearmHeatProperties::coolingDelayTime,
                RFEFirearmHeatProperties::new);
    }

}
