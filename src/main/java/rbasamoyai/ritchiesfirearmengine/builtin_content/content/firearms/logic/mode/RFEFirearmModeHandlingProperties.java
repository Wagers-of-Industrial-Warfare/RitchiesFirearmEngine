package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;

public record RFEFirearmModeHandlingProperties(float movementSpeedModifier, ChargingBehavior chargingBehavior, float heatCapacity,
                                               float heatRemovedPerTick, float heatRemovedOnCharge, float heatAddedOnFiring,
                                               int coolingDelayTime, int maxShots, ImmutableList<RFEMisfire> misfires) {

    public static final Codec<RFEFirearmModeHandlingProperties> CODEC = RecordCodecBuilder.create(o -> o.group(
            Codec.floatRange(-1f, 10f).optionalFieldOf("movement_speed_modifier", 0f).forGetter(RFEFirearmModeHandlingProperties::movementSpeedModifier),
            StringRepresentable.fromEnum(ChargingBehavior::values).optionalFieldOf("charging_behavior", ChargingBehavior.HOLD).forGetter(RFEFirearmModeHandlingProperties::chargingBehavior),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_capacity", 0f).forGetter(RFEFirearmModeHandlingProperties::heatCapacity),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_per_tick", 0f).forGetter(RFEFirearmModeHandlingProperties::heatRemovedPerTick),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_on_charge", 0f).forGetter(RFEFirearmModeHandlingProperties::heatRemovedOnCharge),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_added_on_firing", 0f).forGetter(RFEFirearmModeHandlingProperties::heatAddedOnFiring),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("cooling_delay_time", 0).forGetter(RFEFirearmModeHandlingProperties::coolingDelayTime),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max_shots", 0).forGetter(RFEFirearmModeHandlingProperties::maxShots),
            RFEMisfire.LIST_CODEC.xmap(li -> ImmutableList.<RFEMisfire>builder().addAll(li).build(), Lists::newArrayList)
                    .optionalFieldOf("misfire_chances", ImmutableList.of()).forGetter(RFEFirearmModeHandlingProperties::misfires)
    ).apply(o, RFEFirearmModeHandlingProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEFirearmModeHandlingProperties> STREAM_CODEC = RFEByteBufCodecUtils.composite9(
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::movementSpeedModifier,
        NeoForgeStreamCodecs.enumCodec(ChargingBehavior.class), RFEFirearmModeHandlingProperties::chargingBehavior,
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::heatCapacity,
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::heatRemovedPerTick,
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::heatRemovedOnCharge,
        ByteBufCodecs.FLOAT, RFEFirearmModeHandlingProperties::heatAddedOnFiring,
        ByteBufCodecs.VAR_INT, RFEFirearmModeHandlingProperties::coolingDelayTime,
        ByteBufCodecs.VAR_INT, RFEFirearmModeHandlingProperties::maxShots,
        RFEMisfire.STREAM_CODEC.apply(RFEByteBufCodecUtils.immutableList()), RFEFirearmModeHandlingProperties::misfires,
        RFEFirearmModeHandlingProperties::new);

    public static RFEFirearmModeHandlingProperties fromItemDefinition(RFEFirearmModeBuilder builder) {
        return new RFEFirearmModeHandlingProperties(builder.movementSpeedModifier, builder.chargingBehavior, builder.heatCapacity,
                builder.heatRemovedPerTick, builder.heatRemovedOnCharge, builder.heatAddedOnFiring, builder.coolingDelayTime,
                builder.maxShots, ImmutableList.<RFEMisfire>builder().addAll(builder.misfires).build());
    }

}
