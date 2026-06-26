package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.*;

public class FirearmDataUtils {

    // Charging methods

    public static boolean isCharged(ItemStack itemStack) {
        return isCharged(itemStack.getComponents());
    }

    public static boolean isCharged(DataComponentMap components) {
        return components.has(RFEDataComponents.IS_CHARGED);
    }

    public static boolean isCharged(DataComponentPatch components) {
        Optional<? extends Boolean> o = components.get(RFEDataComponents.IS_CHARGED);
        return o != null && o.isPresent();
    }

    public static void setCharged(ItemStack itemStack, boolean charged) {
        if (charged) {
            itemStack.set(RFEDataComponents.IS_CHARGED, true);
        } else {
            itemStack.remove(RFEDataComponents.IS_CHARGED);
        }
    }

    public static DataComponentPatch setCharged(DataComponentPatch data, boolean charged) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (charged) {
            patched.set(RFEDataComponents.IS_CHARGED, true);
        } else {
            patched.remove(RFEDataComponents.IS_CHARGED);
        }
        return patched.asPatch();
    }

    // Jamming methods

    public static boolean isJammed(ItemStack itemStack) {
        return isJammed(itemStack.getComponentsPatch());
    }

    public static boolean isJammed(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.IS_JAMMED);
        return o != null && o.isPresent();
    }

    public static void setJammed(ItemStack itemStack, boolean jammed) {
        if (jammed) {
            itemStack.set(RFEDataComponents.IS_JAMMED, true);
        } else {
            itemStack.remove(RFEDataComponents.IS_JAMMED);
        }
    }

    public static DataComponentPatch setJammed(DataComponentPatch data, boolean jammed) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (jammed) {
            patched.set(RFEDataComponents.IS_JAMMED, true);
        } else {
            patched.remove(RFEDataComponents.IS_JAMMED);
        }
        return patched.asPatch();
    }

    // Ammunition methods

//    public static ListTag writeAmmoList(List<ItemStack> ammo) {
//        ListTag list = new ListTag();
//        int emptyCount = 0;
//        for (ItemStack itemStack : ammo) {
//            if (itemStack.isEmpty()) {
//                ++emptyCount;
//            } else {
//                while (emptyCount > 0) {
//                    byte count = (byte) Math.min(emptyCount, 64);
//                    emptyTag.putByte("count", count);
//                    emptyCount -= count;
//                    list.add(emptyTag);
//                }
//                list.add(itemStack.save(new CompoundTag()));
//            }
//        }
//        while (emptyCount > 0) {
//            CompoundTag emptyTag = ItemStack.EMPTY.save(new CompoundTag());
//            byte count = (byte) Math.min(emptyCount, 64);
//            emptyTag.putByte("Count", count);
//            emptyCount -= count;
//            list.add(emptyTag);
//        }
//        return list;
//    }

    // TODO figure out compact placeholders

    public static List<ItemStack> readAmmoList(RFEItemContainerContents contents) {
        int sz = contents.getSlots();
        List<ItemStack> list = new LinkedList<>();
        for (int i = 0; i < sz; ++i)
            list.add(contents.getStackInSlot(i));
        return list;
    }

    public static List<ItemStack> getRounds(DataComponentPatch data, DataComponentType<RFEItemContainerContents> type) {
        Optional<? extends RFEItemContainerContents> o = data.get(type);
        if (o == null || o.isEmpty())
            return new LinkedList<>();
        return readAmmoList(o.get());
    }

    public static List<ItemStack> getRounds(ItemStack itemStack, DataComponentType<RFEItemContainerContents> type) {
        return getRounds(itemStack.getComponentsPatch(), type);
    }

    public static void saveRounds(ItemStack itemStack, DataComponentType<RFEItemContainerContents> type, List<ItemStack> ammo) {
        itemStack.set(type, RFEItemContainerContents.fromItems(ammo));
    }

    public static DataComponentPatch saveRounds(DataComponentPatch source, DataComponentType<RFEItemContainerContents> type, List<ItemStack> ammo) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, source);
        patched.set(type, RFEItemContainerContents.fromItems(ammo));
        return patched.asPatch();
    }

    /**
     * Strip the item at either the start or end of the ammo list. If empty slots are tracked and the targeted slot is
     * empty, the result will be empty.
     */
    public static ItemStack stripAmmo(List<ItemStack> ammo, boolean last, boolean simulate, boolean trackEmptySlots) {
        if (ammo.isEmpty())
            return ItemStack.EMPTY;
        int index = last ? ammo.size() - 1 : 0;
        ItemStack next = ammo.get(index);
        ItemStack ret = next.copyWithCount(1);
        if (!simulate) {
            if (!next.isEmpty())
                next.shrink(1);
            if (next.isEmpty()) {
                if (trackEmptySlots) {
                    ammo.set(index, ItemStack.EMPTY);
                } else {
                    ammo.remove(index);
                }
            } else if (trackEmptySlots) {
                ammo.add(index, ItemStack.EMPTY);
            }
        }
        return ret;
    }

    /**
     * Strips multiple ammo. If count is less than 1, no ammo is stripped. Ammo stripping is blocked by empty slots if
     * set to not ignore them.
     */
    public static List<ItemStack> stripMultipleAmmo(List<ItemStack> ammo, int count, boolean last, boolean simulate,
                                                    boolean trackEmptySlots, boolean ignoreEmptySlots) {
        List<ItemStack> stripped = new LinkedList<>();
        if (ammo.isEmpty() || count < 1)
            return stripped;
        ListIterator<ItemStack> lister = last ? ammo.listIterator(ammo.size()) : ammo.listIterator();
        int newCount = count;
        List<Integer> newEmptySpaces = new ArrayList<>();
        while (last ? lister.hasPrevious() : lister.hasNext()) {
            ItemStack stackToRemove = last ? lister.previous() : lister.next();
            if (stackToRemove.isEmpty() && ignoreEmptySlots)
                continue;
            int toRemove = Math.min(newCount, stackToRemove.getCount());
            if (toRemove < 1)
                break;
            stripped.add(stackToRemove.copyWithCount(toRemove));
            if (!simulate) {
                stackToRemove.shrink(toRemove);
                if (trackEmptySlots) {
                    int index = last ? lister.nextIndex() : lister.previousIndex();
                    for (int i = 0; i < toRemove; ++i)
                        newEmptySpaces.add(index);
                }
                if (stackToRemove.isEmpty())
                    lister.remove();
            }
            newCount -= toRemove;
        }
        if (!last)
            Collections.reverse(newEmptySpaces); // Ensure indices are ordered largest to smallest
        for (int i : newEmptySpaces)
            ammo.add(i, ItemStack.EMPTY);
        return stripped;
    }

    /**
     * Does not modify the passed ItemStack. Adds the entire itemStack.
     * If trackEmptySlots is true, the amount of items (including empty slots) in the ammo list is assumed to be the capacity.
     *
     * @return The amount of rounds loaded
     */
    public static int addAmmo(List<ItemStack> ammo, ItemStack itemStack, boolean last, boolean trackEmptySlots) {
        return addAmmo(ammo, itemStack, last, trackEmptySlots, 0);
    }

    /**
     * Does not modify the passed ItemStack. Set maxCount to 0 to add the entire stack.
     * If trackEmptySlots is true, the amount of items (including empty slots) in the ammo list is assumed to be the capacity.
     *
     * @return The amount of rounds loaded
     */
    public static int addAmmo(List<ItemStack> ammo, ItemStack itemStack, boolean last, boolean trackEmptySlots, int maxCount) {
        if (itemStack.isEmpty() || maxCount < 0)
            return 0;
        ItemStack copy = itemStack.copy();
        if (trackEmptySlots) {
            int emptySlots = 0;
            for (ItemStack ammoStack : ammo)
                emptySlots += ammoStack.isEmpty() ? 1 : 0;
            copy.setCount(Math.min(copy.getCount(), emptySlots));
        }
        if (maxCount > 0)
            copy.setCount(Math.min(copy.getCount(), maxCount));
        if (copy.isEmpty())
            return 0;
        if (ammo.isEmpty()) {
            ammo.add(copy);
            return copy.getCount();
        }
        if (trackEmptySlots) {
            ListIterator<ItemStack> lister = ammo.listIterator(last ? ammo.size() : 0);
            ItemStack priorSlot = ItemStack.EMPTY;
            int count = copy.getCount();
            while ((last ? lister.hasPrevious() : lister.hasNext()) && !copy.isEmpty()) {
                ItemStack ammoSlot = last ? lister.previous() : lister.next();
                if (!ammoSlot.isEmpty()) {
                    if (ItemStack.isSameItemSameComponents(ammoSlot, priorSlot)) {
                        int maxCompress = Math.min(ammoSlot.getCount(), priorSlot.getMaxStackSize() - priorSlot.getCount());
                        priorSlot.grow(maxCompress);
                        ammoSlot.shrink(maxCompress);
                    }
                    if (ammoSlot.isEmpty()) {
                        lister.remove();
                    } else {
                        priorSlot = ammoSlot;
                    }
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(copy, priorSlot) && priorSlot.getCount() < priorSlot.getMaxStackSize()) {
                    priorSlot.grow(1);
                    copy.shrink(1);
                    lister.remove();
                } else {
                    lister.set(priorSlot = copy.split(1));
                }
            }
            return count;
        } else {
            int index = last ? ammo.size() - 1 : 0;
            ItemStack toStack = ammo.get(index);
            int partial = 0;
            if (ItemStack.isSameItemSameComponents(copy, toStack)) {
                int stackable = Math.min(toStack.getMaxStackSize(), copy.getCount() + toStack.getCount()) - toStack.getCount();
                toStack.grow(stackable);
                copy.shrink(stackable);
                if (copy.isEmpty()) {
                    return stackable;
                } else {
                    partial = stackable;
                }
            }
            ammo.add(index, copy);
            return partial + copy.getCount();
        }
    }

    /**
     * Does not modify the source. Set maxCapacity to 0 to add the entire source.
     * If trackEmptySlots is true, the amount of items (including empty slots) in the destination ammo list is assumed to be the capacity.
     *
     * @return the amount of rounds added
     */
    public static int addMultipleAmmo(List<ItemStack> dest, List<ItemStack> source, boolean addToDestEnd,
                                      boolean fromSourceEnd, boolean trackEmptySlots, int maxCapacity) {
        if (source.isEmpty())
            return 0;
        int addable = maxCapacity > 0 ? Math.max(0, maxCapacity - RFEItemUtils.countItems(dest)) : RFEItemUtils.countItems(source);
        int oldAddable = addable;
        if (addable < 1)
            return 0;
        if (fromSourceEnd) {
            source = new LinkedList<>(source);
            Collections.reverse(source);
        }
        for (ItemStack sourceStack : source) {
            addable -= addAmmo(dest, sourceStack, addToDestEnd, trackEmptySlots, addable);
            if (addable < 1)
                break;
        }
        return oldAddable - addable;
    }

    public static void setActionTime(ItemStack itemStack, int cooldown) {
        itemStack.set(RFEDataComponents.ACTION_TIME, cooldown);
    }

    public static int getActionTime(ItemStack itemStack) {
        return itemStack.getOrDefault(RFEDataComponents.ACTION_TIME, 0);
    }

    public static void setAction(ItemStack itemStack, @Nullable RFEFirearmItem.Action action) {
        if (action != null) {
            itemStack.set(RFEDataComponents.FIREARM_ACTION, action);
        } else {
            itemStack.remove(RFEDataComponents.FIREARM_ACTION);
        }
    }

    public static DataComponentPatch cancelReload(ItemStack itemStack, DataComponentPatch data) {
        FirearmDataUtils.setAction(itemStack, null);
        FirearmDataUtils.setActionTime(itemStack, 0);
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.remove(RFEDataComponents.RELOAD_PHASE);
        patched.remove(RFEDataComponents.RELOAD_PHASE_INDEX);
        return patched.asPatch();
    }

    public static DataComponentPatch cancelUnload(ItemStack itemStack, DataComponentPatch data) {
        FirearmDataUtils.setAction(itemStack, null);
        FirearmDataUtils.setActionTime(itemStack, 0);
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.remove(RFEDataComponents.UNLOAD_PHASE);
        patched.remove(RFEDataComponents.UNLOAD_PHASE_INDEX);
        return patched.asPatch();
    }

    @Nullable
    public static RFEFirearmItem.Action getAction(ItemStack itemStack) {
        return itemStack.get(RFEDataComponents.FIREARM_ACTION);
    }

    // Heating methods

    public static void setHeat(ItemStack itemStack, float heat) {
        itemStack.set(RFEDataComponents.FIREARM_HEAT, heat);
    }

    public static DataComponentPatch setHeat(DataComponentPatch data, float heat) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.FIREARM_HEAT, heat);
        return patched.asPatch();
    }

    public static float getHeat(ItemStack itemStack) {
        return getHeat(itemStack.getComponentsPatch());
    }

    public static float getHeat(DataComponentPatch data) {
        Optional<? extends Float> o = data.get(RFEDataComponents.FIREARM_HEAT);
        return o != null && o.isPresent() ? o.get() : 0f;
    }

    public static void addHeat(ItemStack itemStack, float addedHeat) {
        itemStack.set(RFEDataComponents.FIREARM_HEAT, itemStack.getOrDefault(RFEDataComponents.FIREARM_HEAT, 0).floatValue() + addedHeat);
    }

    public static DataComponentPatch addHeat(DataComponentPatch data, float addedHeat) {
        return setHeat(data, getHeat(data) + addedHeat);
    }

    public static void setCoolingDelay(ItemStack itemStack, int delay) {
        itemStack.set(RFEDataComponents.COOLING_DELAY, delay);
    }

    public static DataComponentPatch setCoolingDelay(DataComponentPatch data, int delay) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.COOLING_DELAY, delay);
        return patched.asPatch();
    }

    public static int getCoolingDelay(ItemStack itemStack) {
        return getCoolingDelay(itemStack.getComponentsPatch());
    }

    public static int getCoolingDelay(DataComponentPatch data) {
        Optional<? extends Integer> o = data.get(RFEDataComponents.COOLING_DELAY);
        return o != null && o.isPresent() ? o.get() : 0;
    }

    public static void setOverheated(ItemStack itemStack, boolean overheated) {
        if (overheated) {
            itemStack.set(RFEDataComponents.OVERHEATED, true);
        } else {
            itemStack.remove(RFEDataComponents.OVERHEATED);
        }
    }

    public static DataComponentPatch setOverheated(DataComponentPatch data, boolean overheated) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (overheated) {
            patched.set(RFEDataComponents.OVERHEATED, true);
        } else {
            patched.remove(RFEDataComponents.OVERHEATED);
        }
        return patched.asPatch();
    }

    public static boolean isOverheated(ItemStack itemStack) {
        return isOverheated(itemStack.getComponentsPatch());
    }

    public static boolean isOverheated(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.OVERHEATED);
        return o != null && o.isPresent();
    }

    // Key methods

    public static void setHoldingAttackKey(ItemStack itemStack, boolean holdingAttackKey) {
        if (holdingAttackKey) {
            itemStack.set(RFEDataComponents.HOLDING_ATTACK_KEY, true);
        } else {
            itemStack.remove(RFEDataComponents.HOLDING_ATTACK_KEY);
        }
    }

    public static boolean isHoldingAttackKey(ItemStack itemStack) {
        return itemStack.has(RFEDataComponents.HOLDING_ATTACK_KEY);
    }

    // Aiming methods

    public static void setAiming(ItemStack itemStack, boolean aiming) {
        if (aiming) {
            itemStack.set(RFEDataComponents.AIMING, true);
        } else {
            itemStack.remove(RFEDataComponents.AIMING);
        }
    }

    public static boolean isAiming(ItemStack itemStack) {
        return itemStack.has(RFEDataComponents.AIMING);
    }

    public static void setAimingTime(ItemStack itemStack, int time) {
        itemStack.set(RFEDataComponents.AIMING_TIME, time);
    }

    public static int getAimingTime(ItemStack itemStack) {
        return itemStack.getOrDefault(RFEDataComponents.AIMING_TIME, 0);
    }
    
    // Windup methods

    public static void setWindingUp(ItemStack itemStack, boolean windingUp) {
        if (windingUp) {
            itemStack.set(RFEDataComponents.WINDING_UP, true);
        } else {
            itemStack.remove(RFEDataComponents.WINDING_UP);
        }
    }

    public static DataComponentPatch setWindingUp(DataComponentPatch data, boolean windingUp) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (windingUp) {
            patched.set(RFEDataComponents.WINDING_UP, true);
        } else {
            patched.remove(RFEDataComponents.WINDING_UP);
        }
        return patched.asPatch();
    }

    public static boolean isWindingUp(ItemStack itemStack) {
        return isWindingUp(itemStack.getComponentsPatch());
    }

    public static boolean isWindingUp(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.WINDING_UP);
        return o != null && o.isPresent();
    }
    
    // Extra firing time methods

    public static void setExtraFiringTime(ItemStack itemStack, float extraFiringTime) {
        itemStack.set(RFEDataComponents.EXTRA_FIRING_TIME, extraFiringTime);
    }

    public static DataComponentPatch setExtraFiringTime(DataComponentPatch data, float extraFiringTime) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.EXTRA_FIRING_TIME, extraFiringTime);
        return patched.asPatch();
    }

    public static float getExtraFiringTime(ItemStack itemStack) {
        return getExtraFiringTime(itemStack.getComponentsPatch());
    }

    public static float getExtraFiringTime(DataComponentPatch data) {
        Optional<? extends Float> o = data.get(RFEDataComponents.EXTRA_FIRING_TIME);
        return o != null && o.isPresent() ? o.get() : 0f;
    }
    
    // Burst fire methods

    public static void setBurstFireCount(ItemStack itemStack, int burstFireCount) {
        itemStack.set(RFEDataComponents.BURST_FIRE_COUNT, burstFireCount);
    }

    public static DataComponentPatch setBurstFireCount(DataComponentPatch data, int burstFireCount) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.BURST_FIRE_COUNT, burstFireCount);
        return patched.asPatch();
    }

    public static void clearBurstFireCount(ItemStack itemStack) {
        itemStack.remove(RFEDataComponents.BURST_FIRE_COUNT);
    }

    public static DataComponentPatch clearBurstFireCount(DataComponentPatch data) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.remove(RFEDataComponents.BURST_FIRE_COUNT);
        return patched.asPatch();
    }

    public static int getBurstFireCount(ItemStack itemStack) {
        return getBurstFireCount(itemStack.getComponentsPatch());
    }

    public static int getBurstFireCount(DataComponentPatch data) {
        Optional<? extends Integer> o = data.get(RFEDataComponents.BURST_FIRE_COUNT);
        return o != null && o.isPresent() ? o.get() : 0;
    }
    
    // Force cancel action methods

    public static void setForceCancelAction(ItemStack itemStack, boolean forceCancelAction) {
        if (forceCancelAction) {
            itemStack.set(RFEDataComponents.FORCE_CANCEL_ACTION, true);
        } else {
            itemStack.remove(RFEDataComponents.FORCE_CANCEL_ACTION);
        }
    }

    public static DataComponentPatch setForceCancelAction(DataComponentPatch data, boolean forceCancelAction) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (forceCancelAction) {
            patched.set(RFEDataComponents.FORCE_CANCEL_ACTION, true);
        } else {
            patched.remove(RFEDataComponents.FORCE_CANCEL_ACTION);
        }
        return patched.asPatch();
    }

    public static boolean shouldForceCancelAction(ItemStack itemStack) {
        return shouldForceCancelAction(itemStack.getComponentsPatch());
    }

    public static boolean shouldForceCancelAction(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.FORCE_CANCEL_ACTION);
        return o != null && o.isPresent();
    }

    // Shot count methods

    public static void setShotCount(ItemStack itemStack, int shotCount) {
        itemStack.set(RFEDataComponents.SHOT_COUNT, shotCount);
    }

    public static DataComponentPatch setShotCount(DataComponentPatch data, int shotCount) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.SHOT_COUNT, shotCount);
        return patched.asPatch();
    }

    public static int getShotCount(ItemStack itemStack) {
        return getShotCount(itemStack.getComponentsPatch());
    }

    public static int getShotCount(DataComponentPatch data) {
        Optional<? extends Integer> o = data.get(RFEDataComponents.SHOT_COUNT);
        return o != null && o.isPresent() ? o.get() : 0;
    }

    // Is equipped methods

    public static void setEquipped(ItemStack itemStack, boolean isEquipped) {
        itemStack.set(RFEDataComponents.IS_EQUIPPED, isEquipped);
    }

    public static DataComponentPatch setEquipped(DataComponentPatch data, boolean isEquipped) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.IS_EQUIPPED, isEquipped);
        return patched.asPatch();
    }

    public static boolean isEquipped(ItemStack itemStack) {
        return isEquipped(itemStack.getComponentsPatch());
    }

    public static boolean isEquipped(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.IS_EQUIPPED);
        return o != null && o.isPresent() && o.get();
    }
    
    // Is used primer methods

    public static void setUsedPrimer(ItemStack itemStack, boolean isUsedPrimer) {
        itemStack.set(RFEDataComponents.IS_USED_PRIMER, isUsedPrimer);
    }

    public static DataComponentPatch setUsedPrimer(DataComponentPatch data, boolean isUsedPrimer) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        patched.set(RFEDataComponents.IS_USED_PRIMER, isUsedPrimer);
        return patched.asPatch();
    }

    public static boolean isUsedPrimer(ItemStack itemStack) {
        return isUsedPrimer(itemStack.getComponentsPatch());
    }

    public static boolean isUsedPrimer(DataComponentPatch data) {
        Optional<? extends Boolean> o = data.get(RFEDataComponents.IS_USED_PRIMER);
        return o != null && o.isPresent() && o.get();
    }

    // Melee state methods

    public static void setMeleeState(ItemStack itemStack, boolean isMeleeing) {
        itemStack.set(RFEDataComponents.MELEEING, isMeleeing);
    }

    public static boolean isInMeleeState(ItemStack itemStack) {
        return itemStack.getOrDefault(RFEDataComponents.MELEEING, false);
    }

    private FirearmDataUtils() {}

}
