package rbasamoyai.ritchiesfirearmengine.content.firearms.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FirearmDataUtils {

    // Charging methods

    public static boolean isCharged(ItemStack itemStack) {
        return isCharged(itemStack.getOrCreateTag());
    }

    public static boolean isCharged(CompoundTag tag) {
        return tag.contains("Charged");
    }

    public static void setCharged(ItemStack itemStack, boolean charged) {
        setCharged(itemStack.getOrCreateTag(), charged);
    }

    public static void setCharged(CompoundTag tag, boolean charged) {
        if (charged) {
            tag.putBoolean("Charged", true);
        } else {
            tag.remove("Charged");
        }
    }

    // Jamming methods

    public static boolean isJammed(ItemStack itemStack) {
        return isJammed(itemStack.getOrCreateTag());
    }

    public static boolean isJammed(CompoundTag tag) {
        return tag.contains("Jammed");
    }

    public static void setJammed(ItemStack itemStack, boolean jammed) {
        setJammed(itemStack.getOrCreateTag(), jammed);
    }

    public static void setJammed(CompoundTag tag, boolean jammed) {
        if (jammed) {
            tag.putBoolean("Jammed", true);
        } else {
            tag.remove("Jammed");
        }
    }

    // Ammunition methods

    public static ListTag writeAmmoList(List<ItemStack> ammo) {
        ListTag list = new ListTag();
        for (ItemStack itemStack : ammo)
            list.add(itemStack.save(new CompoundTag()));
        return list;
    }

    public static List<ItemStack> readAmmoList(ListTag tag) {
        int sz = tag.size();
        List<ItemStack> list = new LinkedList<>();
        for (int i = 0; i < sz; ++i)
            list.add(ItemStack.of(tag.getCompound(i)));
        return list;
    }

    public static List<ItemStack> getRounds(CompoundTag tag, String tagKey) {
        if (!tag.contains(tagKey, Tag.TAG_LIST))
            return new LinkedList<>();
        return readAmmoList(tag.getList(tagKey, Tag.TAG_COMPOUND));
    }

    public static List<ItemStack> getRounds(ItemStack itemStack, String tagKey) {
        return getRounds(itemStack.getOrCreateTag(), tagKey);
    }

    public static void saveRounds(CompoundTag tag, String tagKey, List<ItemStack> ammo) {
        tag.put(tagKey, writeAmmoList(ammo));
    }

    public static void saveRounds(ItemStack itemStack, String tagKey, List<ItemStack> ammo) {
        saveRounds(itemStack.getOrCreateTag(), tagKey, ammo);
    }

    public static ItemStack stripFirstAmmo(List<ItemStack> ammo, boolean simulate) { return stripAmmo(ammo, false, simulate); }

    public static ItemStack stripAmmo(List<ItemStack> ammo, boolean last, boolean simulate) {
        if (ammo.isEmpty())
            return ItemStack.EMPTY;
        ItemStack next = last ? ammo.get(ammo.size() - 1) : ammo.get(0);
        ItemStack ret = next.copy();
        ret.setCount(1);
        if (!simulate) {
            next.shrink(1);
            if (next.isEmpty())
                ammo.remove(0);
        }
        return ret;
    }

    /**
     * Strips multiple ammo. If count is less than 1, no ammo is stripped.
     */
    public static List<ItemStack> stripMultipleAmmo(List<ItemStack> ammo, int count, boolean last, boolean simulate) {
        List<ItemStack> stripped = new LinkedList<>();
        if (ammo.isEmpty() || count < 1)
            return stripped;
        ListIterator<ItemStack> lister = last ? ammo.listIterator(ammo.size()) : ammo.listIterator();
        int newCount = count;
        while (last ? lister.hasPrevious() : lister.hasNext()) {
            ItemStack stackToRemove = last ? lister.previous() : lister.next();
            int toRemove = Math.min(newCount, stackToRemove.getCount());
            if (toRemove < 1)
                break;
            ItemStack copy = stackToRemove.copy();
            copy.setCount(toRemove);
            stripped.add(copy);
            if (!simulate) {
                stackToRemove.shrink(toRemove);
                if (stackToRemove.isEmpty())
                    lister.remove();
            }
            newCount -= toRemove;
        }
        return stripped;
    }

    /**
     * Does not modify the passed ItemStack. Adds the entire itemStack.
     *
     * @return The amount of rounds loaded
     */
    public static int addAmmo(List<ItemStack> ammo, ItemStack itemStack, boolean last) {
        return addAmmo(ammo, itemStack, last, 0);
    }

    /**
     * Does not modify the passed ItemStack. Set maxCount to 0 to add the entire stack.
     *
     * @return The amount of rounds loaded
     */
    public static int addAmmo(List<ItemStack> ammo, ItemStack itemStack, boolean last, int maxCount) {
        if (itemStack.isEmpty() || maxCount < 0)
            return 0;
        ItemStack copy = itemStack.copy();
        if (maxCount > 0)
            copy.setCount(Math.min(copy.getCount(), maxCount));
        if (ammo.isEmpty()) {
            ammo.add(copy);
            return copy.getCount();
        }
        ItemStack toStack = last ? ammo.get(ammo.size() - 1) : ammo.get(0);
        int partial = 0;
        if (ItemStack.isSameItemSameTags(copy, toStack)) {
            int stackable = Math.min(toStack.getMaxStackSize(), copy.getCount() + toStack.getCount()) - toStack.getCount();
            toStack.grow(stackable);
            copy.shrink(stackable);
            if (copy.isEmpty()) {
                return stackable;
            } else {
                partial = stackable;
            }
        }
        if (last) {
            ammo.add(copy);
        } else {
            ammo.add(0, copy);
        }
        return partial + copy.getCount();
    }

    /**
     * Does not modify the source. Set maxCapacity to 0 to add the entire source.
     *
     * @return the amount of rounds added
     */
    public static int addMultipleAmmo(List<ItemStack> dest, List<ItemStack> source, boolean addToDestEnd, boolean fromSourceEnd, int maxCapacity) {
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
            addable -= addAmmo(dest, sourceStack, addToDestEnd, addable);
            if (addable < 1)
                break;
        }
        return oldAddable - addable;
    }

    public static void setActionTime(ItemStack itemStack, int cooldown) {
        itemStack.getOrCreateTag().putInt("ActionTime", cooldown);
    }

    public static int getActionTime(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("ActionTime");
    }

    public static void setAction(ItemStack itemStack, @Nullable RFEFirearmItem.Action action) {
        if (action != null) {
            itemStack.getOrCreateTag().putString("Action", action.getSerializedName());
        } else {
            itemStack.getOrCreateTag().remove("Action");
        }
    }

    public static void cancelReload(ItemStack itemStack, CompoundTag tag) {
        tag.remove("ReloadPhase");
        tag.remove("ReloadPhaseIndex");
        FirearmDataUtils.setAction(itemStack, null);
        FirearmDataUtils.setActionTime(itemStack, 0);
    }

    public static void cancelUnload(ItemStack itemStack, CompoundTag tag) {
        tag.remove("UnloadPhase");
        tag.remove("UnloadPhaseIndex");
        FirearmDataUtils.setAction(itemStack, null);
        FirearmDataUtils.setActionTime(itemStack, 0);
    }

    @Nullable
    public static RFEFirearmItem.Action getAction(ItemStack itemStack) {
        return RFEFirearmItem.Action.byId(itemStack.getOrCreateTag().getString("Action"));
    }

    // Heating methods

    public static void setHeat(ItemStack itemStack, float heat) {
        setHeat(itemStack.getOrCreateTag(), heat);
    }

    public static void setHeat(CompoundTag tag, float heat) {
        tag.putFloat("FirearmHeat", heat);
    }

    public static float getHeat(ItemStack itemStack) {
        return getHeat(itemStack.getOrCreateTag());
    }

    public static float getHeat(CompoundTag tag) {
        return tag.getFloat("FirearmHeat");
    }

    public static void addHeat(ItemStack itemStack, float addedHeat) {
        addHeat(itemStack.getOrCreateTag(), addedHeat);
    }

    public static void addHeat(CompoundTag tag, float addedHeat) {
        setHeat(tag, getHeat(tag) + addedHeat);
    }

    public static void setCoolingDelay(ItemStack itemStack, int delay) {
        setCoolingDelay(itemStack.getOrCreateTag(), delay);
    }

    public static void setCoolingDelay(CompoundTag tag, int delay) {
        tag.putInt("CoolingDelay", delay);
    }

    public static int getCoolingDelay(ItemStack itemStack) {
        return getCoolingDelay(itemStack.getOrCreateTag());
    }

    public static int getCoolingDelay(CompoundTag tag) {
        return tag.getInt("CoolingDelay");
    }

    public static void setOverheated(ItemStack itemStack, boolean overheated) {
        setOverheated(itemStack.getOrCreateTag(), overheated);
    }

    public static void setOverheated(CompoundTag tag, boolean overheated) {
        if (overheated) {
            tag.putBoolean("Overheated", true);
        } else {
            tag.remove("Overheated");
        }
    }

    public static boolean isOverheated(ItemStack itemStack) {
        return isOverheated(itemStack.getOrCreateTag());
    }

    public static boolean isOverheated(CompoundTag tag) {
        return tag.contains("Overheated");
    }

    // Key methods

    public static void setHoldingAttackKey(ItemStack itemStack, boolean holdingAttackKey) {
        if (holdingAttackKey) {
            itemStack.getOrCreateTag().putBoolean("HoldingAttackKey", true);
        } else {
            itemStack.getOrCreateTag().remove("HoldingAttackKey");
        }
    }

    public static boolean isHoldingAttackKey(ItemStack itemStack) {
        return itemStack.getOrCreateTag().contains("HoldingAttackKey");
    }

    // Aiming methods

    public static void setAiming(ItemStack itemStack, boolean aiming) {
        if (aiming) {
            itemStack.getOrCreateTag().putBoolean("Aiming", aiming);
        } else {
            itemStack.getOrCreateTag().remove("Aiming");
        }
    }

    public static boolean isAiming(ItemStack itemStack) {
        return itemStack.getOrCreateTag().contains("Aiming");
    }

    public static void setAimingTime(ItemStack itemStack, int time) {
        itemStack.getOrCreateTag().putInt("AimingTime", time);
    }

    public static int getAimingTime(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("AimingTime");
    }

    private FirearmDataUtils() {
    }

}
