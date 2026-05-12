package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;

import javax.annotation.Nullable;
import java.util.List;

public record RFEFirearmModeAmmoProperties(ImmutableMap<AmmoPredicate, RFEProjectileType> primaryAmmo, ImmutableList<AmmoPredicate> magazines,
                                           ImmutableList<AmmoPredicate> speedloaders, ImmutableList<AmmoPredicate> secondaryAmmo,
                                           @Nullable RFEProjectileType unlimitedProjectile) {

    public ImmutableCollection<AmmoPredicate> primaryAmmoPredicates() { return this.primaryAmmo.keySet(); }

    public boolean isValidItem(ItemStack item) {
        if (this.isValidPrimaryAmmo(item))
            return true;
        for (AmmoPredicate magPred : this.magazines) {
            if (magPred.test(item)) {
                if (!(item.getItem() instanceof MagazineItem magazineItem))
                    return false;
                List<ItemStack> storedAmmo = magazineItem.getStoredAmmo(item);
                if (storedAmmo.isEmpty())
                    return false;
                for (ItemStack magAmmo : storedAmmo) {
                    if (!this.isValidPrimaryAmmo(magAmmo))
                        return false;
                }
                return true;
            }
        }
        for (AmmoPredicate loaderPred : this.speedloaders) {
            if (loaderPred.test(item)) {
                if (!(item.getItem() instanceof MagazineItem speedloaderItem))
                    return false;
                List<ItemStack> storedAmmo = speedloaderItem.getStoredAmmo(item);
                if (storedAmmo.isEmpty())
                    return false;
                for (ItemStack magAmmo : storedAmmo) {
                    if (!this.isValidPrimaryAmmo(magAmmo))
                        return false;
                }
                return true;
            }
        }
        for (AmmoPredicate secondaryPred : this.secondaryAmmo) {
            if (secondaryPred.test(item))
                return true;
        }
        return false;
    }

    public boolean isValidPrimaryAmmo(ItemStack item) {
        for (AmmoPredicate pred : this.primaryAmmo.keySet()) {
            if (pred.test(item))
                return true;
        }
        return false;
    }

}
