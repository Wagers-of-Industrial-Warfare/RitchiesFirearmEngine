package rbasamoyai.ritchiesfirearmengine.default_index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.AmmoPacketItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.content.ammo.MagazineItemPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.plugins.RFEPlugin;

import java.util.function.BiConsumer;

/**
 * Because it's always best to lead by example.
 * Also required for most content anyway.
 */
public class BuiltInRFEPlugin implements RFEPlugin {

    @Override
    public void register() {
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo"), new AmmoItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("magazine"), new MagazineItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("speedloader"), new MagazineItem.Builder()); // Alias of magazine
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo_packet"), new AmmoPacketItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("firearm"), new RFEDefaultFirearmItem.Builder());
        // TODO revolver builder

        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("entity_ammo_count"), BuiltInRFEPlugin::entityAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("free_ammo_space"), BuiltInRFEPlugin::freeAmmoSpace);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("extra_ammo_space"), BuiltInRFEPlugin::extraAmmoSpace);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("firearm_has_ammo"), BuiltInRFEPlugin::firearmHasAmmo);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("has_chambered_round"), BuiltInRFEPlugin::hasChamberedRound);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("is_charged"), BuiltInRFEPlugin::isCharged);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("is_jammed"), BuiltInRFEPlugin::isJammed);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("firearm_has_magazine"), BuiltInRFEPlugin::firearmHasMagazine);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("entity_has_magazine"), BuiltInRFEPlugin::entityHasMagazine);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("best_speedloader_ammo_count"), BuiltInRFEPlugin::bestSpeedloaderAmmoCount);
    }

    @Override
    public void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        registry.accept(RitchiesFirearmEngine.resource("magazine_item"), MagazineItemPropertiesHandler.ReloadListener.INSTANCE);
    }

    /**
     * Primary ammo count
     */
    private static float entityAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.countAmmo(itemStack, entity);
        return 0;
    }

    /**
     * Free space based on
     * 1. if magazine, the space available in the magazine, or
     * 2. if internal capacity, space available with respect to nominal capacity
     */
    private static float freeAmmoSpace(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.freeAmmoSpace(itemStack);
        return 0;
    }

    /**
     * Extra ammo space based on:
     * 1. if magazine, 0
     * 2. if internal capacity, space available with respect to actual internal capacity, after nominal capacity
     */
    private static float extraAmmoSpace(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.extraAmmoSpace(itemStack);
        return 0;
    }

    /**
     * Whether the firearm contains primary ammo
     */
    private static float firearmHasAmmo(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.hasAmmo(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the firearm has a loaded primary round and is charged
     */
    private static float hasChamberedRound(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.hasChamberedRound(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the firearm is charged; does not require round
     */
    private static float isCharged(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.isCharged(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the firearm is jammed
     */
    private static float isJammed(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.isJammed(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the firearm has a magazine
     */
    private static float firearmHasMagazine(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.hasMagazine(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the entity has a magazine
     */
    private static float entityHasMagazine(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.entityHasMagazine(itemStack, entity) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the player has a speedloader. A speedloader that exactly matches the ammo count will be chosen;
     * otherwise, the speedloader with the greatest amount of ammo is chosen.
     */
    private static float bestSpeedloaderAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.bestSpeedloaderAmmoCount(itemStack, entity);
        return 0;
    }

}
