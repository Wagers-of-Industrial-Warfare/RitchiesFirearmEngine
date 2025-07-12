package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * General repository for item info providers.
 */
public class RFEHudItemInfoProviders {

    private static final List<RFEAmmoInfoProvider> AMMO_PROVIDERS = new LinkedList<>();

    public static void registerAmmoProvider(RFEAmmoInfoProvider prov) { AMMO_PROVIDERS.add(prov); }

    @Nullable
    public static List<ItemStack> getAmmoStacksFromItem(ItemStack itemStack) {
        for (RFEAmmoInfoProvider prov : AMMO_PROVIDERS) {
            List<ItemStack> ammo = prov.apply(itemStack);
            if (ammo != null)
                return ammo;
        }
        return null;
    }

    @FunctionalInterface
    public interface RFEAmmoInfoProvider extends Function<ItemStack, List<ItemStack>> {
    }

    private static final List<RFEAmmoInventoryCountProvider> AMMO_INVENTORY_COUNT_PROVIDERS = new LinkedList<>();

    public static void registerAmmoInventoryCountProvider(RFEAmmoInventoryCountProvider prov) { AMMO_INVENTORY_COUNT_PROVIDERS.add(prov); }

    public static int getAmmoInventoryCount(ItemStack itemStack, LivingEntity entity) {
        List<ItemStack> items = RFEItemUtils.getEntityInventory(entity);
        for (RFEAmmoInventoryCountProvider prov : AMMO_INVENTORY_COUNT_PROVIDERS) {
            Optional<Integer> op = prov.apply(itemStack, items);
            if (op.isPresent())
                return op.get();
        }
        return 0;
    }

    /**
     * Return {@code Optional.of(-1)} to mark ammo count as infinite.
     */
    @FunctionalInterface
    public interface RFEAmmoInventoryCountProvider extends BiFunction<ItemStack, List<ItemStack>, Optional<Integer>> {
    }

    private RFEHudItemInfoProviders() {}

}
