package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import java.util.List;

public class FirearmModeDataPackProperties {

    private final List<AmmoPredicate> primaryAmmoPredicates;
    private final List<AmmoPredicate> speedloaderAmmoPredicates;
    private final List<AmmoPredicate> magazineAmmoPredicates;
    private final List<AmmoPredicate> secondaryAmmoPredicates;

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
        this.primaryAmmoPredicates = builder.primaryAmmoPredicates;
        this.speedloaderAmmoPredicates = builder.speedloaderAmmoPredicates;
        this.magazineAmmoPredicates = builder.magazineAmmoPredicates;
        this.secondaryAmmoPredicates = builder.secondaryAmmoPredicates;

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

    public List<AmmoPredicate> primaryAmmoPredicates() { return this.primaryAmmoPredicates; }
    public List<AmmoPredicate> speedloaderAmmoPredicates() { return this.speedloaderAmmoPredicates; }
    public List<AmmoPredicate> magazineAmmoPredicates() { return this.magazineAmmoPredicates; }
    public List<AmmoPredicate> secondaryAmmoPredicates() { return this.secondaryAmmoPredicates; }

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
