package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;

import java.util.ArrayList;
import java.util.List;

public record RFEFirearmModeHandlingProperties(float movementSpeedModifier, ChargingBehavior chargingBehavior, float heatCapacity,
                                               float heatRemovedPerTick, float heatRemovedOnCharge, float heatAddedOnFiring,
                                               int coolingDelayTime, ImmutableList<RFEMisfire> misfires) {

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

    public static class Builder {
        protected float movementSpeedModifier = 1;
        protected ChargingBehavior chargingBehavior = ChargingBehavior.HOLD;
        protected float heatCapacity = 0;
        protected float heatRemovedPerTick = 0;
        protected float heatRemovedOnCharge = 0;
        protected float heatAddedOnFiring = 0;
        protected int coolingDelayTime = 0;
        protected final List<RFEMisfire> misfires = new ArrayList<>();

        public Builder movementSpeedModifier(float movementSpeedModifier) {
            this.movementSpeedModifier = Mth.clamp(movementSpeedModifier, -1, 10);
            return this;
        }

        public Builder chargingBehavior(ChargingBehavior chargingBehavior) {
            this.chargingBehavior = chargingBehavior;
            return this;
        }

        public Builder heatCapacity(float heatCapacity) {
            if (heatCapacity < 0)
                throw new IllegalStateException("Cannot specify cooldown time less than 0");
            this.heatCapacity = heatCapacity;
            return this;
        }

        public Builder heatRemovedPerTick(float heatRemovedPerTick) {
            if (heatRemovedPerTick < 0)
                throw new IllegalStateException("Cannot specify default heat removed per tick less than 0");
            this.heatRemovedPerTick = heatRemovedPerTick;
            return this;
        }

        public Builder heatRemovedOnCharge(float heatRemovedOnCharge) {
            if (heatRemovedOnCharge < 0)
                throw new IllegalStateException("Cannot specify default heat removed on charge less than 0");
            this.heatRemovedOnCharge = heatRemovedOnCharge;
            return this;
        }

        public Builder heatAddedOnFiring(float heatAddedOnFiring) {
            if (heatAddedOnFiring < 0)
                throw new IllegalStateException("Cannot specify default heat added on firing less than 0");
            this.heatAddedOnFiring = heatAddedOnFiring;
            return this;
        }

        public Builder coolingDelay(int coolingDelayTime) {
            if (coolingDelayTime < 0)
                throw new IllegalStateException("Cannot specify default cooling delay less than 0");
            this.coolingDelayTime = coolingDelayTime;
            return this;
        }

        public Builder addMisfire(RFEMisfire misfire) {
            this.misfires.add(misfire);
            return this;
        }

        public RFEFirearmModeHandlingProperties build() {
            return new RFEFirearmModeHandlingProperties(this.movementSpeedModifier, this.chargingBehavior, this.heatCapacity,
                    this.heatRemovedPerTick, this.heatRemovedOnCharge, this.heatAddedOnFiring, this.coolingDelayTime,
                    ImmutableList.<RFEMisfire>builder().addAll(this.misfires).build());
        }
    }

}
