package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * General repository for item info providers.
 */
public class RFEHudItemInfoProviders {

    private static final Map<Item, RFEHudInfoProvider> AMMO_PROVIDERS = new Reference2ObjectOpenHashMap<>();

    public static void registerHudInfoProvider(Item item, RFEHudInfoProvider prov) {
        if (AMMO_PROVIDERS.containsKey(item))
            throw new IllegalStateException("Already registered HUD info provider for item " + item);
        AMMO_PROVIDERS.put(item, prov);
    }

    @Nullable
    public static RFEHudInfoProvider getHudProvider(ItemStack itemStack) {
        if (!AMMO_PROVIDERS.containsKey(itemStack.getItem()))
            return null;
        return AMMO_PROVIDERS.get(itemStack.getItem());
    }

    public interface RFEHudInfoProvider {
        /**
         * Return {@code null} to mark infinite ammo.
         * @param itemStack the firearm
         * @return the primary ammunition loaded in this gun, or null if infinite ammunition
         */
        @Nullable List<ItemStack> getPrimaryAmmo(ItemStack itemStack);

        /**
         * Return {@code null} to mark infinite ammo.
         * @param itemStack the firearm
         * @return the secondary ammunition loaded in this gun, or null if infinite ammunition
         */
        @Nullable default List<ItemStack> getSecondaryAmmo(ItemStack itemStack) { return List.of(); }

        /**
         * Return {@code Optional.of(-1)} to mark infinite ammo.
         * @param itemStack the firearm
         * @param inventory the inventory the firearm consumes from
         * @param countLooseRounds if loose rounds should be counted
         * @return the amount of primary ammunition in the inventory
         */
        Optional<Integer> countPrimaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds);

        /**
         * Return {@code Optional.of(-1)} to mark infinite ammo.
         * @param itemStack the firearm
         * @param inventory the inventory the firearm consumes from
         * @param countLooseRounds if loose rounds should be counted
         * @return the amount of secondary ammunition in the inventory
         */
        default Optional<Integer> countSecondaryAmmoInInventory(ItemStack itemStack, List<ItemStack> inventory, boolean countLooseRounds) { return Optional.of(0); }

        /**
         * Get the current heat level of the firearm.
         * @param itemStack the firearm
         * @return the heat amount of the firearm
         */
        default float getHeatAmount(ItemStack itemStack) { return 0f; }

        /**
         * Get the current heat capacity of the firearm.
         * @param itemStack the firearm
         * @return the heat capacity of the firearm
         */
        default float getHeatCapacity(ItemStack itemStack) { return 1f; }

        /**
         * Get if the gun is melee mode.
         * @param itemStack the firearm
         * @return true if the firearm is meleeing, false if not
         */
        default boolean isMeleeing(ItemStack itemStack, LivingEntity entity) { return false; }
    }

    private RFEHudItemInfoProviders() {}

}
