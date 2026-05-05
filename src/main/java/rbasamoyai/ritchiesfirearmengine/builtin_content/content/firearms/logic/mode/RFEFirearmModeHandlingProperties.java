package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;

public record RFEFirearmModeHandlingProperties(float movementSpeedModifier, ChargingBehavior chargingBehavior, float heatCapacity,
                                               float heatRemovedPerTick, float heatRemovedOnCharge, float heatAddedOnFiring,
                                               int coolingDelayTime, ImmutableList<RFEMisfire> misfires) {

    public static final Codec<RFEFirearmModeHandlingProperties> CODEC = RecordCodecBuilder.create(o -> o.group(
            Codec.floatRange(-1f, 10f).optionalFieldOf("movement_speed_modifier", 0f).forGetter(RFEFirearmModeHandlingProperties::movementSpeedModifier),
            StringRepresentable.fromEnum(ChargingBehavior::values).optionalFieldOf("charging_behavior", ChargingBehavior.HOLD).forGetter(RFEFirearmModeHandlingProperties::chargingBehavior),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_capacity", 0f).forGetter(RFEFirearmModeHandlingProperties::heatCapacity),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_per_tick", 0f).forGetter(RFEFirearmModeHandlingProperties::heatRemovedPerTick),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_removed_on_charge", 0f).forGetter(RFEFirearmModeHandlingProperties::heatRemovedOnCharge),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("heat_added_on_firing", 0f).forGetter(RFEFirearmModeHandlingProperties::heatAddedOnFiring),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("cooling_delay_time", 0).forGetter(RFEFirearmModeHandlingProperties::coolingDelayTime),
            RFEMisfire.LIST_CODEC.xmap(li -> ImmutableList.<RFEMisfire>builder().addAll(li).build(), Lists::newArrayList)
                    .optionalFieldOf("misfire_chances", ImmutableList.of()).forGetter(RFEFirearmModeHandlingProperties::misfires)
    ).apply(o, RFEFirearmModeHandlingProperties::new));

    public static final StreamCodec<FriendlyByteBuf, RFEFirearmModeHandlingProperties> STREAM_CODEC =
            StreamCodec.of(RFEFirearmModeHandlingProperties::toNetwork, RFEFirearmModeHandlingProperties::fromNetwork);

    public static RFEFirearmModeHandlingProperties fromItemDefinition(RFEFirearmModeBuilder builder) {
        return new RFEFirearmModeHandlingProperties(builder.movementSpeedModifier, builder.chargingBehavior, builder.heatCapacity,
                builder.heatRemovedPerTick, builder.heatRemovedOnCharge, builder.heatAddedOnFiring, builder.coolingDelayTime,
                ImmutableList.<RFEMisfire>builder().addAll(builder.misfires).build());
    }

    public static RFEFirearmModeHandlingProperties fromNetwork(FriendlyByteBuf buf) {
        float movementSpeedMultiplier = buf.readFloat();
        ChargingBehavior chargingBehavior = buf.readEnum(ChargingBehavior.class);
        float heatCapacity = buf.readFloat();
        float heatRemovedPerTick = buf.readFloat();
        float heatRemovedOnCharge = buf.readFloat();
        float heatAddedOnFiring = buf.readFloat();
        int coolingDelayTime = buf.readVarInt();
        int misfiresSz = buf.readVarInt();
        ImmutableList.Builder<RFEMisfire> misfires = ImmutableList.builder();
        for (int i = 0; i < misfiresSz; ++i) {
            ResourceLocation id = buf.readResourceLocation();
            float chance = buf.readFloat();
            misfires.add(RFEContentBuilderRegistry.getMisfireProvider(id).apply(chance));
        }
        return new RFEFirearmModeHandlingProperties(movementSpeedMultiplier, chargingBehavior, heatCapacity, heatRemovedPerTick,
                heatRemovedOnCharge, heatAddedOnFiring, coolingDelayTime, misfires.build());
    }

    public static void toNetwork(FriendlyByteBuf buf, RFEFirearmModeHandlingProperties properties) {
        buf.writeFloat(properties.movementSpeedModifier);
        buf.writeEnum(properties.chargingBehavior)
                .writeFloat(properties.heatCapacity)
                .writeFloat(properties.heatRemovedOnCharge)
                .writeFloat(properties.heatRemovedOnCharge)
                .writeFloat(properties.heatAddedOnFiring);
        buf.writeVarInt(properties.coolingDelayTime)
                .writeVarInt(properties.misfires.size());
        for (RFEMisfire misfire : properties.misfires) {
            buf.writeResourceLocation(RFEContentBuilderRegistry.getMisfireProviderId(misfire.getMisfireProvider()))
                    .writeFloat(misfire.getChance());
        }
    }

}
