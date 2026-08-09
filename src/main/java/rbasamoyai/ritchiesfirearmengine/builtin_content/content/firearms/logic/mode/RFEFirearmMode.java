package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ai.ICanFireRFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmCondition;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhaseAccessFilter;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags.RFEItemTags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEAimAngles;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.*;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadManager;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.network.ClientboundRunFiringLogicPacket;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundRunFiringLogicPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Basic firearm mode class.
 */
public class RFEFirearmMode {

    public static final StreamCodec<RegistryFriendlyByteBuf, ImmutableMap<String, DataComponentPatch>> MODE_DATA_STREAM_CODEC =
            RFEByteBufCodecUtils.immutableMap(ByteBufCodecs.STRING_UTF8, DataComponentPatch.STREAM_CODEC);
    public static final Codec<ImmutableMap<String, DataComponentPatch>> MODE_DATA_CODEC =
            Codec.unboundedMap(Codec.STRING, DataComponentPatch.CODEC).xmap(RFEUtils::toImmutableMap, Function.identity());

    protected final String modeId;
    protected final String modeDisplayId;

    protected final RFEFirearmModeHandlingProperties defaultDataPackProperties;

    protected final String modeDataId;

    // Drawing
    protected final int drawTime;
    @Nullable protected final SoundEvent drawSound;

    // Mode change
    protected final int modeChangeTime;
    @Nullable protected final SoundEvent modeChangeSound;

    // Aiming
    protected final boolean canAim;
    protected final int aimTime;
    protected final int unaimTime;
    @Nullable protected final SoundEvent aimSound;
    @Nullable protected final SoundEvent unaimSound;

    // Ammo
    protected final boolean ammoRequired;
    protected final int internalCapacity;
    protected final int nominalCapacity;
    protected final boolean plusOneCapacity;
    protected final boolean trackEmptySlots;

    protected final boolean requiresSecondaryAmmo;
    protected final boolean secondaryAmmoRemainsAfterFiring;
    protected final int internalSecondaryCapacity;
    protected final int nominalSecondaryCapacity;
    protected final boolean plusOneSecondaryCapacity;
    protected final boolean trackEmptySecondarySlots;

    // Firing
    protected final FireMode fireMode;
    protected final float firingCooldown;
    protected final boolean ammoConsumedLast;
    protected final boolean ignoreEmptySlotsWhenFiring;
    protected final int shotsFired;
    protected final int burstRoundCount;
    protected final boolean slamfire;
    protected final int slotsCycledAfterFiring;
    @Nullable protected final SoundEvent firingSound;
    protected final float firingSoundRange;
    // Dry fire
    protected final boolean canDryFire;
    @Nullable protected final SoundEvent dryFireSound;
    // Misfire
    @Nullable protected final SoundEvent misfireSound;
    // Wind-up
    protected final int windUpTime;
    protected final boolean canInterruptWindUp;
    @Nullable protected final SoundEvent windUpSound;
    // Wind-down
    protected final int windDownTime;
    @Nullable protected final SoundEvent windDownSound;
    // Eject magazines
    protected final boolean ejectMagazineOnLastShot;
    @Nullable protected final SoundEvent ejectMagazineOnLastShotSound;
    // Zeroing
    protected final float pitchAdjustment;

    // Reloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> reloadPhases;
    protected final Map<ResourceLocation, CompareValueSource> reloadingCompareValues;

    // Unloading
    protected final Map<ReloadPhase.PhaseType, List<ReloadPhase>> unloadPhases;
    protected final Map<ResourceLocation, CompareValueSource> unloadingCompareValues;

    // Charging
    protected final List<ChargeAction> chargeActions;
    protected final Map<ResourceLocation, CompareValueSource> chargingCompareValues;
    protected final boolean resetChargeOnUnequip;

    // Meleeing
    protected final boolean canMelee;
    protected final boolean forcedMelee;
    protected final int enterMeleeWindowTime;
    protected final int exitMeleeTime;

    // Overheating
    protected final boolean canOverheat;
    protected final int cooldownTime;
    @Nullable protected final SoundEvent cooldownSound;

    // Attachments
    @Nullable protected final ResourceLocation loadedRoundAttachmentSlot;
    @Nullable protected final ResourceLocation magazineAttachmentSlot;
    @Nullable protected final ResourceLocation loadedSecondaryAttachmentSlot;

    // Misc
    protected final float itemLength;

    public RFEFirearmMode(RFEFirearmModeBuilder builder, String modeId) {
        this.modeId = modeId;
        this.modeDisplayId = builder.modeDisplayId;

        this.defaultDataPackProperties = RFEFirearmModeHandlingProperties.fromItemDefinition(builder);

        this.modeDataId = builder.modeTagId;

        this.drawTime = builder.drawTime;
        this.drawSound = builder.drawSound;

        this.modeChangeTime = builder.modeChangeTime;
        this.modeChangeSound = builder.modeChangeSound;

        this.canAim = builder.canAim;
        this.aimTime = builder.aimTime;
        this.unaimTime = builder.unaimTime;
        this.aimSound = builder.aimSound;
        this.unaimSound = builder.unaimSound;

        this.ammoRequired = builder.ammoRequired;
        this.internalCapacity = builder.internalCapacity;
        this.nominalCapacity = builder.nominalCapacity;
        this.plusOneCapacity = builder.plusOneCapacity;
        this.trackEmptySlots = builder.trackEmptySlots;

        this.requiresSecondaryAmmo = builder.requiresSecondaryAmmo;
        this.secondaryAmmoRemainsAfterFiring = builder.secondaryAmmoRemainsAfterFiring;
        this.internalSecondaryCapacity = builder.internalSecondaryCapacity;
        this.nominalSecondaryCapacity = builder.nominalSecondaryCapacity;
        this.plusOneSecondaryCapacity = builder.plusOneSecondaryCapacity;
        this.trackEmptySecondarySlots = builder.trackEmptySecondarySlots;

        this.fireMode = builder.fireMode;
        this.firingCooldown = builder.firingCooldown;
        this.ammoConsumedLast = builder.ammoConsumedLast;
        this.ignoreEmptySlotsWhenFiring = builder.ignoreEmptySlotsWhenFiring;
        this.shotsFired = builder.shotsFired;
        this.burstRoundCount = builder.burstRoundCount;
        this.slamfire = builder.slamfire;
        this.slotsCycledAfterFiring = builder.slotsCycledAfterFiring;
        this.canDryFire = builder.canDryFire;
        this.firingSound = builder.firingSound;
        this.firingSoundRange = builder.firingSoundRange;
        this.dryFireSound = builder.dryFireSound;
        this.misfireSound = builder.misfireSound;
        this.windUpTime = builder.windUpTime;
        this.canInterruptWindUp = builder.canInterruptWindUp;
        this.windUpSound = builder.windUpSound;
        this.windDownTime = builder.windDownTime;
        this.windDownSound = builder.windDownSound;
        this.ejectMagazineOnLastShot = builder.ejectMagazineOnLastShot;
        this.ejectMagazineOnLastShotSound = builder.ejectMagazineOnLastShotSound;
        this.pitchAdjustment = builder.pitchAdjustment;

        this.resetChargeOnUnequip = builder.resetChargeOnUnequip;

        this.reloadPhases = builder.finalReloadPhases;
        this.unloadPhases = builder.finalUnloadPhases;
        this.chargeActions = builder.chargeActions;

        this.reloadingCompareValues = builder.reloadingCompareValues;
        this.unloadingCompareValues = builder.unloadingCompareValues;
        this.chargingCompareValues = builder.chargingCompareValues;

        this.canMelee = builder.canMelee;
        this.forcedMelee = builder.forcedMelee;
        this.enterMeleeWindowTime = builder.enterMeleeWindowTime;
        this.exitMeleeTime = builder.exitMeleeTime;

        this.canOverheat = builder.canOverheat;
        this.cooldownTime = builder.cooldownTime;
        this.cooldownSound = builder.cooldownSound;

        this.itemLength = builder.itemLength;

        this.loadedRoundAttachmentSlot = builder.loadedRoundAttachmentSlot;
        this.magazineAttachmentSlot = builder.magazineAttachmentSlot;
        this.loadedSecondaryAttachmentSlot = builder.loadedSecondaryAttachmentSlot;
    }

    public String getModeId() { return this.modeId; }
    public String getDisplayId() { return this.modeDisplayId; }

    public FireMode getFireMode() { return this.fireMode; }

    public RFEFirearmModeHandlingProperties getHandlingProperties(ItemStack itemStack) {
        ImmutableMap<String, RFEFirearmModeHandlingProperties> handlingPropertiesByMode = RFEFirearmHandlingPropertiesHandler.getHandlingProperties(itemStack);
        return handlingPropertiesByMode.getOrDefault(this.modeId, this.defaultDataPackProperties);
    }

    public RFEFirearmModeAmmoProperties getAmmoProperties(ItemStack itemStack) {
        return RFEFirearmAmmoHandler.getAmmoProperties(itemStack).getProperties(this.modeId);
    }

    public DataComponentPatch getModeData(ItemStack itemStack) {
        if (!itemStack.has(RFEDataComponents.FIREARM_MODE_DATA))
            itemStack.set(RFEDataComponents.FIREARM_MODE_DATA, ImmutableMap.of());
        ImmutableMap<String, DataComponentPatch> modeDataIMap = itemStack.get(RFEDataComponents.FIREARM_MODE_DATA);
        if (modeDataIMap.containsKey(this.modeDataId))
            return modeDataIMap.get(this.modeDataId);
        Object2ObjectOpenHashMap<String, DataComponentPatch> modeDataMap = new Object2ObjectOpenHashMap<>(modeDataIMap);
        modeDataMap.put(this.modeDataId, DataComponentPatch.EMPTY);
        itemStack.set(RFEDataComponents.FIREARM_MODE_DATA, RFEUtils.toImmutableMap(modeDataMap));
        return DataComponentPatch.EMPTY;
    }

    public void saveModeData(ItemStack itemStack, DataComponentPatch data) {
        Map<String, DataComponentPatch> map = new HashMap<>(itemStack.getOrDefault(RFEDataComponents.FIREARM_MODE_DATA, ImmutableMap.of()));
        map.put(this.modeDataId, data);
        itemStack.set(RFEDataComponents.FIREARM_MODE_DATA, ImmutableMap.copyOf(map));
    }

    public int getNominalCapacity(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0)
            return this.nominalCapacity;
        DataComponentPatch modeData = this.getModeData(itemStack);
        Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
        if (o == null || o.isEmpty())
            return 0;
        ItemStack magazine = o.get().copyOne();
        return magazine.getItem() instanceof MagazineItem magazineItem ? magazineItem.getMagazineCapacity(magazine) : 0;
    }

    public List<ItemStack> getLoadedAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalCapacity > 0) {
            return FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
        } else {
            List<ItemStack> list = new ArrayList<>();
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty())
                    list.add(loadedRound);
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent()) {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem)
                    list.addAll(magazineItem.getStoredAmmo(magazine));
            }
            return list;
        }
    }

    public int getLoadedAmmoCount(ItemStack itemStack, LivingEntity entity, boolean countPlusOne) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalCapacity > 0)
            return RFEItemUtils.countItems(FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS));
        int count = 0;
        if (this.plusOneCapacity && countPlusOne && !this.getLoadedRound(itemStack).isEmpty())
            ++count;
        Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
        if (o != null && o.isPresent()) {
            ItemStack magazine = o.get().copyOne();
            if (magazine.getItem() instanceof MagazineItem magazineItem)
                count += RFEItemUtils.countItems(magazineItem.getStoredAmmo(magazine));
        }
        return count;
    }

    public List<ItemStack> getNextRoundsInItem(ItemStack itemStack, @Nullable LivingEntity entity, int count, boolean strip) {
        List<ItemStack> totalStripped = new LinkedList<>();
        if (this.internalCapacity > 0) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            List<ItemStack> list = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            List<ItemStack> internalStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                    !strip, this.trackEmptySlots, this.ignoreEmptySlotsWhenFiring);
            count -= RFEItemUtils.countItems(internalStripped);
            totalStripped.addAll(internalStripped);
            if (strip)
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS, list));
        } else {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    --count;
                    totalStripped.add(loadedRound.split(1));
                    if (strip)
                        this.setLoadedRound(itemStack, ItemStack.EMPTY);
                }
            }
            DataComponentPatch modeData = this.getModeData(itemStack);
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent()) {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    List<ItemStack> list = magazineItem.getStoredAmmo(magazine);
                    List<ItemStack> magStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                            !strip, this.trackEmptySlots, this.ignoreEmptySlotsWhenFiring);
                    count -= RFEItemUtils.countItems(magStripped);
                    totalStripped.addAll(magStripped);
                    if (strip) {
                        magazineItem.writeStoredAmmo(magazine, list);
                        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                        patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                        this.saveModeData(itemStack, patched.asPatch());
                    }
                }
            }
        }
        return totalStripped;
    }

    public ItemStack getLoadedRound(ItemStack itemStack) {
        Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.LOADED_ROUND);
        return o != null && o.isPresent() ? o.get().copyOne() : ItemStack.EMPTY;
    }

    public void setLoadedRound(ItemStack itemStack, ItemStack roundStack) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
        if (roundStack.isEmpty()) {
            patched.remove(RFEDataComponents.LOADED_ROUND);
        } else {
            patched.set(RFEDataComponents.LOADED_ROUND, RFEItemContainerContents.fromItems(List.of(roundStack)));
        }
        this.saveModeData(itemStack, patched.asPatch());
    }

    public List<ItemStack> getLoadedSecondaryAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalSecondaryCapacity > 0) {
            return FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
        } else {
            List<ItemStack> list = new ArrayList<>();
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty())
                    list.add(loadedPrimer);
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent()) {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem)
                    list.addAll(magazineItem.getStoredSecondaryAmmo(magazine));
            }
            return list;
        }
    }

    public List<ItemStack> getNextSecondariesInItem(ItemStack itemStack, @Nullable LivingEntity entity, int count,
                                                    boolean strip, boolean countUsedPrimers) {
        List<ItemStack> totalStripped = new LinkedList<>();
        if (this.internalSecondaryCapacity > 0) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            List<ItemStack> list = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            List<ItemStack> internalStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                    !strip, this.trackEmptySecondarySlots, this.ignoreEmptySlotsWhenFiring);
            for (ItemStack stripped : internalStripped) {
                if (countUsedPrimers || !FirearmDataUtils.isUsedPrimer(stripped)) {
                    count -= stripped.getCount();
                    totalStripped.add(stripped);
                }
            }
            if (strip)
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS, list));
        } else {
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty() && !FirearmDataUtils.isUsedPrimer(loadedPrimer)) {
                    --count;
                    totalStripped.add(loadedPrimer.split(1));
                    if (strip)
                        this.setLoadedPrimer(itemStack, ItemStack.EMPTY);
                }
            }
            DataComponentPatch modeData = this.getModeData(itemStack);
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent()) {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    List<ItemStack> list = magazineItem.getStoredSecondaryAmmo(magazine);
                    List<ItemStack> magStripped = FirearmDataUtils.stripMultipleAmmo(list, count, this.ammoConsumedLast,
                            !strip, this.trackEmptySecondarySlots, this.ignoreEmptySlotsWhenFiring);
                    count -= RFEItemUtils.countItems(magStripped);
                    totalStripped.addAll(magStripped);
                    if (strip) {
                        magazineItem.writeStoredSecondaryAmmo(magazine, list);
                        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                        patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                        this.saveModeData(itemStack, patched.asPatch());
                    }
                }
            }
        }
        return totalStripped;
    }

    public ItemStack getLoadedPrimer(ItemStack itemStack) {
        Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.LOADED_PRIMER);
        return o != null && o.isPresent() ? o.get().copyOne() : ItemStack.EMPTY;
    }

    public void setLoadedPrimer(ItemStack itemStack, ItemStack primerStack) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
        if (primerStack.isEmpty()) {
            patched.remove(RFEDataComponents.LOADED_PRIMER);
        } else {
            patched.set(RFEDataComponents.LOADED_PRIMER, RFEItemContainerContents.fromItems(List.of(primerStack)));
        }
        this.saveModeData(itemStack, patched.asPatch());
    }

    public void useOrRemoveNextSecondariesInItem(ItemStack itemStack, @Nullable LivingEntity entity, int firedCount) {
        List<ItemStack> secondaries;
        if (this.internalSecondaryCapacity > 0) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            secondaries = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
        } else {
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty() && !FirearmDataUtils.isUsedPrimer(loadedPrimer)) {
                    --firedCount;
                    if (this.secondaryAmmoRemainsAfterFiring) {
                        FirearmDataUtils.setUsedPrimer(loadedPrimer, true);
                    } else {
                        loadedPrimer = ItemStack.EMPTY;
                    }
                    this.setLoadedPrimer(itemStack, loadedPrimer);
                }
            }
            DataComponentPatch modeData = this.getModeData(itemStack);
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            secondaries = magazineItem.getStoredSecondaryAmmo(magazine);
        }
        // Iterate over list and modify each stack in copy. Goes as long as there are unfired primers in the count,
        // or if the list runs out
        ListIterator<ItemStack> lister = this.ammoConsumedLast ? secondaries.listIterator(secondaries.size()) : secondaries.listIterator();
        LinkedList<ItemStack> newSecondaries = new LinkedList<>();
        int newCount = firedCount;
        while (this.ammoConsumedLast ? lister.hasPrevious() : lister.hasNext()) {
            ItemStack stackToUseOrRemove = this.ammoConsumedLast ? lister.previous() : lister.next();
            boolean skip = stackToUseOrRemove.isEmpty() || FirearmDataUtils.isUsedPrimer(stackToUseOrRemove);
            if (!skip) {
                int toUseOrFire = Math.min(newCount, stackToUseOrRemove.getCount());
                if (toUseOrFire < 1) {
                    if (this.ammoConsumedLast) {
                        newSecondaries.addFirst(stackToUseOrRemove);
                    } else {
                        newSecondaries.addLast(stackToUseOrRemove);
                    }
                    continue;
                }
                if (this.secondaryAmmoRemainsAfterFiring) { // Get new stack of fired primers from unfired primers
                    ItemStack newStack = stackToUseOrRemove.split(toUseOrFire);
                    FirearmDataUtils.setUsedPrimer(newStack, true);
                    if (this.ammoConsumedLast) {
                        newSecondaries.addFirst(newStack);
                    } else {
                        newSecondaries.addLast(newStack);
                    }
                } else {
                    stackToUseOrRemove.shrink(toUseOrFire); // Simulate stripping primers
                    if (!this.ignoreEmptySlotsWhenFiring) {
                        for (int i = 0; i < toUseOrFire; ++i) {
                            if (this.ammoConsumedLast) {
                                newSecondaries.addFirst(ItemStack.EMPTY);
                            } else {
                                newSecondaries.addLast(ItemStack.EMPTY);
                            }
                        }
                    }
                }
                newCount -= toUseOrFire;
            }
            if (skip || !stackToUseOrRemove.isEmpty()) {
                if (this.ammoConsumedLast) {
                    newSecondaries.addFirst(stackToUseOrRemove);
                } else {
                    newSecondaries.addLast(stackToUseOrRemove);
                }
            }
        }
        if (this.internalSecondaryCapacity > 0) {
            this.saveModeData(itemStack, FirearmDataUtils.saveRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_PRIMERS, newSecondaries));
        } else {
            DataComponentPatch modeData = this.getModeData(itemStack);
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent()) {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    magazineItem.writeStoredSecondaryAmmo(magazine, newSecondaries);
                    PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                    patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                    this.saveModeData(itemStack, patched.asPatch());
                }
            }
        }
    }

    public boolean isBusyWithStagedAction(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        Optional<? extends ReloadPhase.PhaseType> reload = modeData.get(RFEDataComponents.RELOAD_PHASE);
        if (reload != null && reload.isPresent())
            return true;
        Optional<? extends ReloadPhase.PhaseType> unload = modeData.get(RFEDataComponents.UNLOAD_PHASE);
        return unload != null && unload.isPresent();
    }

    public boolean canFireProjectile(ItemStack itemStack, LivingEntity entity) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.fireMode == FireMode.SAFETY || !FirearmDataUtils.isCharged(modeData)
                || FirearmDataUtils.getActionTime(itemStack) > 0 || this.isWindingUp(itemStack, entity))
            return false;
        RFEFirearmModeHandlingProperties handlingProperties = this.getHandlingProperties(itemStack);
        if (handlingProperties.maxShots() > 0 && FirearmDataUtils.getShotCount(modeData) >= handlingProperties.maxShots())
            return false;
        if (this.isJammed(itemStack))
            return false;
        if (!this.ammoRequired || this.canDryFire)
            return true;
        if (this.requiresSecondaryAmmo && this.getNextSecondariesInItem(itemStack, entity, this.shotsFired, false, false).isEmpty())
            return false;
        if (this.plusOneCapacity && this.getLoadedRound(itemStack).isEmpty())
            return false;
        return !this.getNextRoundsInItem(itemStack, entity, this.shotsFired, false).isEmpty();
    }

    public void fireFirearm(ItemStack itemStack, LivingEntity entity, FiringType firing) {
        InteractionHand hand = entity.getMainHandItem() == itemStack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean client = entity.level().isClientSide;

        if (firing != FiringType.EFFECTS) {
            if (this.windUpTime > 0) {
                if (!this.isWindingUp(itemStack, entity) && firing == FiringType.CLICK) {
                    this.setWindingUp(itemStack, entity, true);
                    FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.FIRING);
                    FirearmDataUtils.setActionTime(itemStack, this.windUpTime);
                    this.playWindUpEffects(itemStack, entity);
                    return;
                } else {
                    this.setWindingUp(itemStack, entity, false);
                }
            }

            boolean canFireClick = client && entity instanceof Player || !client && !(entity instanceof Player);
            if (canFireClick && firing == FiringType.CLICK || !client && firing == FiringType.AUTOMATIC)
                this.handlePlayerAmmoAndShootingOnClient(itemStack, entity, firing);
            return;
        }

        if (!client)
            this.tryStartBurstFire(itemStack, entity);

        RFEFirearmModeHandlingProperties firearmProperties = this.getHandlingProperties(itemStack);

        if (this.canOverheat) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            modeData = FirearmDataUtils.addHeat(modeData, firearmProperties.heatProperties().heatAddedOnFiring());
            modeData = FirearmDataUtils.setCoolingDelay(modeData, firearmProperties.heatProperties().coolingDelayTime());
            if (FirearmDataUtils.getHeat(modeData) > firearmProperties.heatProperties().heatCapacity())
                modeData = FirearmDataUtils.setOverheated(modeData, true);
            this.saveModeData(itemStack, modeData);
        }

        this.setCharged(itemStack, entity, false);
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.FIRING);
        if (entity instanceof ICanFireRFEFirearmItem aiShooter)
            aiShooter.ritchiesfirearmengine$onShotFired(itemStack);
        if (this.firingCooldown > 0) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            float extraActionTime = Math.max(FirearmDataUtils.getExtraFiringTime(modeData), 0);
            float time = this.firingCooldown + extraActionTime;
            int actionTime = Mth.floor(time);
            float remainder = time - actionTime;
            FirearmDataUtils.setActionTime(itemStack, actionTime);
            this.saveModeData(itemStack, FirearmDataUtils.setExtraFiringTime(modeData, remainder));
        }
        this.playFiringEffects(itemStack, entity);
    }

    protected boolean isWindingUp(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.isWindingUp(this.getModeData(itemStack));
    }

    protected void setWindingUp(ItemStack itemStack, LivingEntity entity, boolean windUp) {
        this.saveModeData(itemStack, FirearmDataUtils.setWindingUp(this.getModeData(itemStack), windUp));
    }

    public void playWindUpEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.windUpSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.windUpSound, SoundSource.NEUTRAL, 1, 1);
    }

    protected void handlePlayerAmmoAndShootingOnClient(ItemStack itemStack, LivingEntity entity, FiringType firing) {
        if (!FirearmDataUtils.isHoldingAttackKey(itemStack) && this.fireMode == FireMode.FULL_AUTO)
            return;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        RFEFirearmModeHandlingProperties handlingProperties = this.getHandlingProperties(itemStack);

        boolean jam = false;
        for (RFEMisfire misfireTest : handlingProperties.misfires()) {
            if (misfireTest.canMisfire(itemStack, entity)) {
                jam = true;
                break;
            }
        }
        this.setJammed(itemStack, entity, jam);

        List<RFEProjectileType> toFire = new ArrayList<>();
        if (!jam) {
            if (this.ammoRequired) {
                List<ItemStack> strippedSecondaries = List.of();
                if (this.requiresSecondaryAmmo)
                    strippedSecondaries = this.getNextSecondariesInItem(itemStack, entity, this.shotsFired, false, true);
                List<ItemStack> strippedAmmo = this.getNextRoundsInItem(itemStack, entity, this.shotsFired, false);
                for (ItemStack ammoStack : strippedAmmo) {
                    for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : ammoProperties.primaryAmmo().entrySet()) {
                        if (entry.getKey().test(ammoStack)) {
                            int sz = ammoStack.getCount();
                            RFEProjectileType projectileType = entry.getValue();
                            for (int i = 0; i < sz; ++i) {
                                if (this.requiresSecondaryAmmo) {
                                    ItemStack currentSecondary = FirearmDataUtils.stripAmmo(strippedSecondaries, this.ammoConsumedLast,
                                            this.trackEmptySecondarySlots, this.ignoreEmptySlotsWhenFiring);
                                    if (currentSecondary.isEmpty() || FirearmDataUtils.isUsedPrimer(currentSecondary))
                                        continue;
                                }
                                toFire.add(projectileType);
                            }
                            break;
                        }
                    }
                }
            } else {
                RFEProjectileType unlimitedProjectile = ammoProperties.unlimitedProjectile();
                if (unlimitedProjectile != null) {
                    // TODO one-time warning if no projectile?
                    for (int i = 0; i < this.shotsFired; ++i)
                        toFire.add(unlimitedProjectile);
                }
            }
        }

        InteractionHand hand = entity.getMainHandItem() == itemStack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        Vec3 pos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());

        List<RFEFiringInput> firingInputs = new ArrayList<>();

        for (RFEProjectileType type : toFire) {
            // TODO shooter positioning
            Vec3 aimDirection = entity.getViewVector(1f);
            ResourceLocation typeId = RFEProjectileTypeHandler.getProjectileTypeId(type);
            if (typeId != null) {
                // TODO warn once if missing?
                firingInputs.add(new RFEFiringInput(typeId, aimDirection, pos));
            }
        }

        RFERecoilClientImpulse impulse = new RFERecoilClientImpulse(RFEAimAngles.ZERO_ANGLES, RFEAimAngles.ZERO_ANGLES, 0);
        if (!firingInputs.isEmpty()) {
            RFERecoilInstance recoilInstance = RFERecoilManager.getRecoilInstance(entity, itemStack);
            RFERecoilProvider provider = RFERecoilProviderPackHandler.getRecoilProviders(itemStack).getProperties(this.modeId);
            if (recoilInstance == null || provider != RFERecoilManager.getCurrentProvider(entity, hand)) {
                recoilInstance = provider.createRecoilInstance(itemStack, entity, entity.getRandom());
                RFERecoilManager.trackRecoil(recoilInstance, provider, entity, itemStack, hand);
            }
            impulse = recoilInstance.updateRecoil(itemStack, entity);
        }

        UUID recoilUUID = RFERecoilManager.getRecoilId(itemStack);
        if (entity.level().isClientSide) {
            if (entity instanceof Player)
                RFENetwork.sendToServer(new ServerboundRunFiringLogicPacket(firingInputs, jam, hand, recoilUUID));
        } else {
            if (entity instanceof ServerPlayer splayer) {
                RFENetwork.sendToPlayer(new ClientboundRunFiringLogicPacket(hand, impulse, recoilUUID), splayer);
            } else if (!(entity instanceof Player)) {
                this.handleServerRecoil(itemStack, entity, hand, impulse, recoilUUID);
            }
            this.handleFiringInputOnServer(itemStack, entity, firingInputs, jam, recoilUUID, hand);
        }
    }

    public void handleServerRecoil(ItemStack itemStack, LivingEntity entity, InteractionHand hand,
                                   RFERecoilClientImpulse recoil, @Nullable UUID recoilUUID) {
        RFERecoilManager.setRecoilId(itemStack, recoilUUID);
        RFERecoilInstance recoilInstance = RFERecoilManager.getRecoilInstance(entity, itemStack);
        RFERecoilProvider provider = RFERecoilProviderPackHandler.getRecoilProviders(itemStack).getProperties(this.modeId);
        if (recoilInstance == null || provider != RFERecoilManager.getCurrentProvider(entity, hand)) {
            recoilInstance = provider.createRecoilInstance(itemStack, entity, entity.getRandom());
            RFERecoilManager.trackRecoil(recoilInstance, provider, entity, itemStack, hand);
        }
        recoilInstance.updateRecoilWithImpulse(itemStack, entity, recoil);
    }

    public void handleFiringInputOnServer(ItemStack itemStack, LivingEntity entity, List<RFEFiringInput> firingInputs,
                                          boolean jam, @Nullable UUID recoilUUID, InteractionHand hand) {
        this.setJammed(itemStack, entity, jam);
        if (firingInputs.isEmpty() || jam) {
            if (jam) {
                this.playMisfireEffects(itemStack, entity);
            } else if (this.canDryFire) {
                this.playDryFiringEffects(itemStack, entity);
            }
            this.setCharged(itemStack, entity, false);
            return;
        }

        RFERecoilManager.setRecoilId(itemStack, recoilUUID);
        if (this.ammoRequired) {
            this.getNextRoundsInItem(itemStack, entity, firingInputs.size(), true);
            if (this.requiresSecondaryAmmo)
                this.useOrRemoveNextSecondariesInItem(itemStack, entity, firingInputs.size());
        }

        RFESpreadInstance spreadInstance = RFESpreadManager.getSpreadInstance(entity, itemStack);
        RFESpreadProvider provider = RFESpreadProviderPackHandler.getSpreadProviders(itemStack).getProperties(this.modeId);
        if (spreadInstance == null || provider != RFESpreadManager.getCurrentProvider(entity, hand)) {
            spreadInstance = provider.createSpreadInstance(itemStack, entity, entity.getRandom());
            RFESpreadManager.trackSpread(spreadInstance, provider, entity, itemStack, hand);
        }

        for (RFEFiringInput input : firingInputs) {
            RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(input.projectile());
            if (type == null) {
                // TODO warn once?
                continue;
            }
            RFEProjectileInstance projectile = type.createInstance();
            projectile.setOwner(entity);
            projectile.setPosition(input.pos());

            Vec3 aimDirection = input.aim();
            projectile.shoot(aimDirection.x, aimDirection.y, aimDirection.z, this.getPitchAdjustment(itemStack), itemStack, entity, spreadInstance);
            spreadInstance.updateSpread(itemStack, entity);
            RFEProjectileManager.queueAddedProjectile(projectile, entity.level());
        }
        DataComponentPatch modeData = this.getModeData(itemStack);
        this.saveModeData(itemStack, FirearmDataUtils.setShotCount(modeData, FirearmDataUtils.getShotCount(modeData) + firingInputs.size()));

        this.fireFirearm(itemStack, entity, FiringType.EFFECTS);
    }

    public void playFiringEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.firingSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.firingSound, SoundSource.NEUTRAL, this.firingSoundRange, 1);
    }

    public void playDryFiringEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.dryFireSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.dryFireSound, SoundSource.NEUTRAL, 1, 1);
    }

    public void playMisfireEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.misfireSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.misfireSound, SoundSource.NEUTRAL, 1, 1);
    }

    public void onTickFiring(ItemStack itemStack, LivingEntity entity) {
        // TODO winding up and winding down
        DataComponentPatch stackData = itemStack.getComponentsPatch();
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
        if (this.isWindingUp(itemStack, entity)) {
            this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
            return;
        }
        if (this.ejectMagazineOnLastShot && this.getLoadedAmmoCount(itemStack, entity, true) == 0) {
            ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
            RFEItemUtils.addItemToEntity(previousMagazine, entity);
        }
        this.indexMagazine(itemStack, entity, this.slotsCycledAfterFiring);

        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);

        if (this.canOverheat && FirearmDataUtils.isOverheated(this.getModeData(itemStack))) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
            FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            return;
        }
        if (this.fireMode == FireMode.SINGLE_ACTION
                && this.automaticSingleActionCycle(itemStack, entity, holdingKey)
                && this.canChargeInternal(itemStack, entity)) {
            this.onCharge(itemStack, entity);
            return;
        }
        if (this.fireMode.isSelfLoading()) {
            if (this.isJammed(itemStack)) {
                return;
            } else {
                this.finishCharge(itemStack, entity);
            }
        }
        if (entity.level().isClientSide)
            return; // Handle automatic fire on the server only
        if (this.fireMode == FireMode.FULL_AUTO && this.canFireProjectile(itemStack, entity)) {
            if (!(itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKey) || holdAttackKey.isHoldingAttackKey(itemStack, entity))
                this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
            return;
        }
        if (this.fireMode == FireMode.BURST && this.canContinueBurstFire(itemStack, entity)) {
            this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
            this.decrementBurstFire(itemStack, entity);
        }
    }

    public boolean automaticSingleActionCycle(ItemStack itemStack, LivingEntity entity, boolean hold) {
        return this.getHandlingProperties(itemStack).chargingBehavior().canChargeAfterFiring(hold);
    }

    public boolean canContinueBurstFire(ItemStack itemStack, LivingEntity entity) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        int burstFireCount = FirearmDataUtils.getBurstFireCount(modeData);
        if (burstFireCount <= 0 || !this.canFireProjectile(itemStack, entity)) {
            this.saveModeData(itemStack, FirearmDataUtils.clearBurstFireCount(modeData));
            return false;
        } else {
            return true;
        }
    }

    public void tryStartBurstFire(ItemStack itemStack, LivingEntity entity) {
        if (!entity.level().isClientSide && this.fireMode == FireMode.BURST && this.burstRoundCount > 1 && !this.isBurstFiring(itemStack, entity))
            this.saveModeData(itemStack, FirearmDataUtils.setBurstFireCount(this.getModeData(itemStack), this.burstRoundCount - 1));
    }

    public void decrementBurstFire(ItemStack itemStack, LivingEntity entity) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        this.saveModeData(itemStack, FirearmDataUtils.setBurstFireCount(modeData, FirearmDataUtils.getBurstFireCount(modeData) - 1));
    }

    public boolean isBurstFiring(ItemStack itemStack, LivingEntity entity) {
        if (this.burstRoundCount <= 1 || this.fireMode != FireMode.BURST)
            return false;
        return FirearmDataUtils.getBurstFireCount(this.getModeData(itemStack)) > 0;
    }

    public void clearBurstFiring(ItemStack itemStack) {
        this.saveModeData(itemStack, FirearmDataUtils.clearBurstFireCount(this.getModeData(itemStack)));
    }

    public boolean tryRunningReloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType,
                                          boolean onInput, ReloadPhaseAccessFilter filter) {
        if (!this.ammoRequired)
            return false;
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (onInput && this.isBusyWithStagedAction(itemStack))
            return false;
        if (!this.reloadPhases.containsKey(phaseType))
            return false;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.reloadingCompareValues, itemStack, entity);
        for (ListIterator<ReloadPhase> lister = this.reloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (!filter.test(index) || !phase.test(compareContext))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.RELOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
            patched.set(RFEDataComponents.RELOAD_PHASE, phase.phaseType());
            patched.set(RFEDataComponents.RELOAD_PHASE_INDEX, index);
            this.saveModeData(itemStack, patched.asPatch());
            return true;
        }
        return false;
    }

    public void onTickReload(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        if (!this.ammoRequired && !this.requiresSecondaryAmmo) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
            return;
        }

        Optional<? extends ReloadPhase.PhaseType> phaseTypeO = this.getModeData(itemStack).get(RFEDataComponents.RELOAD_PHASE);
        if (phaseTypeO == null || phaseTypeO.isEmpty()) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
            return;
        }
        ReloadPhase.PhaseType phaseType = phaseTypeO.get();
        List<ReloadPhase> phaseList = this.reloadPhases.get(phaseType);
        Optional<? extends Integer> phaseIndexO = this.getModeData(itemStack).get(RFEDataComponents.RELOAD_PHASE_INDEX);
        int phaseIndex = phaseIndexO != null && phaseIndexO.isPresent() ? phaseIndexO.get() : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
            return;
        }
        ReloadPhase phase = phaseList.get(phaseIndex);

        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        phase.playEffects(itemStack, entity, phase.time() - actionTime);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.RELOAD) {
            this.executeReloadPhase(itemStack, entity, phase, actionTime);
        } else if (phaseType == ReloadPhase.PhaseType.UNLOAD) {
            this.executeUnloadPhase(itemStack, entity, phase, actionTime);
        }

        if (actionTime > 0)
            return;
        if (phaseType == ReloadPhase.PhaseType.INDEX)
            this.indexMagazine(itemStack, entity, phase.indexCount());
        if (phase.chargeFirearm())
            this.finishCharge(itemStack, entity);
        if (phase.ejectMagazine()) {
            ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
            RFEItemUtils.addItemToEntity(previousMagazine, entity);
        }
        if (phaseType == ReloadPhase.PhaseType.FINISH) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
            return;
        }
        if (this.shouldForceCancelAction(itemStack, entity)) {
            this.setForceCancelAction(itemStack, entity, false);
            if (!this.tryRunningReloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false, phase.accessiblePhases(ReloadPhase.PhaseType.FINISH)))
                this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
            return;
        }

        List<ReloadPhase.PhaseType> executionOrder = List.of(ReloadPhase.PhaseType.INDEX, ReloadPhase.PhaseType.UNLOAD,
                ReloadPhase.PhaseType.RELOAD, ReloadPhase.PhaseType.FINISH);
        for (ReloadPhase.PhaseType ptype : executionOrder) {
            if (this.tryRunningReloadAction(itemStack, entity, ptype, false, phase.accessiblePhases(ptype)))
                return;
        }
        this.saveModeData(itemStack, FirearmDataUtils.cancelReload(itemStack, this.getModeData(itemStack)));
    }

    public void indexMagazine(ItemStack itemStack, LivingEntity entity, int indexCount) {
        if (indexCount == 0)
            return;
        boolean reverse = indexCount < 0;
        indexCount = Math.abs(indexCount);
        DataComponentPatch modeData = this.getModeData(itemStack);
        List<ItemStack> ammoList;
        List<ItemStack> secondaryList;
        int primaryCapacity;
        int secondaryCapacity;
        if (!this.requiresAmmo()) {
            ammoList = new ArrayList<>();
            primaryCapacity = 0;
        } else if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            primaryCapacity = this.internalCapacity;
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
            primaryCapacity = magazineItem.getMagazineCapacity(magazine);
        }
        if (!this.requiresSecondaryAmmo()) {
            secondaryList = new ArrayList<>();
            secondaryCapacity = 0;
        } else if (this.internalSecondaryCapacity > 0) {
            secondaryList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            secondaryCapacity = this.internalSecondaryCapacity;
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
            secondaryCapacity = magazineItem.getSecondaryMagazineCapacity(magazine);
        }
        if (this.trackEmptySlots) {
            int diff = Math.max(0, primaryCapacity - RFEItemUtils.countItemsIncludingSlots(ammoList));
            for (int i = 0; i < diff; ++i)
                ammoList.add(ItemStack.EMPTY);
        }
        if (this.trackEmptySecondarySlots) {
            int diff = Math.max(0, secondaryCapacity - RFEItemUtils.countItemsIncludingSlots(secondaryList));
            for (int i = 0; i < diff; ++i)
                secondaryList.add(ItemStack.EMPTY);
        }

        if (ammoList.isEmpty() && secondaryList.isEmpty())
            return;

        if (!ammoList.isEmpty()) {
            int count = indexCount;
            while (count > 0) {
                int stripIndex = reverse ? ammoList.size() - 1 : 0;
                ItemStack strip = ammoList.get(stripIndex);
                int deductible = Math.min(count, strip.isEmpty() ? 1 : strip.getCount());
                ItemStack split = strip.isEmpty() ? ItemStack.EMPTY : strip.split(deductible);
                if (strip.isEmpty())
                    ammoList.remove(stripIndex);
                int addIndex = reverse ? 0 : ammoList.size();
                if (split.isEmpty()) {
                    ammoList.add(addIndex, split);
                } else if (ammoList.isEmpty()) {
                    ammoList.add(split);
                } else {
                    ItemStack next = ammoList.get(reverse ? 0 : ammoList.size() - 1);
                    if (ItemStack.isSameItemSameComponents(next, split)) {
                        int maxAddable = Math.min(next.getMaxStackSize() - split.getCount(), split.getCount());
                        next.grow(maxAddable);
                        split.shrink(maxAddable);
                    }
                    if (!split.isEmpty())
                        ammoList.add(addIndex, split);
                }
                count -= deductible;
            }
        }
        if (!secondaryList.isEmpty()) {
            int secondaryCount = indexCount;
            while (secondaryCount > 0) {
                int stripIndex = reverse ? secondaryList.size() - 1 : 0;
                ItemStack strip = secondaryList.get(stripIndex);
                int deductible = Math.min(secondaryCount, strip.isEmpty() ? 1 : strip.getCount());
                ItemStack split = strip.isEmpty() ? ItemStack.EMPTY : strip.split(deductible);
                if (strip.isEmpty())
                    secondaryList.remove(stripIndex);
                int addIndex = reverse ? 0 : secondaryList.size();
                if (split.isEmpty()) {
                    secondaryList.add(addIndex, split);
                } else if (secondaryList.isEmpty()) {
                    secondaryList.add(split);
                } else {
                    ItemStack next = secondaryList.get(reverse ? 0 : secondaryList.size() - 1);
                    if (ItemStack.isSameItemSameComponents(next, split)) {
                        int maxAddable = Math.min(next.getMaxStackSize() - split.getCount(), split.getCount());
                        next.grow(maxAddable);
                        split.shrink(maxAddable);
                    }
                    if (!split.isEmpty())
                        secondaryList.add(addIndex, split);
                }
                secondaryCount -= deductible;
            }
        }
        if (this.requiresAmmo()) {
            if (this.internalCapacity > 0) {
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS, ammoList));
            } else {
                Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
                if (o == null || o.isEmpty())
                    return;
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    magazineItem.writeStoredAmmo(magazine, ammoList);
                    PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                    patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                    this.saveModeData(itemStack, patched.asPatch());
                }
            }
        }
        modeData = this.getModeData(itemStack);
        if (this.requiresSecondaryAmmo()) {
            if (this.internalSecondaryCapacity > 0) {
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS, secondaryList));
            } else {
                Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
                if (o == null || o.isEmpty())
                    return;
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    magazineItem.writeStoredSecondaryAmmo(magazine, secondaryList);
                    PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                    patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                    this.saveModeData(itemStack, patched.asPatch());
                }
            }
        }
    }

    public void executeReloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        boolean unlimitedItemReloading = itemStack.getOrDefault(RFEDataComponents.USING_UNLIMITED_AMMO_RELOAD, false);

        int actualTime = phase.time() - actionTime;
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.reloadMagazineTime() == actualTime) {
                Predicate<ItemStack> magPred = RFEUtils.orAllPredicates(ammoProperties.magazines());
                Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
                ItemStack foundMagazine = unlimitedItemReloading ? ItemStack.EMPTY
                        : RFEItemUtils.findFullestMagazine(entity, magPred, ammoPred, true);
                if (foundMagazine.isEmpty() && RFEFirearmItem.canEntityInfiniteReload(entity, itemStack)) {
                    List<ItemStack> infiniteAmmo = ammoProperties.unlimitedPrimaryReloadItems();
                    for (ItemStack s : infiniteAmmo) {
                        if (!magPred.test(s))
                            continue;
                        foundMagazine = s.copy();
                        break;
                    }
                }
                if (!foundMagazine.isEmpty())
                    this.setMagazine(itemStack, entity, foundMagazine);
            }
            return;
        }
        int reloadCount = phase.reloadsAtTime(actualTime);
        if (reloadCount < 1)
            return;
        boolean addedLast = phase.ammoAddedLast();
        if (phase.reloadType() == ReloadPhase.ReloadType.SECONDARIES) {
            Predicate<ItemStack> secondaryPred = RFEUtils.orAllPredicates(ammoProperties.secondaryAmmo());
            List<ItemStack> foundSecondaries = new LinkedList<>();
            List<ItemStack> secondaryList;
            int addable;
            if (this.internalSecondaryCapacity > 0) {
                secondaryList = FirearmDataUtils.getRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_PRIMERS);
                addable = Mth.clamp(this.internalSecondaryCapacity - RFEItemUtils.countItems(secondaryList), 0, reloadCount);
            } else {
                Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.DETACHED_MAGAZINE);
                if (o == null || o.isEmpty())
                    return;
                ItemStack magazine = o.get().copyOne();
                if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                    return;
                secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
                addable = Mth.clamp(magazineItem.getSecondaryMagazineCapacity(magazine) - RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer)), 0, reloadCount);
            }
            if (!unlimitedItemReloading) {
                RFEItemUtils.consumeItemsFromEntity(entity, secondaryPred.and(s -> s != itemStack && !FirearmDataUtils.isUsedPrimer(s)), s -> {
                    int takeAmount = Math.min(addable - RFEItemUtils.countItems(foundSecondaries), s.getMaxStackSize());
                    ItemStack addition;
                    if (s.is(RFEItemTags.INFINITE_AMMO.tag)) {
                        addition = s.copyWithCount(takeAmount);
                    } else {
                        addition = s.split(takeAmount);
                    }
                    FirearmDataUtils.addAmmo(foundSecondaries, addition, false, false);
                    return s.isEmpty() ? ItemStack.EMPTY : s;
                }, () -> foundSecondaries.size() >= addable || !foundSecondaries.isEmpty() && foundSecondaries.get(foundSecondaries.size() - 1).is(RFEItemTags.INFINITE_AMMO.tag));
            }
            if (foundSecondaries.isEmpty() && RFEFirearmItem.canEntityInfiniteReload(entity, itemStack)) {
                List<ItemStack> infiniteSecondaries = ammoProperties.unlimitedSecondaryReloadItems();
                for (ItemStack s : infiniteSecondaries) {
                    if (!secondaryPred.test(s))
                        continue;
                    foundSecondaries.add(s.copyWithCount(reloadCount));
                    break;
                }
            }
            if (!foundSecondaries.isEmpty()) {
                for (ItemStack sourceStack : foundSecondaries)
                    FirearmDataUtils.addAmmo(secondaryList, sourceStack, addedLast, this.trackEmptySecondarySlots, 0);
            }
            if (this.internalSecondaryCapacity > 0) {
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_PRIMERS, secondaryList));
            } else {
                Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.DETACHED_MAGAZINE);
                if (o == null || o.isEmpty())
                    return;
                ItemStack magazine = o.get().copyOne();
                if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                    return;
                magazineItem.writeStoredSecondaryAmmo(magazine, secondaryList);
                PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
                patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                this.saveModeData(itemStack, patched.asPatch());
            }
            return;
        }
        boolean replaceChamberedRound = phase.replaceChamberedRound() && !addedLast;
        List<ItemStack> ammoList;
        int capacity;
        ItemStack chambered = ItemStack.EMPTY;
        boolean reloadPlusOneDirectly = false;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_ROUNDS);
            if (this.trackEmptySlots) {
                int diff = this.internalCapacity - RFEItemUtils.countItemsIncludingSlots(ammoList);
                for (int i = 0; i < diff; ++i)
                    ammoList.add(ItemStack.EMPTY);
            }
            capacity = this.internalCapacity;
            if (!phase.ammoAddedLast() && !replaceChamberedRound)
                chambered = FirearmDataUtils.stripAmmo(ammoList, false, false, this.trackEmptySlots);
        } else {
            Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty()) {
                ammoList = new ArrayList<>();
                capacity = 0;
            } else {
                ItemStack magazine = o.get().copyOne();
                if (magazine.getItem() instanceof MagazineItem magazineItem) {
                    ammoList = magazineItem.getStoredAmmo(magazine);
                    capacity = magazineItem.getMagazineCapacity(magazine);
                } else {
                    ammoList = new ArrayList<>();
                    capacity = 0;
                }
            }
            if (this.plusOneCapacity && replaceChamberedRound) {
                if (capacity == 0)
                    reloadPlusOneDirectly = true;
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    if (capacity == 0) {
                        chambered = loadedRound;
                    } else {
                        FirearmDataUtils.addAmmo(ammoList, loadedRound, false, this.trackEmptySlots, 0);
                    }
                    this.setLoadedRound(itemStack, ItemStack.EMPTY);
                }
                ++capacity;
            } else if (!this.plusOneCapacity && !replaceChamberedRound) {
                chambered = FirearmDataUtils.stripAmmo(ammoList, false, true, this.trackEmptySlots);
            }
        }
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        if (phase.reloadType() == ReloadPhase.ReloadType.ROUNDS) {
            int addable = Mth.clamp(capacity - RFEItemUtils.countItems(ammoList), 0, reloadCount);
            if (addable > 0) {
                List<ItemStack> foundAmmo = new LinkedList<>();
                if (!unlimitedItemReloading) {
                    RFEItemUtils.consumeItemsFromEntity(entity, ammoPred.and(s -> s != itemStack), s -> {
                        int takeAmount = Math.min(addable - RFEItemUtils.countItems(foundAmmo), s.getMaxStackSize());
                        ItemStack addition;
                        if (s.is(RFEItemTags.INFINITE_AMMO.tag)) {
                            addition = s.copyWithCount(takeAmount);
                        } else {
                            addition = s.split(takeAmount);
                        }
                        FirearmDataUtils.addAmmo(foundAmmo, addition, false, false);
                        return s.isEmpty() ? ItemStack.EMPTY : s;
                    }, () -> foundAmmo.size() >= addable || !foundAmmo.isEmpty() && foundAmmo.get(foundAmmo.size() - 1).is(RFEItemTags.INFINITE_AMMO.tag));
                }
                if (foundAmmo.isEmpty() && RFEFirearmItem.canEntityInfiniteReload(entity, itemStack)) {
                    List<ItemStack> infiniteRounds = ammoProperties.unlimitedPrimaryReloadItems();
                    for (ItemStack s : infiniteRounds) {
                        if (!ammoPred.test(s))
                            continue;
                        foundAmmo.add(s.copyWithCount(addable));
                        break;
                    }
                }
                if (!foundAmmo.isEmpty()) {
                    for (ItemStack sourceStack : foundAmmo)
                        FirearmDataUtils.addAmmo(ammoList, sourceStack, addedLast, this.trackEmptySlots, 0);
                }
            }
        } else if (phase.reloadType() == ReloadPhase.ReloadType.SPEEDLOADERS) {
            Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(ammoProperties.speedloaders());
            int reloadCount1 = capacity - this.getLoadedAmmoCount(itemStack, entity, false);
            ItemStack bestSpeedloaderStack = unlimitedItemReloading ? ItemStack.EMPTY
                    : RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, true);
            if (bestSpeedloaderStack.isEmpty() && RFEFirearmItem.canEntityInfiniteReload(entity, itemStack)) {
                List<ItemStack> infiniteSpeedloader = ammoProperties.unlimitedPrimaryReloadItems();
                for (ItemStack s : infiniteSpeedloader) {
                    if (!speedloaderPred.test(s))
                        continue;
                    bestSpeedloaderStack = s.copy();
                    break;
                }
            }
            if (bestSpeedloaderStack.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> strippedAmmo = magazineItem.getStoredAmmo(bestSpeedloaderStack);
                int consumed = FirearmDataUtils.addMultipleAmmo(ammoList, strippedAmmo, addedLast, false, this.trackEmptySlots, capacity);
                FirearmDataUtils.stripMultipleAmmo(strippedAmmo, consumed, false, false, false /* TODO track empty slots */, true);
                magazineItem.writeStoredAmmo(bestSpeedloaderStack, strippedAmmo);
                RFEItemUtils.addItemToEntity(bestSpeedloaderStack, entity);
            }
        }
        if (!chambered.isEmpty())
            FirearmDataUtils.addAmmo(ammoList, chambered, false, this.trackEmptySlots);
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalCapacity > 0) {
            this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS, ammoList));
        } else if (reloadPlusOneDirectly) {
            ItemStack top = FirearmDataUtils.stripAmmo(ammoList, false, false, false); // rather sloppy but whatever --ritchie
            this.setLoadedRound(itemStack, top);
            for (ItemStack remainder : ammoList)
                RFEItemUtils.addItemToEntity(remainder, entity);
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            magazineItem.writeStoredAmmo(magazine, ammoList);
            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
            patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
            this.saveModeData(itemStack, patched.asPatch());
        }
    }

    public boolean canCancelReloadOrUnloadByClick(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        if (action != RFEFirearmItem.Action.RELOAD && action != RFEFirearmItem.Action.UNLOAD)
            return false;
        DataComponentType<ReloadPhase.PhaseType> type = action == RFEFirearmItem.Action.RELOAD ? RFEDataComponents.RELOAD_PHASE : RFEDataComponents.UNLOAD_PHASE;
        Optional<? extends ReloadPhase.PhaseType> o = this.getModeData(itemStack).get(type);
        ReloadPhase.PhaseType phaseType = o != null && o.isPresent() ? o.get() : null;
        return phaseType == ReloadPhase.PhaseType.RELOAD || phaseType == ReloadPhase.PhaseType.UNLOAD;
    }

    public void setForceCancelAction(ItemStack itemStack, LivingEntity entity, boolean forceCancelAction) {
        this.saveModeData(itemStack, FirearmDataUtils.setForceCancelAction(this.getModeData(itemStack), forceCancelAction));
    }

    public boolean shouldForceCancelAction(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.shouldForceCancelAction(this.getModeData(itemStack));
    }

    public boolean tryRunningUnloadAction(ItemStack itemStack, LivingEntity entity, ReloadPhase.PhaseType phaseType,
                                          boolean onInput, ReloadPhaseAccessFilter filter) {
        if (!this.ammoRequired)
            return false;
        if (FirearmDataUtils.getActionTime(itemStack) > 0)
            return false;
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (onInput && this.isBusyWithStagedAction(itemStack))
            return false;
        if (!this.unloadPhases.containsKey(phaseType))
            return false;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.unloadingCompareValues, itemStack, entity);
        for (ListIterator<ReloadPhase> lister = this.unloadPhases.get(phaseType).listIterator(); lister.hasNext(); ) {
            int index = lister.nextIndex();
            ReloadPhase phase = lister.next();
            if (!filter.test(index) || !phase.test(compareContext))
                continue;
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.UNLOAD);
            FirearmDataUtils.setActionTime(itemStack, phase.time());
            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
            patched.set(RFEDataComponents.UNLOAD_PHASE, phase.phaseType());
            patched.set(RFEDataComponents.UNLOAD_PHASE_INDEX, index);
            this.saveModeData(itemStack, patched.asPatch());
            return true;
        }
        return false;
    }

    public void onTickUnload(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        if (!this.ammoRequired && !this.requiresSecondaryAmmo) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
            return;
        }

        Optional<? extends ReloadPhase.PhaseType> phaseTypeO = this.getModeData(itemStack).get(RFEDataComponents.UNLOAD_PHASE);
        if (phaseTypeO == null || phaseTypeO.isEmpty()) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
            return;
        }
        ReloadPhase.PhaseType phaseType = phaseTypeO.get();
        List<ReloadPhase> phaseList = this.unloadPhases.get(phaseType);
        Optional<? extends Integer> phaseIndexO = this.getModeData(itemStack).get(RFEDataComponents.UNLOAD_PHASE_INDEX);
        int phaseIndex = phaseIndexO != null && phaseIndexO.isPresent() ? phaseIndexO.get() : -1;
        if (phaseIndex < 0 || phaseList.size() <= phaseIndex) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
            return;
        }
        ReloadPhase phase = phaseList.get(phaseIndex);

        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        phase.playEffects(itemStack, entity, phase.time() - actionTime);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (phaseType == ReloadPhase.PhaseType.UNLOAD)
            this.executeUnloadPhase(itemStack, entity, phase, actionTime);

        if (actionTime > 0)
            return;
        if (phaseType == ReloadPhase.PhaseType.INDEX)
            this.indexMagazine(itemStack, entity, phase.indexCount());
        if (phase.chargeFirearm())
            this.finishCharge(itemStack, entity);
        if (phaseType == ReloadPhase.PhaseType.FINISH) {
            this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
            return;
        }

        if (this.shouldForceCancelAction(itemStack, entity)) {
            this.setForceCancelAction(itemStack, entity, false);
            if (!this.tryRunningUnloadAction(itemStack, entity, ReloadPhase.PhaseType.FINISH, false, phase.accessiblePhases(ReloadPhase.PhaseType.FINISH)))
                this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
            return;
        }

        List<ReloadPhase.PhaseType> executionOrder = List.of(ReloadPhase.PhaseType.INDEX, ReloadPhase.PhaseType.UNLOAD, ReloadPhase.PhaseType.FINISH);
        for (ReloadPhase.PhaseType ptype : executionOrder) {
            if (this.tryRunningUnloadAction(itemStack, entity, ptype, false, phase.accessiblePhases(ptype)))
                return;
        }
        this.saveModeData(itemStack, FirearmDataUtils.cancelUnload(itemStack, this.getModeData(itemStack)));
    }

    public void executeUnloadPhase(ItemStack itemStack, LivingEntity entity, ReloadPhase phase, int actionTime) {
        int actualTime = phase.time() - actionTime;
        if (phase.reloadType() == ReloadPhase.ReloadType.MAGAZINES) {
            if (phase.reloadMagazineTime() == actualTime) {
                ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
                RFEItemUtils.addItemToEntity(previousMagazine, entity);
            }
            return;
        }
        int unloadCount = phase.reloadsAtTime(actualTime);
        if (unloadCount < 1)
            return;
        if (phase.reloadType() == ReloadPhase.ReloadType.SECONDARIES) {
            List<ItemStack> items = List.of();
            if (this.internalSecondaryCapacity > 0) {
                List<ItemStack> secondaryList = FirearmDataUtils.getRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_PRIMERS);
                if (this.trackEmptySecondarySlots) {
                    int diff = this.internalSecondaryCapacity - RFEItemUtils.countItemsIncludingSlots(secondaryList);
                    for (int i = 0; i < diff; ++i)
                        secondaryList.add(ItemStack.EMPTY);
                }
                items = FirearmDataUtils.stripMultipleAmmo(secondaryList, unloadCount, phase.ammoAddedLast(), false, this.trackEmptySecondarySlots, true);
                this.saveModeData(itemStack, FirearmDataUtils.saveRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_PRIMERS, secondaryList));
            } else {
                if (this.plusOneSecondaryCapacity) {
                    ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                    if (!loadedPrimer.isEmpty())
                        RFEItemUtils.addItemToEntity(loadedPrimer, entity);
                }
                DataComponentPatch modeData = this.getModeData(itemStack);
                Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
                if (o == null || o.isEmpty())
                    return;
                ItemStack magazine = o.get().copyOne();
                if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                    return;
                List<ItemStack> secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
                items = FirearmDataUtils.stripMultipleAmmo(secondaryList, unloadCount, phase.ammoAddedLast(), false, this.trackEmptySecondarySlots, true);
                magazineItem.writeStoredSecondaryAmmo(magazine, secondaryList);
                PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                this.saveModeData(itemStack, patched.asPatch());
            }
            for (ItemStack item : items)
                RFEItemUtils.addItemToEntity(item, entity);
            return;
        }
        DataComponentPatch modeData = this.getModeData(itemStack);
        List<ItemStack> ammoList;
        if (this.internalCapacity > 0) {
            ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            if (this.trackEmptySlots) {
                int diff = this.internalCapacity - RFEItemUtils.countItemsIncludingSlots(ammoList);
                for (int i = 0; i < diff; ++i)
                    ammoList.add(ItemStack.EMPTY);
            }
        } else {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (!loadedRound.isEmpty()) {
                    RFEItemUtils.addItemToEntity(loadedRound, entity);
                    this.setLoadedRound(itemStack, ItemStack.EMPTY);
                    unloadCount -= 1;
                    if (unloadCount < 1)
                        return;
                }
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            ammoList = magazineItem.getStoredAmmo(magazine);
        }
        List<ItemStack> items = FirearmDataUtils.stripMultipleAmmo(ammoList, unloadCount, phase.ammoAddedLast(), false, this.trackEmptySlots, true);
        for (ItemStack item : items)
            RFEItemUtils.addItemToEntity(item, entity);
        if (this.internalCapacity > 0) {
            this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS, ammoList));
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return;
            magazineItem.writeStoredAmmo(magazine, ammoList);
            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
            patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
            this.saveModeData(itemStack, patched.asPatch());
        }
    }

    public ItemStack setMagazine(ItemStack itemStack, LivingEntity entity, ItemStack magazineStack) {
        if (this.internalCapacity > 0)
            return ItemStack.EMPTY;
        DataComponentPatch modeData = this.getModeData(itemStack);
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
        ItemStack previous = patched.getOrDefault(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.EMPTY).copyOne();
        if (magazineStack.isEmpty()) {
            patched.remove(RFEDataComponents.DETACHED_MAGAZINE);
        } else {
            patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazineStack)));
        }
        this.saveModeData(itemStack, patched.asPatch());
        return previous;
    }

    public void setCharged(ItemStack itemStack, LivingEntity entity, boolean charged) {
        this.saveModeData(itemStack, FirearmDataUtils.setCharged(this.getModeData(itemStack), charged));
    }

    public boolean canChargeInternal(ItemStack itemStack, LivingEntity entity) {
        return !this.isBusyWithStagedAction(itemStack) && FirearmDataUtils.getActionTime(itemStack) <= 0;
    }

    public void onCharge(ItemStack itemStack, LivingEntity entity) {
        if (entity.level().isClientSide)
            return;
        Map<ResourceLocation, Float> compareContext = FirearmCondition.evaluateCompareValueSources(this.chargingCompareValues, itemStack, entity);
        for (ListIterator<ChargeAction> lister = this.chargeActions.listIterator(); lister.hasNext(); ) {
            int i = lister.nextIndex();
            ChargeAction action = lister.next();
            if (action.canExecute(itemStack, entity, compareContext)) {
                this.setChargeAction(itemStack, entity, i, action);
                action.playEffects(itemStack, entity, 0);
                return;
            }
        }
    }

    public void setChargeAction(ItemStack itemStack, LivingEntity entity, int index, ChargeAction action) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
        patched.set(RFEDataComponents.CHARGE_ACTION, index);
        this.saveModeData(itemStack, patched.asPatch());
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.CHARGING);
        FirearmDataUtils.setActionTime(itemStack, action.time());
    }

    public void resetChargeAction(ItemStack itemStack, LivingEntity entity) {
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
        patched.remove(RFEDataComponents.CHARGE_ACTION);
        this.saveModeData(itemStack, patched.asPatch());
        FirearmDataUtils.setAction(itemStack, null);
        FirearmDataUtils.setActionTime(itemStack, 0);
    }

    @Nullable
    public ChargeAction getCurrentChargeAction(ItemStack itemStack, LivingEntity entity) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        Optional<? extends Integer> o = modeData.get(RFEDataComponents.CHARGE_ACTION);
        if (o == null || o.isEmpty())
            return null;
        int index = o.get();
        return 0 <= index && index < this.chargeActions.size() ? this.chargeActions.get(index) : null;
    }

    public void onTickCharging(ItemStack itemStack, LivingEntity entity) {
        ChargeAction action = this.getCurrentChargeAction(itemStack, entity);
        if (action == null) {
            this.resetChargeAction(itemStack, entity);
            return;
        }
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);

        int actualTime = action.time() - actionTime;
        action.playEffects(itemStack, entity, actualTime);

        if (actionTime > 0)
            return;

        boolean isClientside = entity.level().isClientSide;
        if (!isClientside && action.cycleMagazine())
            this.indexMagazine(itemStack, entity, 1);
        this.finishCharge(itemStack, entity);
        if (!isClientside && action.ejectMagazine()) {
            ItemStack previousMagazine = this.setMagazine(itemStack, entity, ItemStack.EMPTY);
            RFEItemUtils.addItemToEntity(previousMagazine, entity);
        }

        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);
        if (!isClientside && this.fireMode == FireMode.SINGLE_ACTION && this.slamfire && holdingKey) {
            this.fireFirearm(itemStack, entity, FiringType.AUTOMATIC);
        } else {
            FirearmDataUtils.setAction(itemStack, null);
        }
    }

    public void finishCharge(ItemStack itemStack, LivingEntity entity) {
        this.setCharged(itemStack, entity, true);
        this.setJammed(itemStack, entity, false);
        if (entity.level().isClientSide)
            return;
        if (this.canOverheat) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            float heat = FirearmDataUtils.getHeat(modeData);
            heat -= this.getHandlingProperties(itemStack).heatProperties().heatRemovedOnCharge();
            heat = Math.max(0, heat);
            modeData = FirearmDataUtils.setHeat(modeData, heat);
            this.saveModeData(itemStack, modeData);
        }

        boolean shouldJam = false;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        if (this.requiresAmmo() && ammoProperties.unlimitedProjectile() == null) {
            if (this.plusOneCapacity) {
                ItemStack loadedRound = this.getLoadedRound(itemStack);
                if (loadedRound.isEmpty()) {
                    List<ItemStack> nextAmmoList = this.getNextRoundsInItem(itemStack, entity, 1, true);
                    ItemStack nextAmmoStack = FirearmDataUtils.stripAmmo(nextAmmoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                    this.setLoadedRound(itemStack, nextAmmoStack);
                    if (nextAmmoStack.isEmpty())
                        this.setCharged(itemStack, entity, false);
                }
            } else {
                List<ItemStack> nextRound = this.getNextRoundsInItem(itemStack, entity, 1, false);
                if (nextRound.isEmpty())
                    this.setCharged(itemStack, entity, false);
            }
            ItemStack chamberedRound = this.getChamberedRound(itemStack);
            if (!chamberedRound.isEmpty()) {
                shouldJam = true;
                for (AmmoPredicate pred : ammoProperties.primaryAmmo().keySet()) {
                    if (pred.test(chamberedRound)) {
                        shouldJam = false;
                        break;
                    }
                }
            }
        }
        if (this.requiresSecondaryAmmo()) {
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (loadedPrimer.isEmpty() || FirearmDataUtils.isUsedPrimer(loadedPrimer)) {
                    List<ItemStack> nextAmmoList = this.getNextSecondariesInItem(itemStack, entity, 1, true, true);
                    ItemStack nextPrimerStack = FirearmDataUtils.stripAmmo(nextAmmoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                    this.setLoadedPrimer(itemStack, nextPrimerStack);
                    if (nextPrimerStack.isEmpty() || FirearmDataUtils.isUsedPrimer(nextPrimerStack))
                        this.setCharged(itemStack, entity, false);
                }
            } else {
                List<ItemStack> nextPrimer = this.getNextRoundsInItem(itemStack, entity, 1, false);
                if (nextPrimer.isEmpty() || FirearmDataUtils.isUsedPrimer(nextPrimer.getFirst()))
                    this.setCharged(itemStack, entity, false);
            }
            ItemStack chamberedPrimer = this.getChamberedSecondary(itemStack);
            if (!chamberedPrimer.isEmpty()) {
                shouldJam = true;
                if (!FirearmDataUtils.isUsedPrimer(chamberedPrimer)) {
                    for (AmmoPredicate pred : ammoProperties.secondaryAmmo()) {
                        if (pred.test(chamberedPrimer)) {
                            shouldJam = false;
                            break;
                        }
                    }
                }
            }
        }
        if (shouldJam) {
            this.setCharged(itemStack, entity, false);
            this.setJammed(itemStack, entity, true);
            if (this.requiresAmmo() && ammoProperties.unlimitedProjectile() == null) {
                if (this.plusOneCapacity) {
                    ItemStack loadedRound = this.getLoadedRound(itemStack);
                    if (!loadedRound.isEmpty()) {
                        this.setLoadedRound(itemStack, ItemStack.EMPTY);
                        RFEItemUtils.addItemToEntity(loadedRound, entity);
                    }
                } else if (this.internalCapacity > 0) {
                    DataComponentPatch modeData = this.getModeData(itemStack);
                    List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
                    ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                    this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS, ammoList));
                    if (!ejected.isEmpty())
                        RFEItemUtils.addItemToEntity(ejected, entity);
                } else {
                    DataComponentPatch modeData = this.getModeData(itemStack);
                    Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
                    if (o != null && o.isPresent()) {
                        ItemStack magazine = o.get().copyOne();
                        if (magazine.getItem() instanceof MagazineItem magazineItem) {
                            List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                            ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySlots);
                            magazineItem.writeStoredAmmo(magazine, ammoList);
                            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                            patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                            this.saveModeData(itemStack, patched.asPatch());
                            if (!ejected.isEmpty())
                                RFEItemUtils.addItemToEntity(ejected, entity);
                        }
                    }
                }
            }
            if (this.requiresSecondaryAmmo()) {
                if (this.plusOneSecondaryCapacity) {
                    ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                    if (!loadedPrimer.isEmpty()) {
                        this.setLoadedPrimer(itemStack, ItemStack.EMPTY);
                        RFEItemUtils.addItemToEntity(loadedPrimer, entity);
                    }
                } else if (this.internalSecondaryCapacity > 0) {
                    DataComponentPatch modeData = this.getModeData(itemStack);
                    List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
                    ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySecondarySlots);
                    this.saveModeData(itemStack, FirearmDataUtils.saveRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS, ammoList));
                    if (!ejected.isEmpty())
                        RFEItemUtils.addItemToEntity(ejected, entity);
                } else {
                    DataComponentPatch modeData = this.getModeData(itemStack);
                    Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
                    if (o != null && o.isPresent()) {
                        ItemStack magazine = o.get().copyOne();
                        if (magazine.getItem() instanceof MagazineItem magazineItem) {
                            List<ItemStack> ammoList = magazineItem.getStoredSecondaryAmmo(magazine);
                            ItemStack ejected = FirearmDataUtils.stripAmmo(ammoList, this.ammoConsumedLast, false, this.trackEmptySecondarySlots);
                            magazineItem.writeStoredSecondaryAmmo(magazine, ammoList);
                            PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, modeData);
                            patched.set(RFEDataComponents.DETACHED_MAGAZINE, RFEItemContainerContents.fromItems(List.of(magazine)));
                            this.saveModeData(itemStack, patched.asPatch());
                            if (!ejected.isEmpty())
                                RFEItemUtils.addItemToEntity(ejected, entity);
                        }
                    }
                }
            }
        }
    }

    public void setJammed(ItemStack itemStack, LivingEntity entity, boolean jammed) {
        this.saveModeData(itemStack, FirearmDataUtils.setJammed(this.getModeData(itemStack), jammed));
    }

    public boolean isJammed(ItemStack itemStack) {
        return FirearmDataUtils.isJammed(this.getModeData(itemStack));
    }

    public void onTickDraw(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.drawTime)
            this.playDrawEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playDrawEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.drawSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.drawSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void onTickCooldown(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.cooldownTime)
            this.playCooldownEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        DataComponentPatch modeData = this.getModeData(itemStack);
        modeData = FirearmDataUtils.setOverheated(modeData, false);
        modeData = FirearmDataUtils.setHeat(modeData, 0);
        this.saveModeData(itemStack, modeData);
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playCooldownEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.cooldownSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.cooldownSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void startSwitchMode(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.SWITCH_MODE);
        FirearmDataUtils.setActionTime(itemStack, this.modeChangeTime);
    }

    public void playSwitchModeEffects(ItemStack itemStack, LivingEntity entity) {
        if (this.modeChangeSound != null)
            entity.level().playSound(null, entity.blockPosition(), this.modeChangeSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void onTickSwitchMode(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.modeChangeTime)
            this.playSwitchModeEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        if (itemStack.getItem() instanceof RFEFirearmItem firearmItem)
            firearmItem.completeSwitchMode(itemStack, entity);
        FirearmDataUtils.setAction(itemStack, null);
    }

    public boolean canAim(ItemStack itemStack, LivingEntity entity) {
        if (!this.canAim || this.isMeleeing(itemStack))
            return false;
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        if (action != null && !action.canAim())
            return false;
        if (action == RFEFirearmItem.Action.SWITCH_MODE)
            ; // TODO do not allow switch mode if transition isn't "seamless" (e.g. safety toggle, mechanism switch)
        return true;
    }

    public void startAiming(ItemStack itemStack, LivingEntity entity) {
        if (!this.canAim) {
            FirearmDataUtils.setAiming(itemStack, false);
            this.setAimingTime(itemStack, entity, 0);
            return;
        }
        FirearmDataUtils.setAiming(itemStack, true);
        int currentUnaimingTime = this.getAimingTime(itemStack, entity);
        float frac = this.unaimTime == 0 ? 0 : (float) currentUnaimingTime / (float) this.unaimTime;
        frac = 1f - frac;
        this.setAimingTime(itemStack, entity, Mth.ceil(this.aimTime * frac));
        if (this.aimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.aimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public void stopAiming(ItemStack itemStack, LivingEntity entity) {
        FirearmDataUtils.setAiming(itemStack, false);
        if (!this.canAim) {
            this.setAimingTime(itemStack, entity, 0);
            return;
        }
        int currentAimingTime = this.getAimingTime(itemStack, entity);
        float frac = this.aimTime == 0 ? 0 : (float) currentAimingTime / (float) this.aimTime;
        frac = 1f - frac;
        this.setAimingTime(itemStack, entity, Mth.ceil(this.unaimTime * frac));
        entity.stopUsingItem();
        if (this.unaimSound != null)
            entity.level().playSound(entity, entity.blockPosition(), this.unaimSound, SoundSource.NEUTRAL, 1f, 1f);
    }

    public boolean isAiming(ItemStack itemStack, LivingEntity entity) {
        return entity instanceof Player ? entity.isUsingItem() : FirearmDataUtils.isAiming(itemStack);
    }

    public int getAimingTime(ItemStack itemStack, LivingEntity entity) {
        return FirearmDataUtils.getAimingTime(itemStack);
    }

    public void setAimingTime(ItemStack itemStack, LivingEntity entity, int time) {
        FirearmDataUtils.setAimingTime(itemStack, time);
    }

    public int aimTime() { return this.aimTime; }

    public int unaimTime() { return this.unaimTime; }

    public void onTick(ItemStack itemStack, LivingEntity entity, boolean selected) {
        if (!selected || !FirearmDataUtils.isEquipped(itemStack)) {
            FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.DRAW);
            FirearmDataUtils.setActionTime(itemStack, this.drawTime);
            FirearmDataUtils.setEquipped(itemStack, true);
            FirearmDataUtils.setAiming(itemStack, false);
            FirearmDataUtils.setMeleeState(itemStack, false);
            this.setAimingTime(itemStack, entity, 0);
            this.clearBurstFiring(itemStack);
            this.setWindingUp(itemStack, entity, false);
            this.setForceCancelAction(itemStack, entity, false);
            if (this.resetChargeOnUnequip)
                this.setCharged(itemStack, entity, false);
            return;
        }
        RFEFirearmModeHandlingProperties properties = this.getHandlingProperties(itemStack);

        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);

        boolean holdingKey = itemStack.getItem() instanceof HoldAttackKeyInteraction holdAttackKeyInteraction
                && holdAttackKeyInteraction.isHoldingAttackKey(itemStack, entity);
        if (action != RFEFirearmItem.Action.FIRING)
            this.clearBurstFiring(itemStack);
        if (action != RFEFirearmItem.Action.RELOAD)
            this.setForceCancelAction(itemStack, entity, false);

        if (this.isMeleeing(itemStack)) {
            if (action == RFEFirearmItem.Action.ENTER_MELEE) {
                this.onTickEnterMelee(itemStack, entity);
            } else {
                FirearmDataUtils.setAction(itemStack, null);
                FirearmDataUtils.setActionTime(itemStack, 0);
            }
            RFEFirearmItem.Action endAction = FirearmDataUtils.getAction(itemStack);
            if (endAction == null)
                this.startIdleEffects(itemStack, entity);
        } else if (action != null) {
            switch (action) {
                case RELOAD -> this.onTickReload(itemStack, entity);
                case UNLOAD -> this.onTickUnload(itemStack, entity);
                case FIRING -> this.onTickFiring(itemStack, entity);
                case CHARGING -> this.onTickCharging(itemStack, entity);
                case DRAW -> this.onTickDraw(itemStack, entity);
                case SWITCH_MODE -> this.onTickSwitchMode(itemStack, entity);
                case COOLDOWN -> this.onTickCooldown(itemStack, entity);
                case ENTER_MELEE -> this.onTickEnterMelee(itemStack, entity);
                case EXIT_MELEE -> this.onTickExitMelee(itemStack, entity);
                default -> {
                    FirearmDataUtils.setAction(itemStack, null);
                    FirearmDataUtils.setActionTime(itemStack, 0);
                }
            }
            RFEFirearmItem.Action endAction = FirearmDataUtils.getAction(itemStack);
            if (endAction == null)
                this.startIdleEffects(itemStack, entity);
        } else {
            if (FirearmDataUtils.isOverheated(this.getModeData(itemStack))) {
                FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.COOLDOWN);
                FirearmDataUtils.setActionTime(itemStack, this.cooldownTime);
            } else if (this.fireMode == FireMode.SINGLE_ACTION
                    && this.automaticSingleActionCycle(itemStack, entity, holdingKey)
                    && this.canChargeInternal(itemStack, entity)) {
                this.onCharge(itemStack, entity);
            }
        }
        if (FirearmDataUtils.getAction(itemStack) != RFEFirearmItem.Action.RELOAD)
            itemStack.set(RFEDataComponents.USING_UNLIMITED_AMMO_RELOAD, false);

        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
        action = FirearmDataUtils.getAction(itemStack);
        if (action != RFEFirearmItem.Action.RELOAD) {
            patched.remove(RFEDataComponents.RELOAD_PHASE);
            patched.remove(RFEDataComponents.RELOAD_PHASE_INDEX);
        }
        if (action != RFEFirearmItem.Action.UNLOAD) {
            patched.remove(RFEDataComponents.UNLOAD_PHASE);
            patched.remove(RFEDataComponents.UNLOAD_PHASE_INDEX);
        }
        this.saveModeData(itemStack, patched.asPatch());
        if (action != RFEFirearmItem.Action.FIRING && this.isWindingUp(itemStack, entity))
            this.setWindingUp(itemStack, entity, false);

        if (this.isAiming(itemStack, entity) && !this.canAim(itemStack, entity)) {
            this.stopAiming(itemStack, entity);
            entity.stopUsingItem();
        }
        int aimingTime = this.getAimingTime(itemStack, entity);
        if (aimingTime > 0) {
            --aimingTime;
            this.setAimingTime(itemStack, entity, aimingTime);
        }

        if (!entity.level().isClientSide) {
            // TODO tick ammo slots (primary, secondary)
            // TODO tick attachment slots

            DataComponentPatch modeData = this.getModeData(itemStack);
            if (this.canOverheat && !FirearmDataUtils.isOverheated(modeData)) {
                int cooldownDelay = FirearmDataUtils.getCoolingDelay(modeData);
                if (cooldownDelay > 0) {
                    --cooldownDelay;
                    modeData = FirearmDataUtils.setCoolingDelay(modeData, cooldownDelay);
                    this.saveModeData(itemStack, modeData);
                } else {
                    float heat = FirearmDataUtils.getHeat(modeData);
                    heat -= properties.heatProperties().heatRemovedPerTick();
                    heat = Math.max(0, heat);
                    modeData = FirearmDataUtils.setHeat(modeData, heat);
                    this.saveModeData(itemStack, modeData);
                }
            }
            if (action != RFEFirearmItem.Action.FIRING) {
                patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, this.getModeData(itemStack));
                patched.remove(RFEDataComponents.EXTRA_FIRING_TIME);
                this.saveModeData(itemStack, patched.asPatch());
            }
        }
    }

    public void startIdleEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public void onReleaseAttackKey(ItemStack itemStack, LivingEntity entity) {
        if (FirearmDataUtils.getAction(itemStack) == RFEFirearmItem.Action.FIRING && this.isWindingUp(itemStack, entity) && this.canInterruptWindUp) {
            FirearmDataUtils.setAction(itemStack, null);
            FirearmDataUtils.setActionTime(itemStack, 0);
            this.setWindingUp(itemStack, entity, false);
        }
    }

    public float getItemLength(ItemStack itemStack, @Nullable LivingEntity entity) {
        return this.itemLength;
    }

    public int countFreeAmmoSpaces(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            int count = RFEItemUtils.countItems(ammoList);
            return this.nominalCapacity - count;
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                int capacity = magazineItem.getMagazineCapacity(magazine);
                List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                int count = RFEItemUtils.countItems(ammoList);
                return capacity - count;
            } else {
                return 0;
            }
        }
    }

    public int countExtraAmmoSpaces(ItemStack itemStack) {
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(this.getModeData(itemStack), RFEDataComponents.INTERNAL_ROUNDS);
            int count = RFEItemUtils.countItems(ammoList);
            int extraSlots = this.internalCapacity - this.nominalCapacity;
            int extraAmmo = Math.max(0, count - this.nominalCapacity);
            return extraSlots - extraAmmo;
        } else {
            return 0; // Magazines have no extra slots
        }
    }

    public boolean hasAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            return RFEItemUtils.countItems(ammoList) > 0;
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return false;
            ItemStack magazine = o.get().copyOne();
            if (magazine.getItem() instanceof MagazineItem magazineItem) {
                List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
                return RFEItemUtils.countItems(ammoList) > 0;
            } else {
                return false;
            }
        }
    }

    public boolean hasSecondaryAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalSecondaryCapacity > 0) {
            List<ItemStack> secondaryList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            return RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer)) > 0;
        } else {
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty() && !FirearmDataUtils.isUsedPrimer(loadedPrimer))
                    return true;
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return false;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return false;
            List<ItemStack> secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
            return RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer)) > 0;
        }
    }

    protected ItemStack getChamberedRound(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        ItemStack chamberedRound;
        if (this.internalCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_ROUNDS);
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        } else if (this.plusOneCapacity) {
            return this.getLoadedRound(itemStack);
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return ItemStack.EMPTY;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return ItemStack.EMPTY;
            List<ItemStack> ammoList = magazineItem.getStoredAmmo(magazine);
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        }
        return chamberedRound;
    }

    public boolean hasChamberedRound(ItemStack itemStack) {
        if (!this.requiresAmmo())
            return true;
        ItemStack chamberedRound = this.getChamberedRound(itemStack);
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        for (AmmoPredicate pred : ammoProperties.primaryAmmo().keySet()) {
            if (pred.test(chamberedRound))
                return true;
        }
        return false;
    }

    public boolean isCharged(ItemStack itemStack) {
        return FirearmDataUtils.isCharged(this.getModeData(itemStack));
    }

    public boolean hasMagazine(ItemStack itemStack) {
        if (this.internalCapacity > 0)
            return false;
        DataComponentPatch modeData = this.getModeData(itemStack);
        Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
        return o != null && o.isPresent() && o.get().copyOne().getItem() instanceof MagazineItem;
    }

    public boolean entityHasMagazine(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0)
            return false;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        Predicate<ItemStack> magazinePred = RFEUtils.orAllPredicates(ammoProperties.magazines());
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        ItemStack magazine = RFEItemUtils.findFullestMagazine(entity, magazinePred, ammoPred, false);
        if (!magazine.isEmpty())
            return true;
        if (!RFEFirearmItem.canEntityInfiniteReload(entity, itemStack))
            return false;
        // Taken to be compatible if passes magazine predicate, checks done in RFEFirearmAmmoHandler
        for (ItemStack s : ammoProperties.unlimitedPrimaryReloadItems()) {
            if (magazinePred.test(s))
                return true;
        }
        return false;
    }

    public int bestSpeedloaderAmmoCount(ItemStack itemStack, LivingEntity entity) {
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        Predicate<ItemStack> ammoPred = RFEUtils.orAllPredicates(ammoProperties.primaryAmmoPredicates());
        Predicate<ItemStack> speedloaderPred = RFEUtils.orAllPredicates(ammoProperties.speedloaders());
        int reloadCount1 = this.countFreeAmmoSpaces(itemStack);
        ItemStack bestSpeedloaderStack = RFEItemUtils.findBestSpeedloader(entity, speedloaderPred, ammoPred, reloadCount1, false);
        if (bestSpeedloaderStack.getItem() instanceof MagazineItem speedloaderItem) {
            int count = speedloaderItem.countAmmo(bestSpeedloaderStack);
            if (count > 0)
                return count;
        }
        if (!RFEFirearmItem.canEntityInfiniteReload(entity, itemStack))
            return 0;
        List<ItemStack> infiniteStacks = ammoProperties.unlimitedPrimaryReloadItems();
        for (ItemStack s : infiniteStacks) {
            if (!(s.getItem() instanceof MagazineItem speedloaderItem))
                continue;
            if (speedloaderPred.test(s))
                return speedloaderItem.countAmmo(s);
        }
        return 0;
    }

    public int countFreeSecondaryAmmoSpaces(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalSecondaryCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            int count = 0;
            for (ItemStack ammo : ammoList) {
                if (!ammo.isEmpty() && !FirearmDataUtils.isUsedPrimer(ammo))
                    count += ammo.getCount();
            }
            return this.nominalSecondaryCapacity - count;
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return 0;
            int capacity = magazineItem.getSecondaryMagazineCapacity(magazine);
            List<ItemStack> secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
            int count = RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer));
            return capacity - count;
        }
    }

    public int countUsedSecondaryAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalSecondaryCapacity > 0) {
            List<ItemStack> secondaryList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            return RFEItemUtils.countItemsConditional(secondaryList, FirearmDataUtils::isUsedPrimer);
        } else {
            int extraCount = 0;
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty() && FirearmDataUtils.isUsedPrimer(loadedPrimer))
                    ++extraCount;
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return 0;
            List<ItemStack> secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
            return RFEItemUtils.countItemsConditional(secondaryList, FirearmDataUtils::isUsedPrimer) + extraCount;
        }
    }

    public int countSecondaryAmmo(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        if (this.internalSecondaryCapacity > 0) {
            List<ItemStack> secondaryList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            return RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer));
        } else {
            int extraCount = 0;
            if (this.plusOneSecondaryCapacity) {
                ItemStack loadedPrimer = this.getLoadedPrimer(itemStack);
                if (!loadedPrimer.isEmpty() && !FirearmDataUtils.isUsedPrimer(loadedPrimer))
                    ++extraCount;
            }
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return 0;
            List<ItemStack> secondaryList = magazineItem.getStoredSecondaryAmmo(magazine);
            return RFEItemUtils.countItemsConditional(secondaryList, Predicate.not(FirearmDataUtils::isUsedPrimer)) + extraCount;
        }
    }

    protected ItemStack getChamberedSecondary(ItemStack itemStack) {
        DataComponentPatch modeData = this.getModeData(itemStack);
        ItemStack chamberedRound;
        if (this.internalSecondaryCapacity > 0) {
            List<ItemStack> ammoList = FirearmDataUtils.getRounds(modeData, RFEDataComponents.INTERNAL_PRIMERS);
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        } else if (this.plusOneSecondaryCapacity) {
            return this.getLoadedPrimer(itemStack);
        } else {
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return ItemStack.EMPTY;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return ItemStack.EMPTY;
            List<ItemStack> ammoList = magazineItem.getStoredSecondaryAmmo(magazine);
            if (ammoList.isEmpty())
                return ItemStack.EMPTY;
            chamberedRound = ammoList.get(this.ammoConsumedLast ? ammoList.size() - 1 : 0);
        }
        return chamberedRound;
    }

    public boolean hasChamberedSecondary(ItemStack itemStack) {
        if (!this.requiresSecondaryAmmo())
            return false;
        ItemStack chamberedSecondary = this.getChamberedSecondary(itemStack);
        if (chamberedSecondary.isEmpty() || FirearmDataUtils.isUsedPrimer(chamberedSecondary))
            return false;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        for (AmmoPredicate pred : ammoProperties.secondaryAmmo()) {
            if (pred.test(chamberedSecondary))
                return true;
        }
        return false;
    }

    public boolean hasChamberedUsedSecondary(ItemStack itemStack) {
        if (!this.requiresSecondaryAmmo())
            return false;
        ItemStack chamberedSecondary = this.getChamberedSecondary(itemStack);
        if (chamberedSecondary.isEmpty() || !FirearmDataUtils.isUsedPrimer(chamberedSecondary))
            return false;
        RFEFirearmModeAmmoProperties ammoProperties = this.getAmmoProperties(itemStack);
        for (AmmoPredicate pred : ammoProperties.secondaryAmmo()) {
            if (pred.test(chamberedSecondary))
                return true;
        }
        return false;
    }

    public int primableAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (!this.requiresSecondaryAmmo())
            return 0;
        int ammoCount = this.getLoadedAmmoCount(itemStack, entity, true);
        int primedCount = this.primedAmmoCount(itemStack, entity);
        return Math.max(0, ammoCount - primedCount);
    }

    public int primedAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (!this.requiresSecondaryAmmo())
            return 0;
        if (!this.requiresAmmo())
            return this.countSecondaryAmmo(itemStack);
        if (!this.trackEmptySlots && !this.trackEmptySecondarySlots) {
            // If empty slots are not tracked for both, just get smaller of loose count
            int ammoCount = this.getLoadedAmmoCount(itemStack, entity, true);
            int secondaryCount = this.countSecondaryAmmo(itemStack);
            return Math.min(ammoCount, secondaryCount);
        }
        List<ItemStack> primaryAmmo = this.getLoadedAmmo(itemStack);
        List<ItemStack> secondaryAmmo = this.getLoadedSecondaryAmmo(itemStack);

        int sz = Math.min(RFEItemUtils.countItemsIncludingSlots(primaryAmmo), RFEItemUtils.countItemsIncludingSlots(secondaryAmmo));
        int paired = 0;
        for (int i = 0; i < sz; ++i) {
            ItemStack primary = FirearmDataUtils.stripAmmo(primaryAmmo, this.ammoConsumedLast, false, false);
            ItemStack secondary = FirearmDataUtils.stripAmmo(secondaryAmmo, this.ammoConsumedLast, false, false);
            if (!primary.isEmpty() && !secondary.isEmpty() && !FirearmDataUtils.isUsedPrimer(secondary))
                ++paired;
        }
        return paired;
    }

    public int getActualAmmoCapacity(ItemStack itemStack, LivingEntity entity) {
        if (this.internalCapacity > 0) {
            return this.internalCapacity;
        } else {
            Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return 0;
            return magazineItem.getMagazineCapacity(magazine);
        }
    }

    public int getActualSecondaryAmmoCapacity(ItemStack itemStack, LivingEntity entity) {
        if (this.internalSecondaryCapacity > 0) {
            return this.internalSecondaryCapacity;
        } else {
            Optional<? extends RFEItemContainerContents> o = this.getModeData(itemStack).get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o == null || o.isEmpty())
                return 0;
            ItemStack magazine = o.get().copyOne();
            if (!(magazine.getItem() instanceof MagazineItem magazineItem))
                return 0;
            return magazineItem.getSecondaryMagazineCapacity(magazine);
        }
    }

    public boolean requiresAmmo() { return this.ammoRequired; }

    public boolean requiresSecondaryAmmo() { return this.requiresSecondaryAmmo; }

    public float getDrawFraction(ItemStack itemStack, LivingEntity entity) {
        return this.drawTime <= 0 ? 1 : Mth.clamp(1f - (float) FirearmDataUtils.getActionTime(itemStack) / (float) this.drawTime, 0f, 1f);
    }

    public float getHeatAmount(ItemStack itemStack) {
        return FirearmDataUtils.getHeat(this.getModeData(itemStack));
    }

    public float getHeatCapacity(ItemStack itemStack) {
        return this.getHandlingProperties(itemStack).heatProperties().heatCapacity();
    }

    public void addModeAttachments(ItemStack itemStack, Map<ResourceLocation, ItemStack> attachments) {
        if (this.loadedRoundAttachmentSlot != null && !attachments.containsKey(this.loadedRoundAttachmentSlot)) {
            List<ItemStack> nextRound = this.getNextRoundsInItem(itemStack, null, 1, false);
            if (!nextRound.isEmpty())
                attachments.put(this.loadedRoundAttachmentSlot, nextRound.get(0).copy());
        }
        if (this.magazineAttachmentSlot != null && !attachments.containsKey(this.magazineAttachmentSlot)) {
            DataComponentPatch modeData = this.getModeData(itemStack);
            Optional<? extends RFEItemContainerContents> o = modeData.get(RFEDataComponents.DETACHED_MAGAZINE);
            if (o != null && o.isPresent())
                attachments.put(this.magazineAttachmentSlot, o.get().copyOne());
        }
        if (this.loadedSecondaryAttachmentSlot != null && !attachments.containsKey(this.loadedSecondaryAttachmentSlot)) {
            List<ItemStack> nextSecondaries = this.getNextSecondariesInItem(itemStack, null, 1, false, true);
            if (!nextSecondaries.isEmpty())
                attachments.put(this.loadedSecondaryAttachmentSlot, nextSecondaries.get(0).copy());
        }
    }

    public void addModeAttachmentSlots(ItemStack itemStack, Set<ResourceLocation> slots) {
        if (this.loadedRoundAttachmentSlot != null)
            slots.add(this.loadedRoundAttachmentSlot);
        if (this.magazineAttachmentSlot != null)
            slots.add(this.magazineAttachmentSlot);
        if (this.loadedSecondaryAttachmentSlot != null)
            slots.add(this.loadedSecondaryAttachmentSlot);
    }

    public int getShotCount(ItemStack itemStack) {
        return FirearmDataUtils.getShotCount(this.getModeData(itemStack));
    }

    public float getPitchAdjustment(ItemStack itemStack) {
        return this.pitchAdjustment; // TODO adjustable sights
    }

    public void handleMeleeInput(ItemStack itemStack, Player player, InteractionHand hand, boolean meleeInput) {
        if (!this.canMelee || this.forcedMelee)
            return;
        RFEFirearmItem.Action action = FirearmDataUtils.getAction(itemStack);
        if (meleeInput && action == null) { // Quick melee
            if (FirearmDataUtils.isInMeleeState(itemStack)) {
                FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.EXIT_MELEE);
                FirearmDataUtils.setActionTime(itemStack, this.exitMeleeTime);
                FirearmDataUtils.setMeleeState(itemStack, false);
            } else {
                FirearmDataUtils.setAction(itemStack, RFEFirearmItem.Action.ENTER_MELEE);
                FirearmDataUtils.setActionTime(itemStack, this.enterMeleeWindowTime);
                FirearmDataUtils.setMeleeState(itemStack, true);
            }
        } else if (!meleeInput && action == RFEFirearmItem.Action.ENTER_MELEE) { // Cancel enter melee toggle
            FirearmDataUtils.setMeleeState(itemStack, false);
        }
    }

    public boolean isMeleeing(ItemStack itemStack) {
        return this.forcedMelee || FirearmDataUtils.isInMeleeState(itemStack);
    }

    public boolean canMelee(ItemStack itemStack) { return this.canMelee; }

    public void onTickEnterMelee(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.cooldownTime)
            this.playEnterMeleeEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playEnterMeleeEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public void onTickExitMelee(ItemStack itemStack, LivingEntity entity) {
        int actionTime = FirearmDataUtils.getActionTime(itemStack);
        if (actionTime == this.cooldownTime)
            this.playExitMeleeEffects(itemStack, entity);
        if (actionTime > 0)
            --actionTime;
        FirearmDataUtils.setActionTime(itemStack, actionTime);
        if (actionTime > 0)
            return;
        FirearmDataUtils.setAction(itemStack, null);
    }

    public void playExitMeleeEffects(ItemStack itemStack, LivingEntity entity) {
    }

    public float getExitMeleeFraction(ItemStack itemStack, LivingEntity entity) {
        return this.exitMeleeTime <= 0 ? 1 : Mth.clamp(1f - (float) FirearmDataUtils.getActionTime(itemStack) / (float) this.exitMeleeTime, 0f, 1f);
    }

    public enum FiringType {
        CLICK,
        AUTOMATIC,
        EFFECTS
    }
    
}
