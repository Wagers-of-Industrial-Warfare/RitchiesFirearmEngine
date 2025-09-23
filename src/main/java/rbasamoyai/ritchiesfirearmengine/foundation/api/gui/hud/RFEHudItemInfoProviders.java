package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * General repository for item info providers.
 */
public class RFEHudItemInfoProviders {

    private static final Map<Item, RFEAmmoInfoProvider> AMMO_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    public static void registerAmmoProvider(Item item, RFEAmmoInfoProvider prov) {
        if (AMMO_PROVIDERS.containsKey(item))
            throw new IllegalStateException("Already registered ammo HUD provider for item " + item);
        AMMO_PROVIDERS.put(item, prov);
    }

    @Nullable
    public static List<ItemStack> getAmmoStacksFromItem(ItemStack itemStack) {
        if (!AMMO_PROVIDERS.containsKey(itemStack.getItem()))
            return null;
        return AMMO_PROVIDERS.get(itemStack.getItem()).apply(itemStack);
    }

    /**
     * Return {@code null} to mark infinite ammo in the gun.
     */
    @FunctionalInterface
    public interface RFEAmmoInfoProvider extends Function<ItemStack, List<ItemStack>> {
    }

    private static final Map<Item, RFEAmmoInventoryCountProvider> AMMO_INVENTORY_COUNT_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    public static void registerAmmoInventoryCountProvider(Item item, RFEAmmoInventoryCountProvider prov) {
        if (AMMO_INVENTORY_COUNT_PROVIDERS.containsKey(item))
            throw new IllegalStateException("Already registered ammo inventory count HUD provider for item " + item);
        AMMO_INVENTORY_COUNT_PROVIDERS.put(item, prov);
    }

    public static int getAmmoInventoryCount(ItemStack itemStack, LivingEntity entity, boolean countLooseRounds) {
        if (!AMMO_INVENTORY_COUNT_PROVIDERS.containsKey(itemStack.getItem()))
            return 0;
        List<ItemStack> items = RFEItemUtils.getEntityInventory(entity);
        return AMMO_INVENTORY_COUNT_PROVIDERS.get(itemStack.getItem()).apply(itemStack, items, countLooseRounds).orElse(0);
    }

    /**
     * Return {@code Optional.of(-1)} to mark ammo count as infinite.
     */
    @FunctionalInterface
    public interface RFEAmmoInventoryCountProvider {
        Optional<Integer> apply(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds);
    }

    private static final Map<Item, RFEHeatInfoProvider> HEAT_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    public static void registerHeatProvider(Item item, RFEHeatInfoProvider prov) {
        if (HEAT_PROVIDERS.containsKey(item)) throw new IllegalStateException("Already registered heat HUD provider for item " + item);
        HEAT_PROVIDERS.put(item, prov);
    }

    public static Optional<Float> getHeatFromItem(ItemStack itemStack) {
        if (!HEAT_PROVIDERS.containsKey(itemStack.getItem())) return Optional.empty();
        return HEAT_PROVIDERS.get(itemStack.getItem()).apply(itemStack);
    }

    private static final Map<Item, RFEHeatInfoProvider> HEAT_CAPACITY_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    public static void registerHeatCapacityProvider(Item item, RFEHeatInfoProvider prov) {
        if (HEAT_CAPACITY_PROVIDERS.containsKey(item)) throw new IllegalStateException("Already registered heat capacity HUD provider for item " + item);
        HEAT_CAPACITY_PROVIDERS.put(item, prov);
    }

    public static Optional<Float> getHeatCapacityFromItem(ItemStack itemStack) {
        if (!HEAT_CAPACITY_PROVIDERS.containsKey(itemStack.getItem())) return Optional.empty();
        return HEAT_CAPACITY_PROVIDERS.get(itemStack.getItem()).apply(itemStack);
    }

    /**
     * Return {@code null} to mark infinite ammo in the gun.
     */
    @FunctionalInterface
    public interface RFEHeatInfoProvider extends Function<ItemStack, Optional<Float>> {
    }

    private RFEHudItemInfoProviders() {}

}
