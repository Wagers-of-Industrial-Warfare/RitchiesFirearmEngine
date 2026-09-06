package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles.BlackPowderSmokeOptions;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.condition.FirearmConditionMacroHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmMode;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.reload_phase.ReloadPhase;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.misfires.RainMisfire;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.misfires.RandomMisfire;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.misfires.SubmergedMisfire;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil.NoRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.simple.SimpleRecoilProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread.NoSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.random.SimpleSpreadProvider;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.armor_piercing.ArmorPiercingHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.body_parts.HeadshotHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.body_parts.HeadshotHitMultiplierGore;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.bullet_health.BulletHealthHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.fixed.FixedHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.vulnerable_to_birdshot.BirdshotHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.hit_multipliers.vulnerable_to_birdshot.BirdshotHitMultiplierGore;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.GenericAttachmentItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.bayonets.BayonetAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.bayonets.BayonetItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.recoil_control.BipodAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.recoil_control.GripAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes.ScopeAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes.ScopeItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.supperssors.SuppressorAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.supperssors.SuppressorItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEIntegralAttachmentData;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemAttachmentContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.buck_and_ball.RFEBuckAndBallProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive.RFEExplosiveProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.rocket.RFEExplosiveRocketProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.shotgun.RFEShotgunProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.RFETags;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("scope"), new ScopeItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("bayonet"), new BayonetItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("suppressor"), new SuppressorItem.Builder());
        RFEContentBuilderRegistry.registerItemBuilder(RitchiesFirearmEngine.resource("generic_attachment"), new GenericAttachmentItem.Builder());

        ProjectileTypes.register();
        SpreadProviders.register();
        RecoilProviders.register();
        HitMultipliers.register();
        Misfires.register();
        AttachmentSlots.register();

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
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("firearm_ammo_count"), BuiltInRFEPlugin::firearmAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("entity_secondary_ammo_count"), BuiltInRFEPlugin::entitySecondaryAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("free_secondary_ammo_space"), BuiltInRFEPlugin::freeSecondaryAmmoSpace);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("used_secondary_ammo_count"), BuiltInRFEPlugin::usedSecondaryAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("secondary_ammo_count"), BuiltInRFEPlugin::secondaryAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("has_chambered_secondary"), BuiltInRFEPlugin::hasChamberedSecondary);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("has_chambered_used_secondary"), BuiltInRFEPlugin::hasChamberedUsedSecondary);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("primable_ammo_count"), BuiltInRFEPlugin::primableAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("primed_ammo_count"), BuiltInRFEPlugin::primedAmmoCount);
        RFEContentBuilderRegistry.registerCompareValueSource(RitchiesFirearmEngine.resource("speedloaders_blocked"), BuiltInRFEPlugin::speedloadersBlocked);
    }

    @Override
    public void afterPackLoading() {
        FirearmConditionMacroHandler.loadMacros();
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
        registry.accept(RitchiesFirearmEngine.resource("hit_multipliers"), RFEHitMultiplierHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("projectile_penetration"), RFEProjectilePenetrationHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("item_attachment_properties"), RFEItemAttachmentsPropertiesHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("item_attachment_menu_slots"), RFEItemAttachmentsMenuSlotsHandler.ReloadListener.INSTANCE);
    }

    @Override
    public void registerPluginParticleTypes(BiConsumer<ResourceLocation, ParticleType<?>> registry) {
        ParticleTypes.register(registry);
    }

    @Override
    public void registerPluginDataComponentTypes(BiConsumer<ResourceLocation, DataComponentType<?>> registry) {
        RFEDataComponents.register(registry);
    }

    @Override
    public void onCommonSetup() {
        RFEItemUtils.registerGeneralItemHandler(new RFEItemUtils.EntityItemHandler() {
            @Override
            public boolean consumeFromInventory(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess) {
                ItemStack offhandStack = entity.getOffhandItem();
                if (predicate.test(offhandStack)) {
                    ItemStack result = op.apply(offhandStack);
                    entity.setItemInHand(InteractionHand.OFF_HAND, result);
                    return breakOnSuccess.get();
                }
                return false;
            }

            @Override
            public boolean addToInventory(LivingEntity entity, ItemStack itemStack) {
                return false;
            }
        });

        RFEItemUtils.registerItemHandlerForType(EntityType.PLAYER, new RFEItemUtils.EntityItemHandler() {
            @Override
            public boolean consumeFromInventory(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess) {
                return entity instanceof Player player && iterateInventory(player.getInventory().items, predicate, op, breakOnSuccess);
            }

            @Override
            public boolean addToInventory(LivingEntity entity, ItemStack itemStack) {
                if (entity instanceof Player player) {
                    player.getInventory().placeItemBackInInventory(itemStack);
                    return true;
                }
                return false;
            }
        });

        RFEItemUtils.registerItemHandlerForType(EntityType.PILLAGER, new RFEItemUtils.EntityItemHandler() {
            @Override
            public boolean consumeFromInventory(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess) {
                return entity instanceof Pillager pillager && iterateInventory(pillager.getInventory().getItems(), predicate, op, breakOnSuccess);
            }

            @Override
            public boolean addToInventory(LivingEntity entity, ItemStack itemStack) {
                if (itemStack.is(RFETags.RFEItemTags.DISPOSABLE_BY_NPCS_ON_RELOAD.tag)
                        || FirearmDataUtils.isUsedPrimer(itemStack)) {
                    itemStack.setCount(0);
                    return false;
                }
                if (entity instanceof Pillager pillager) {
                    ItemStack result = pillager.getInventory().addItem(itemStack);
                    if (result.isEmpty()) {
                        return true;
                    } else {
                        itemStack.setCount(result.getCount());
                        return false;
                    }
                }
                return false;
            }
        });

        RFEItemUtils.registerItemHandlerForType(EntityType.PIGLIN, new RFEItemUtils.EntityItemHandler() {
            @Override
            public boolean consumeFromInventory(LivingEntity entity, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess) {
                return entity instanceof Pillager pillager && iterateInventory(pillager.getInventory().getItems(), predicate, op, breakOnSuccess);
            }

            @Override
            public boolean addToInventory(LivingEntity entity, ItemStack itemStack) {
                if (itemStack.is(RFETags.RFEItemTags.DISPOSABLE_BY_NPCS_ON_RELOAD.tag)
                        || FirearmDataUtils.isUsedPrimer(itemStack)) {
                    itemStack.setCount(0);
                    return false;
                }
                if (entity instanceof Piglin piglin) {
                    ItemStack result = piglin.getInventory().addItem(itemStack);
                    if (result.isEmpty()) {
                        return true;
                    } else {
                        itemStack.setCount(result.getCount());
                        return false;
                    }
                }
                return false;
            }
        });
    }

    private static boolean iterateInventory(List<ItemStack> inventory, Predicate<ItemStack> predicate, UnaryOperator<ItemStack> op, Supplier<Boolean> breakOnSuccess) {
        for (ListIterator<ItemStack> lister = inventory.listIterator(); lister.hasNext(); ) {
            ItemStack invStack = lister.next();
            if (!predicate.test(invStack))
                continue;
            ItemStack result = op.apply(invStack);
            lister.set(result);
            if (breakOnSuccess.get())
                return true;
        }
        return false;
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

    /**
     * How much ammo the firearm has, as would be displayed on an ammo counter.
     */
    private static float firearmAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.ammoCount(itemStack, entity);
        return 0;
    }

    /**
     * Unused secondary ammo count
     */
    private static float entitySecondaryAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.countEntitySecondaryAmmo(itemStack, entity);
        return 0;
    }

    /**
     * Free secondary space based on amount on entity
     */
    private static float freeSecondaryAmmoSpace(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.freeSecondaryAmmoSpace(itemStack);
        return 0;
    }

    /**
     * Used secondary ammo count
     */
    private static float usedSecondaryAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.usedSecondaryAmmoCount(itemStack);
        return 0;
    }

    /**
     * Secondary ammo count, not counting used ammo
     */
    private static float secondaryAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.secondaryAmmoCount(itemStack);
        return 0;
    }

    /**
     * Whether the firearm has a loaded and unused secondary round
     */
    private static float hasChamberedSecondary(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.hasChamberedSecondary(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * Whether the firearm has a loaded and used secondary round
     */
    private static float hasChamberedUsedSecondary(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.hasChamberedUsedSecondary(itemStack) ? 1 : 0;
        return 0;
    }

    /**
     * How many ammo items are missing a complementing primer
     */
    private static float primableAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.primableAmmoCount(itemStack, entity);
        return 0;
    }

    /**
     * How many ammo items have a complementing primer
     */
    private static float primedAmmoCount(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.primedAmmoCount(itemStack, entity);
        return 0;
    }

    private static float speedloadersBlocked(ItemStack itemStack, LivingEntity entity) {
        if (itemStack.getItem() instanceof RFEFirearmItem firearm)
            return firearm.speedloadersBlocked(itemStack, entity) ? 1 : 0;
        return 0;
    }

    public static class ProjectileTypes {
        private static final Map<ResourceLocation, RFEProjectileType.Serializer<?>> SERIALIZERS = new LinkedHashMap<>();
        public static final RFEBulletProjectileType.Serializer BULLET = register("bullet", new RFEBulletProjectileType.Serializer());
        public static final RFEShotgunProjectileType.Serializer SHOTGUN = register("shotgun", new RFEShotgunProjectileType.Serializer());
        public static final RFEBuckAndBallProjectileType.Serializer BUCK_AND_BALL = register("buck_and_ball", new RFEBuckAndBallProjectileType.Serializer());
        public static final RFEExplosiveProjectileType.Serializer EXPLOSIVE = register("explosive", new RFEExplosiveProjectileType.Serializer());
        public static final RFEExplosiveRocketProjectileType.Serializer EXPLOSIVE_ROCKET = register("explosive_rocket", new RFEExplosiveRocketProjectileType.Serializer());

        private static <T extends RFEProjectileType.Serializer<?>> T register(String id, T ser) {
            ResourceLocation loc = RitchiesFirearmEngine.resource(id);
            if (SERIALIZERS.containsKey(loc))
                throw new IllegalStateException("Already registered data component type " + loc);
            SERIALIZERS.put(loc, ser);
            return ser;
        }

        public static void register() {
            SERIALIZERS.forEach(RFEContentBuilderRegistry::registerProjectileTypeSerializer);
        }

        private ProjectileTypes() {}
    }

    public static class SpreadProviders {
        public static final RFESpreadProvider.Serializer<NoSpreadProvider> NO_SPREAD = new NoSpreadProvider.Serializer();
        public static final RFESpreadProvider.Serializer<SimpleSpreadProvider> SIMPLE = new SimpleSpreadProvider.Serializer();

        public static void register() {
            RFEContentBuilderRegistry.registerSpreadProviderSerializer(RitchiesFirearmEngine.resource("no_spread"), NO_SPREAD);
            RFEContentBuilderRegistry.registerSpreadProviderSerializer(RitchiesFirearmEngine.resource("simple"), SIMPLE);
        }

        private SpreadProviders() {}
    }

    public static class RecoilProviders {
        public static final RFERecoilProvider.Serializer<NoRecoilProvider> NO_RECOIL = new NoRecoilProvider.Serializer();
        public static final RFERecoilProvider.Serializer<SimpleRecoilProvider> SIMPLE = new SimpleRecoilProvider.Serializer();

        public static void register() {
            RFEContentBuilderRegistry.registerRecoilProviderSerializer(RitchiesFirearmEngine.resource("no_recoil"), NO_RECOIL);
            RFEContentBuilderRegistry.registerRecoilProviderSerializer(RitchiesFirearmEngine.resource("simple"), SIMPLE);
        }

        private RecoilProviders() {}
    }

    public static class HitMultipliers {
        public static final RFEHitMultiplier.Provider FIXED = FixedHitMultiplier::new;
        public static final RFEHitMultiplier.Provider ARMOR_PIERCING = ArmorPiercingHitMultiplier::new;
        public static final RFEHitMultiplier.Provider HEADSHOT = HeadshotHitMultiplier::new;
        public static final RFEHitMultiplier.Provider HEADSHOT_GORE = HeadshotHitMultiplierGore::new;
        public static final RFEHitMultiplier.Provider VULNERABLE_TO_BIRDSHOT = BirdshotHitMultiplier::new;
        public static final RFEHitMultiplier.Provider VULNERABLE_TO_BIRDSHOT_GORE = BirdshotHitMultiplierGore::new;
        public static final RFEHitMultiplier.Provider BULLET_HEALTH = BulletHealthHitMultiplier::new;

        public static void register() {
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("fixed"), FIXED);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("armor_piercing"), ARMOR_PIERCING);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("headshot"), HEADSHOT);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("headshot_gore"), HEADSHOT_GORE);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("vulnerable_to_birdshot"), VULNERABLE_TO_BIRDSHOT);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("vulnerable_to_birdshot_gore"), VULNERABLE_TO_BIRDSHOT_GORE);
            RFEContentBuilderRegistry.registerHitMultiplierProvider(RitchiesFirearmEngine.resource("bullet_health"), BULLET_HEALTH);
        }

        private HitMultipliers() {}
    }

    public static class Misfires {
        public static final RFEMisfire.Provider RANDOM = register("random", RandomMisfire::of);
        public static final RFEMisfire.Provider WHEN_RAINING = register("when_raining", RainMisfire::of);
        public static final RFEMisfire.Provider WHEN_SUBMERGED = register("when_submerged", SubmergedMisfire::new);

        public static void register() {
        }

        private static RFEMisfire.Provider register(String id, RFEMisfire.Provider prov) {
            RFEContentBuilderRegistry.registerMisfireProvider(RitchiesFirearmEngine.resource(id), prov);
            return prov;
        }

        private Misfires() {}
    }

    public static class ParticleTypes {
        public static final ParticleType<BlackPowderSmokeOptions> BLACK_POWDER_SMOKE = new ParticleType<>(true) {
            @Override public MapCodec<BlackPowderSmokeOptions> codec() { return BlackPowderSmokeOptions.CODEC; }
            @Override public StreamCodec<? super RegistryFriendlyByteBuf, BlackPowderSmokeOptions> streamCodec() { return BlackPowderSmokeOptions.STREAM_CODEC; }
        };

        public static void register(BiConsumer<ResourceLocation, ParticleType<?>> registry) {
            registry.accept(RitchiesFirearmEngine.resource("black_powder_smoke"), BLACK_POWDER_SMOKE);
        }

        private ParticleTypes() {}
    }

    public static class
    RFEDataComponents {
        private static final Map<ResourceLocation, DataComponentType<?>> TYPES = new LinkedHashMap<>();

        public static final DataComponentType<RFEItemContainerContents> ROUNDS = register("rounds",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<RFEItemContainerContents> PRIMERS = register("primers",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<String> FIREARM_MODE = register("firearm_mode",
                builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

        public static final DataComponentType<ImmutableMap<String, DataComponentPatch>> FIREARM_MODE_DATA = register("firearm_mode_data",
                builder -> builder.persistent(RFEFirearmMode.MODE_DATA_CODEC).networkSynchronized(RFEFirearmMode.MODE_DATA_STREAM_CODEC));

        public static final DataComponentType<Boolean> IS_CHARGED = register("is_charged",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> IS_JAMMED = register("is_jammed",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Integer> ACTION_TIME = register("action_time",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<RFEFirearmItem.Action> FIREARM_ACTION = register("firearm_action",
                builder -> builder.persistent(RFEFirearmItem.Action.CODEC).networkSynchronized(RFEFirearmItem.Action.STREAM_CODEC));

        public static final DataComponentType<Float> FIREARM_HEAT = register("firearm_heat",
                builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

        public static final DataComponentType<Integer> COOLING_DELAY = register("cooling_delay",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> OVERHEATED = register("overheated",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> HOLDING_ATTACK_KEY = register("holding_attack_key",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> AIMING = register("aiming",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Integer> AIMING_TIME = register("aiming_time",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> WINDING_UP = register("winding_up",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Float> EXTRA_FIRING_TIME = register("extra_firing_time",
                builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

        public static final DataComponentType<Integer> BURST_FIRE_COUNT = register("burst_fire_count",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<RFEItemContainerContents> DETACHED_MAGAZINE = register("detached_magazine",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<RFEItemContainerContents> INTERNAL_ROUNDS = register("internal_rounds",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<RFEItemContainerContents> LOADED_ROUND = register("loaded_round",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<RFEItemContainerContents> INTERNAL_PRIMERS = register("internal_primers",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<RFEItemContainerContents> LOADED_PRIMER = register("loaded_primer",
                builder -> builder.persistent(RFEItemContainerContents.CODEC).networkSynchronized(RFEItemContainerContents.STREAM_CODEC));

        public static final DataComponentType<ReloadPhase.PhaseType> RELOAD_PHASE = register("reload_phase",
                builder -> builder.persistent(ReloadPhase.PhaseType.CODEC).networkSynchronized(ReloadPhase.PhaseType.STREAM_CODEC));

        public static final DataComponentType<ReloadPhase.PhaseType> UNLOAD_PHASE = register("unload_phase",
                builder -> builder.persistent(ReloadPhase.PhaseType.CODEC).networkSynchronized(ReloadPhase.PhaseType.STREAM_CODEC));

        public static final DataComponentType<Integer> RELOAD_PHASE_INDEX = register("reload_phase_index",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Integer> UNLOAD_PHASE_INDEX = register("unload_phase_index",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> FORCE_CANCEL_ACTION = register("force_cancel_action",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Integer> CHARGE_ACTION = register("charge_action",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<RFEItemAttachmentContents> ITEM_ATTACHMENTS = register("item_attachments",
                builder -> builder.persistent(RFEItemAttachmentContents.CODEC).networkSynchronized(RFEItemAttachmentContents.STREAM_CODEC));

        public static final DataComponentType<RFEIntegralAttachmentData> INTEGRAL_ATTACHMENTS = register("integral_attachments",
                builder -> builder.persistent(RFEIntegralAttachmentData.CODEC).networkSynchronized(RFEIntegralAttachmentData.STREAM_CODEC));

        public static final DataComponentType<Integer> SHOT_COUNT = register("shot_count",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> IS_EQUIPPED = register("is_equipped",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> IS_USED_PRIMER = register("is_used_primer",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Long> LAST_SHOT_TIME = register("last_shot_time",
                builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

        public static final DataComponentType<Integer> FILLED_MAX_STACK_SIZE = register("filled_max_stack_size",
                builder -> builder.persistent(ExtraCodecs.intRange(1, 99)).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> MELEEING = register("meleeing",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> USING_UNLIMITED_AMMO_RELOAD = register("using_unlimited_ammo_reload",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Boolean> REMOVED_ATTACHMENT = register("removed_attachment",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        public static final DataComponentType<Integer> ZOOM_LEVEL_INDEX = register("zoom_level_index",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Boolean> DEPLOYED_SETTING = register("deployed_setting",
                builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

        private static <V> DataComponentType<V> register(String id, UnaryOperator<DataComponentType.Builder<V>> builderOp) {
            ResourceLocation loc = RitchiesFirearmEngine.resource(id);
            if (TYPES.containsKey(loc))
                throw new IllegalStateException("Already registered data component type " + loc);
            DataComponentType<V> type = builderOp.apply(DataComponentType.builder()).build();
            TYPES.put(loc, type);
            return type;
        }

        public static void register(BiConsumer<ResourceLocation, DataComponentType<?>> registry) { TYPES.forEach(registry); }

        private RFEDataComponents() {}
    }

    public static class AttachmentSlots {
        public static final RFEItemAttachmentProperties.Serializer<ScopeAttachmentProperties> SCOPE = register("scope", new ScopeAttachmentProperties.Serializer());
        public static final RFEItemAttachmentProperties.Serializer<BayonetAttachmentProperties> BAYONET = register("bayonet", new BayonetAttachmentProperties.Serializer());
        public static final RFEItemAttachmentProperties.Serializer<SuppressorAttachmentProperties> SUPPRESSOR = register("suppressor", new SuppressorAttachmentProperties.Serializer());
        public static final RFEItemAttachmentProperties.Serializer<GripAttachmentProperties> GRIP = register("grip", new GripAttachmentProperties.Serializer());
        public static final RFEItemAttachmentProperties.Serializer<BipodAttachmentProperties> BIPOD = register("bipod", new BipodAttachmentProperties.Serializer());

        public static void register() {}

        private static <T extends RFEItemAttachmentProperties> RFEItemAttachmentProperties.Serializer<T> register(String id, RFEItemAttachmentProperties.Serializer<T> ser) {
            RFEContentBuilderRegistry.registerItemAttachmentSerializer(RitchiesFirearmEngine.resource(id), ser);
            return ser;
        }

        private AttachmentSlots() {}
    }

}
