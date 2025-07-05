package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

public class FirearmModeDataPackProperties {

    private final float spread;
    private final float jamChance;
    private final boolean manualCharging;
    private final float verticalRecoil;
    private final float horizontalRecoil;

    private final float heatCapacity;
    private final float heatRemovedPerTick;
    private final float heatRemovedOnCharge;
    private final float heatAddedOnFiring;
    private final int coolingDelayTime;

    public FirearmModeDataPackProperties(RFEFirearmModeBuilder builder) {
        this.spread = builder.spread;
        this.jamChance = builder.jamChance;
        this.manualCharging = builder.manualCharging;
        this.verticalRecoil = builder.verticalRecoil;
        this.horizontalRecoil = builder.horizontalRecoil;

        this.heatCapacity = builder.heatCapacity;
        this.heatRemovedPerTick = builder.heatRemovedPerTick;
        this.heatRemovedOnCharge = builder.heatRemovedOnCharge;
        this.heatAddedOnFiring = builder.heatAddedOnFiring;
        this.coolingDelayTime = builder.coolingDelayTime;
    }

    public float spread() { return this.spread; }
    public float jamChance() { return this.jamChance; }
    public boolean manualCharging() { return this.manualCharging; }
    public float verticalRecoil() { return this.verticalRecoil; }
    public float horizontalRecoil() { return this.horizontalRecoil; }

    public float heatCapacity() { return this.heatCapacity; }
    public float heatRemovedPerTick() { return this.heatRemovedPerTick; }
    public float heatRemovedOnCharge() { return this.heatRemovedOnCharge; }
    public float heatAddedOnFiring() { return this.heatAddedOnFiring; }
    public int coolingDelayTime() { return this.coolingDelayTime; }

}
