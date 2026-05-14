package rbasamoyai.ritchiesfirearmengine.utils;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RFEItemUtils {

    public interface EntityItemHandler {
        boolean consumeFromInventory(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess);
        boolean addToInventory(LivingEntity entity, ItemStack itemStack);
    }

    private static final List<EntityItemHandler> GENERAL_ITEM_HANDLERS = new ReferenceArrayList<>();
    private static final Multimap<EntityType<?>, EntityItemHandler> TYPED_ITEM_HANDLERS = LinkedHashMultimap.create();

    public static void registerGeneralItemHandler(EntityItemHandler handler) {
        GENERAL_ITEM_HANDLERS.add(handler);
    }

    public static void registerItemHandlerForType(EntityType<?> type, EntityItemHandler handler) {
        TYPED_ITEM_HANDLERS.put(type, handler);
    }

    public static void addItemToEntity(ItemStack itemStack, LivingEntity entity) {
        for (EntityItemHandler generalHandler : GENERAL_ITEM_HANDLERS) {
            if (generalHandler.addToInventory(entity, itemStack))
                return;
        }
        for (EntityItemHandler typedHandler : TYPED_ITEM_HANDLERS.get(entity.getType())) {
            if (typedHandler.addToInventory(entity, itemStack))
                return;
        }
        Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), itemStack);
    }

    public static void consumeItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op) {
        consumeItemsFromEntity(entity, predicate, op, () -> true);
    }

    public static void consumeItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op,
                                              Supplier<Boolean> breakOnSuccess) {
        for (EntityItemHandler generalHandler : GENERAL_ITEM_HANDLERS) {
            if (generalHandler.consumeFromInventory(entity, predicate, op, breakOnSuccess))
                return;
        }
        for (EntityItemHandler typedHandler : TYPED_ITEM_HANDLERS.get(entity.getType())) {
            if (typedHandler.consumeFromInventory(entity, predicate, op, breakOnSuccess))
                return;
        }
    }

    /**
     * Set maxCount to 0 to take as many items as possible.
     */
    public static List<ItemStack> getItemsFromEntity(LivingEntity entity, Predicate<ItemStack> predicate, int maxCount, boolean take) {
        List<ItemStack> list = new LinkedList<>();
        consumeItemsFromEntity(entity, predicate, s -> {
            int takeAmount = maxCount > 0 ? Math.min(maxCount - countItems(list), s.getMaxStackSize()) : s.getCount();
            ItemStack addition;
            if (take) {
                addition = s.split(takeAmount);
            } else {
                addition = s.copyWithCount(takeAmount);
            }
            FirearmDataUtils.addAmmo(list, addition, false, false);
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
            list.add(s);
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

    public static int countItemsIncludingSlots(List<ItemStack> items) {
        int count = 0;
        for (ItemStack itemStack : items)
            count += itemStack.isEmpty() ? 1 : itemStack.getCount();
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
                return take && !itemStack.is(RFEItemTags.INFINITE_AMMO.tag) ? itemStack : itemStack.copy();
            if (ammoCount > bestCount || ammoCount <= largestCount)
                continue;
            largestCount = ammoCount;
            bestStack = itemStack;
        }
        return take && !bestStack.is(RFEItemTags.INFINITE_AMMO.tag) ? bestStack.split(bestStack.getCount()) : bestStack.copy();
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
        return take && !bestStack.is(RFEItemTags.INFINITE_AMMO.tag) ? bestStack.split(bestStack.getCount()) : bestStack.copy();
    }

    private RFEItemUtils() {}

}
