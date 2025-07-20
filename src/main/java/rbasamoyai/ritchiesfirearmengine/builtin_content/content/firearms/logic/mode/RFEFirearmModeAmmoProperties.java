package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;

import javax.annotation.Nullable;

public record RFEFirearmModeAmmoProperties(ImmutableMap<AmmoPredicate, RFEProjectileType> primaryAmmo, ImmutableList<AmmoPredicate> magazines,
                                           ImmutableList<AmmoPredicate> speedloaders, ImmutableList<AmmoPredicate> secondaryAmmo,
                                           @Nullable RFEProjectileType unlimitedProjectile) {

    public ImmutableCollection<AmmoPredicate> primaryAmmoPredicates() { return this.primaryAmmo.keySet(); }

}
