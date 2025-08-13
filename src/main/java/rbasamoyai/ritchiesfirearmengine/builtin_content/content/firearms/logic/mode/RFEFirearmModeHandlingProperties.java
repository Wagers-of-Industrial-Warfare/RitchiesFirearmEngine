package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import net.minecraft.network.FriendlyByteBuf;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.ChargingBehavior;

public record RFEFirearmModeHandlingProperties(float jamChance, ChargingBehavior chargingBehavior, float heatCapacity,
                                               float heatRemovedPerTick, float heatRemovedOnCharge, float heatAddedOnFiring,
                                               int coolingDelayTime) {

    public static RFEFirearmModeHandlingProperties fromItemDefinition(RFEFirearmModeBuilder builder) {
        return new RFEFirearmModeHandlingProperties(builder.jamChance, builder.chargingBehavior, builder.heatCapacity,
                builder.heatRemovedPerTick, builder.heatRemovedOnCharge, builder.heatAddedOnFiring, builder.coolingDelayTime);
    }

    public static RFEFirearmModeHandlingProperties fromNetwork(FriendlyByteBuf buf) {
        float jamChance = buf.readFloat();
        ChargingBehavior chargingBehavior = buf.readEnum(ChargingBehavior.class);
        float heatCapacity = buf.readFloat();
        float heatRemovedPerTick = buf.readFloat();
        float heatRemovedOnCharge = buf.readFloat();
        float heatAddedOnFiring = buf.readFloat();
        int coolingDelayTime = buf.readVarInt();
        return new RFEFirearmModeHandlingProperties(jamChance, chargingBehavior, heatCapacity, heatRemovedPerTick,
                heatRemovedOnCharge, heatAddedOnFiring, coolingDelayTime);
    }

    public static void toNetwork(FriendlyByteBuf buf, RFEFirearmModeHandlingProperties properties) {
        buf.writeFloat(properties.jamChance);
        buf.writeEnum(properties.chargingBehavior)
                .writeFloat(properties.heatCapacity)
                .writeFloat(properties.heatRemovedOnCharge)
                .writeFloat(properties.heatRemovedOnCharge)
                .writeFloat(properties.heatAddedOnFiring);
        buf.writeVarInt(properties.coolingDelayTime);
    }

    public static class Builder {
        protected float jamChance = 0;
        protected ChargingBehavior chargingBehavior = ChargingBehavior.HOLD;
        protected float heatCapacity = 0;
        protected float heatRemovedPerTick = 0;
        protected float heatRemovedOnCharge = 0;
        protected float heatAddedOnFiring = 0;
        protected int coolingDelayTime = 0;

        public Builder jamChance(float jamChance) {
            if (jamChance < 0 || 1 < jamChance)
                throw new IllegalStateException("Cannot specify jam chance less than 0 or greater than 1");
            this.jamChance = jamChance;
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

        public RFEFirearmModeHandlingProperties build() {
            return new RFEFirearmModeHandlingProperties(this.jamChance, this.chargingBehavior, this.heatCapacity,
                    this.heatRemovedPerTick, this.heatRemovedOnCharge, this.heatAddedOnFiring, this.coolingDelayTime);
        }
    }

}
