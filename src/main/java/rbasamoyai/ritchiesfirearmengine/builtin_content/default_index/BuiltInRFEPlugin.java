package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.*;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles.BlackPowderSmokeOptions;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEDefaultFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmAmmoHandler;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config.RFEFirearmHandlingPropertiesHandler;
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
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemAttachmentContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_handling.RFEItemContainerContents;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.buck_and_ball.RFEBuckAndBallProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.explosive.RFEExplosiveProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.rocket.RFEExplosiveRocketProjectileType;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.shotgun.RFEShotgunProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplier;
import rbasamoyai.ritchiesfirearmengine.foundation.api.hit_multiplier.RFEHitMultiplierHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.misfires.RFEMisfire;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProviderPackHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProviderPackHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
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
        // TODO revolver builder

        ProjectileTypes.register();
        SpreadProviders.register();
        RecoilProviders.register();
        HitMultipliers.register();
        Misfires.register();

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
    }

    @Override
    public void registerPluginParticleTypes(BiConsumer<ResourceLocation, ParticleType<?>> registry) {
        ParticleTypes.register(registry);
    }

    @Override
    public void registerPluginDataComponentTypes(BiConsumer<ResourceLocation, DataComponentType<?>> registry) {
        RFEDataComponents.register(registry);
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

    public static class RFEDataComponents {
        private static final Map<ResourceLocation, DataComponentType<?>> TYPES = new LinkedHashMap<>();

        public static final DataComponentType<RFEItemContainerContents> ROUNDS = register("rounds",
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

        public static final DataComponentType<Integer> SHOT_COUNT = register("shot_count",
                builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        // TODO move to base RFE index
        public static final DataComponentType<UUID> RECOIL_IDENTIFIER = register("recoil_identifier",
                builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

        // TODO move to base RFE index
        public static final DataComponentType<UUID> SPREAD_IDENTIFIER = register("spread_identifier",
                builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

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

}
