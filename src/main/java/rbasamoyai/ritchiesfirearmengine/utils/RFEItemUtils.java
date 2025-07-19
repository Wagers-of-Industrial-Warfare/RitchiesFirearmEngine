package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RFEItemUtils {

    public static void addItemToEntity(ItemStack itemStack, LivingEntity entity) {
        if (entity instanceof Player player) {
            player.getInventory().placeItemBackInInventory(itemStack);
            return;
        }
        // TODO item handlers
        Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), itemStack);
    }

    public static void consumeItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op) {
        consumeItemsFromEntity(entity, predicate, op, () -> true);
    }

    public static void consumeItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op,
                                              Supplier<Boolean> breakOnSuccess) {
        // TODO item handlers
        ItemStack offhandStack = entity.getOffhandItem();
        if (predicate.test(offhandStack)) {
            ItemStack result = op.apply(offhandStack);
            entity.setItemInHand(InteractionHand.OFF_HAND, result);
            if (breakOnSuccess.get())
                return;
        }
        if (entity instanceof Player player) {
            for (ListIterator<ItemStack> lister = player.getInventory().items.listIterator(); lister.hasNext(); ) {
                ItemStack invStack = lister.next();
                if (!predicate.test(invStack))
                    continue;
                ItemStack result = op.apply(invStack);
                lister.set(result);
                if (breakOnSuccess.get())
                    return;
            }
        }
    }

    /**
     * Set maxCount to 0 to take as many items as possible.
     */
    public static List<ItemStack> getItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, int maxCount, boolean take) {
        List<ItemStack> list = new LinkedList<>();
        consumeItemsFromEntity(entity, predicate, s -> {
            int takeAmount = maxCount > 0 ? Math.min(maxCount - countItems(list), s.getMaxStackSize()) : s.getMaxStackSize();
            ItemStack addition;
            if (take) {
                addition = s.split(takeAmount);
            } else {
                addition = s.copyWithCount(takeAmount);
            }
            FirearmDataUtils.addAmmo(list, addition, false);
            return s.isEmpty() ? ItemStack.EMPTY : s;
        }, () -> maxCount > 0 && list.size() >= maxCount);
        return list;
    }

    /**
     * Returns the same item stack objects.
     */
    private static List<ItemStack> getDirectItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate) {
        List<ItemStack> list = new LinkedList<>();
        consumeItemsFromEntity(entity, predicate, s -> {
            list.add(0, s);
            return s.isEmpty() ? ItemStack.EMPTY : s;
        }, () -> false);
        return list;
    }

    public static List<ItemStack> getEntityInventory(LivingEntity entity) {
        // TODO handlers
        List<ItemStack> list = new LinkedList<>();
        ItemStack offhandStack = entity.getOffhandItem();
        if (!offhandStack.isEmpty())
            list.add(offhandStack);
        if (entity instanceof Player player) {
            for (ItemStack itemStack : player.getInventory().items) {
                if (!itemStack.isEmpty())
                    list.add(itemStack);
            }
        }
        return list;
    }

    public static int countItems(List<ItemStack> items) {
        int count = 0;
        for (ItemStack itemStack : items)
            count += itemStack.getCount();
        return count;
    }

    /**
     * Set best to 0 to return nothing.
     */
    public static ItemStack findBestSpeedloader(LivingEntity entity, Predicate<ItemStack> speedloaderPredicate,
                                                Predicate<ItemStack> ammoPredicate, int bestCount, boolean take) {
        if (bestCount <= 0)
            return ItemStack.EMPTY;
        List<ItemStack> list = getDirectItemsFromEntity(entity, speedloaderPredicate);
        if (list.isEmpty())
            return ItemStack.EMPTY;
        int largestCount = 0;
        ItemStack bestStack = ItemStack.EMPTY;
        for (ItemStack itemStack : list) {
            if (!(itemStack.getItem() instanceof MagazineItem magazineItem))
                continue;
            List<ItemStack> storedAmmo = magazineItem.getStoredAmmo(itemStack);
            if (storedAmmo.isEmpty())
                continue;
            boolean success = true;
            for (ItemStack ammo : storedAmmo) {
                if (!ammoPredicate.test(ammo)) {
                    success = false;
                    break;
                }
            }
            if (!success)
                continue;
            int ammoCount = countItems(storedAmmo);
            if (ammoCount == bestCount)
                return itemStack;
            if (ammoCount <= largestCount)
                continue;
            largestCount = ammoCount;
            bestStack = itemStack;
        }
        return take ? bestStack.split(bestStack.getCount()) : bestStack.copy();
    }

    public static ItemStack findFullestMagazine(LivingEntity entity, Predicate<ItemStack> magazinePredicate, Predicate<ItemStack> ammoPredicate, boolean take) {
        List<ItemStack> list = getDirectItemsFromEntity(entity, magazinePredicate);
        if (list.isEmpty())
            return ItemStack.EMPTY;
        ItemStack bestStack = ItemStack.EMPTY;
        int largestCount = 0;
        for (ItemStack itemStack : list) {
            if (!(itemStack.getItem() instanceof MagazineItem magazineItem))
                continue;
            List<ItemStack> storedAmmo = magazineItem.getStoredAmmo(itemStack);
            boolean success = true;
            for (ItemStack ammo : storedAmmo) {
                if (!ammoPredicate.test(ammo)) {
                    success = false;
                    break;
                }
            }
            if (!success)
                continue;
            int count = countItems(storedAmmo);
            if (count > largestCount) {
                largestCount = count;
                bestStack = itemStack;
            }
        }
        return take ? bestStack.split(bestStack.getCount()) : bestStack.copy();
    }

    private RFEItemUtils() {}

}
