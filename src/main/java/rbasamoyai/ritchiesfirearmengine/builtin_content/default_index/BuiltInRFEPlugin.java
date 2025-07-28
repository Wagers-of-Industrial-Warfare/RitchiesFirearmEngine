package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmCondtionMacroHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil.NoRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.simple.SimpleRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread.NoSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.random.SimpleSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudItemInfoProviders;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;

import java.util.function.BiConsumer;

/**
 * Because it's always best to lead by example.
 * Also required for most content anyway.
 */
public class BuiltInRFEPlugin implements RFEPlugin {

    @Override
    public void register() {
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("basic"), new RFEBasicItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo"), new RFEBasicItem.Builder()); // Alias of basic
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("magazine"), new MagazineItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("speedloader"), new MagazineItem.Builder()); // Alias of magazine
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("ammo_packet"), new AmmoPacketItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("firearm"), new RFEDefaultFirearmItem.Builder());
        // TODO revolver builder

        ProjectileTypes.register();
        SpreadProviders.register();
        RecoilProviders.register();

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

        RFEHudItemInfoProviders.registerAmmoProvider(RFEFirearmItem::getAmmoItemsForHUD);
        RFEHudItemInfoProviders.registerAmmoInventoryCountProvider(RFEFirearmItem::getInventoryAmmoCountForHUD);
    }

    @Override
    public void afterPackLoading() {
        FirearmCondtionMacroHandler.loadMacros();
    }

    @Override
    public void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        registry.accept(RitchiesFirearmEngine.resource("magazine_item_properties"), MagazineItemPropertiesHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("ammo_packet_item_properties"), AmmoPacketItemPropertiesHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("rfe_projectile_types"), RFEProjectileTypeHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("firearm_ammo"), RFEFirearmAmmoHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("firearm_handling"), RFEFirearmHandlingPropertiesHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("firearm_spread"), RFESpreadProviderPackHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("firearm_recoil"), RFERecoilProviderPackHandler.ReloadListener.INSTANCE);
    }

    /**
     * Primary ammo count
     */
    private static float entityAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.countEntityAmmo(itemStack, entity);
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

    public static class ProjectileTypes {
        public static final RFEBulletProjectileType.Serializer BULLET = new RFEBulletProjectileType.Serializer();

        public static void register() {
            RFEContentBuilderRegistry.registerProjectileTypeSerializer(RitchiesFirearmEngine.resource("bullet"), BULLET);
        }
    }

    public static class SpreadProviders {
        public static final RFESpreadProvider.Serializer<NoSpreadProvider> NO_SPREAD = new NoSpreadProvider.Serializer();
        public static final RFESpreadProvider.Serializer<SimpleSpreadProvider> SIMPLE = new SimpleSpreadProvider.Serializer();

        public static void register() {
            RFEContentBuilderRegistry.registerSpreadProviderSerializer(RitchiesFirearmEngine.resource("no_spread"), NO_SPREAD);
            RFEContentBuilderRegistry.registerSpreadProviderSerializer(RitchiesFirearmEngine.resource("simple"), SIMPLE);
        }
    }

    public static class RecoilProviders {
        public static final RFERecoilProvider.Serializer<NoRecoilProvider> NO_RECOIL = new NoRecoilProvider.Serializer();
        public static final RFERecoilProvider.Serializer<SimpleRecoilProvider> SIMPLE = new SimpleRecoilProvider.Serializer();

        public static void register() {
            RFEContentBuilderRegistry.registerRecoilProviderSerializer(RitchiesFirearmEngine.resource("no_recoil"), NO_RECOIL);
            RFEContentBuilderRegistry.registerRecoilProviderSerializer(RitchiesFirearmEngine.resource("simple"), SIMPLE);
        }
    }

}
