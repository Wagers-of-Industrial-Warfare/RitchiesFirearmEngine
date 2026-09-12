package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public record RFEFirearmModeAmmoProperties(ImmutableMap<AmmoPredicate, RFEProjectileType> primaryAmmo, ImmutableList<AmmoPredicate> magazines,
                                           ImmutableList<AmmoPredicate> speedloaders, ImmutableList<AmmoPredicate> secondaryAmmo,
                                           @Nullable RFEProjectileType unlimitedProjectile, ImmutableList<ItemStack> unlimitedPrimaryReloadItems,
                                           ImmutableList<ItemStack> unlimitedSecondaryReloadItems) {

    public ImmutableCollection<AmmoPredicate> primaryAmmoPredicates() { return this.primaryAmmo.keySet(); }

    public Predicate<ItemStack> extraMagazineItemPredicate() {
        Predicate<ItemStack> basePred = RFEUtils.orAllPredicates(this.magazines);
        return basePred.and(s -> s.getItem() instanceof MagazineItem magazineItem && magazineItem.testInnerData(s, basePred, 16));
    }

    public boolean isValidItem(ItemStack item) {
        if (this.isValidPrimaryAmmo(item))
            return true;
        Predicate<ItemStack> magPred = this.extraMagazineItemPredicate();
        if (magPred.test(item)) {
            MagazineItem magazineItem = (MagazineItem) item.getItem();
            List<ItemStack> storedAmmo = magazineItem.getStoredAmmo(item);
            List<ItemStack> storedPrimers = magazineItem.getStoredSecondaryAmmo(item);
            if (storedAmmo.isEmpty() && storedPrimers.isEmpty())
                return false;
            for (ItemStack magAmmo : storedAmmo) {
                if (!this.isValidPrimaryAmmo(magAmmo))
                    return false;
            }
            for (ItemStack magAmmo : storedPrimers) {
                for (AmmoPredicate secondaryPred : this.secondaryAmmo) {
                    if (!secondaryPred.test(magAmmo))
                        return false;
                }
            }
            return true;
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
        if (FirearmDataUtils.isUsedPrimer(item))
            return false;
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
