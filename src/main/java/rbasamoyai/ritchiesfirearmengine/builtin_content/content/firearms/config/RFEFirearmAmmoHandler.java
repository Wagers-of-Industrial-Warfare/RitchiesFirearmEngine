package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.ammo.MagazineItem;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.AmmoPredicate;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.mode.RFEFirearmModeAmmoProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileTypeHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.data_packing.RFEJsonResourceReloadListener;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.RFEPacket;
import rbasamoyai.ritchiesfirearmengine.utils.RFEByteBufCodecUtils;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RFEFirearmAmmoHandler {

    private static final Map<Item, RFEFirearmProperties<RFEFirearmModeAmmoProperties>> FIREARM_AMMO_PROPERTIES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Item, UnresolvedItemAmmoProperties> UNRESOLVED_PROPERTIES = new Reference2ObjectOpenHashMap<>();

    private static final RFEFirearmModeAmmoProperties EMPTY_MODE = new RFEFirearmModeAmmoProperties(ImmutableMap.of(),
            ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), null, ImmutableList.of(), ImmutableList.of());
    private static final RFEFirearmProperties<RFEFirearmModeAmmoProperties> EMPTY = new RFEFirearmProperties<>(EMPTY_MODE, ImmutableMap.of());

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Codec<UnresolvedItemAmmoProperties> CODEC = RecordCodecBuilder.create(o -> o.group(
            UnresolvedModeAmmoProperties.CODEC.forGetter(p -> p.defaultModeProperties),
            ExtraCodecs.strictUnboundedMap(Codec.STRING, UnresolvedModeAmmoProperties.CODEC.codec()).optionalFieldOf("modes", Map.of()).forGetter(p -> p.modeProperties)
    ).apply(o, UnresolvedItemAmmoProperties::fromCodec));

    public static class ReloadListener extends RFEJsonResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();

        private ReloadListener() { super(GSON, RitchiesFirearmEngine.MOD_ID + "/firearm_ammo"); }

        @Override
        protected void apply(Multimap<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
            clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : data.entries()) {
                ResourceLocation id = entry.getKey();
                try {
                    Item item = BuiltInRegistries.ITEM.getOptional(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " does not exist"));
                    loadIncomplete(entry.getValue(), item);
                } catch (Exception e) {
                    LOGGER.error("Error loading firearm ammo properties for {}: {}", id, e);
                }
            }
        }
    }

    private static void clear() {
        FIREARM_AMMO_PROPERTIES.clear();
        UNRESOLVED_PROPERTIES.clear();
    }

    public static void loadProjectileTypes() {
        FIREARM_AMMO_PROPERTIES.clear();
        for (Map.Entry<Item, UnresolvedItemAmmoProperties> entry : UNRESOLVED_PROPERTIES.entrySet())
            FIREARM_AMMO_PROPERTIES.put(entry.getKey(), entry.getValue().resolve(entry.getKey()));
        UNRESOLVED_PROPERTIES.clear();
    }

    private static void loadIncomplete(JsonElement el, Item item) {
        UnresolvedItemAmmoProperties oldProperties = UNRESOLVED_PROPERTIES.computeIfAbsent(item, i -> new UnresolvedItemAmmoProperties());
        UnresolvedItemAmmoProperties newProperties = CODEC.parse(JsonOps.INSTANCE, el)
                .getOrThrow(s -> new IllegalStateException("Error decoding JSON: " + s));

        applyProperties(oldProperties.defaultModeProperties, newProperties.defaultModeProperties);

        for (Map.Entry<String, UnresolvedModeAmmoProperties> mode : newProperties.modeProperties.entrySet()) {
            UnresolvedModeAmmoProperties oldModeProperties = oldProperties.modeProperties.computeIfAbsent(mode.getKey(), n -> new UnresolvedModeAmmoProperties());
            applyProperties(oldModeProperties, mode.getValue());
        }
    }

    private static void applyProperties(UnresolvedModeAmmoProperties oldProperties, UnresolvedModeAmmoProperties newProperties) {
        if (newProperties.replacePrimaryAmmo)
            oldProperties.primaryAmmo.clear();
        oldProperties.primaryAmmo.putAll(newProperties.primaryAmmo);

        if (newProperties.noUnlimitedProjectile) {
            oldProperties.unlimitedProjectile = null;
        } else if (newProperties.unlimitedProjectile != null) {
            oldProperties.unlimitedProjectile = newProperties.unlimitedProjectile;
        }

        if (newProperties.replacePrimaryReloadItems)
            oldProperties.unlimitedPrimaryReloadItems.clear();
        oldProperties.unlimitedPrimaryReloadItems.addAll(newProperties.unlimitedPrimaryReloadItems);

        if (newProperties.replaceUnlimitedSecondaryReloadItems)
            oldProperties.unlimitedSecondaryReloadItems.clear();
        oldProperties.unlimitedSecondaryReloadItems.addAll(newProperties.unlimitedSecondaryReloadItems);

        if (newProperties.replaceMagazines)
            oldProperties.magazines.clear();
        oldProperties.magazines.addAll(newProperties.magazines);

        if (newProperties.replaceSpeedloaders)
            oldProperties.speedloaders.clear();
        oldProperties.speedloaders.addAll(newProperties.speedloaders);

        if (newProperties.replaceSecondaryAmmo)
            oldProperties.secondaryAmmo.clear();
        oldProperties.secondaryAmmo.addAll(newProperties.secondaryAmmo);
    }

    public static RFEFirearmProperties<RFEFirearmModeAmmoProperties> getAmmoProperties(Item item) {
        return FIREARM_AMMO_PROPERTIES.getOrDefault(item, EMPTY);
    }

    public static RFEFirearmProperties<RFEFirearmModeAmmoProperties> getAmmoProperties(ItemStack item) {
        return getAmmoProperties(item.getItem());
    }

    public static void syncToAll() {
        RFENetwork.sendToAll(ClientboundSyncFirearmAmmoPropertiesPacket.fromLoadedProperties());
    }

    public static void syncToPlayer(ServerPlayer player) {
        RFENetwork.sendToPlayer(ClientboundSyncFirearmAmmoPropertiesPacket.fromLoadedProperties(), player);
    }

    public record ClientboundSyncFirearmAmmoPropertiesPacket(Reference2ObjectOpenHashMap<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> properties) implements RFEPacket {
        private static final StreamCodec<RegistryFriendlyByteBuf, RFEFirearmProperties<UnresolvedModeAmmoProperties>> UNRESOLVED_PROPERTIES_STREAM_CODEC =
                RFEFirearmProperties.makeStreamCodec(UnresolvedModeAmmoProperties.SYNC_STREAM_CODEC);

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncFirearmAmmoPropertiesPacket> STREAM_CODEC =
                ByteBufCodecs.map(Reference2ObjectOpenHashMap::new, ByteBufCodecs.registry(Registries.ITEM), UNRESOLVED_PROPERTIES_STREAM_CODEC)
                        .map(ClientboundSyncFirearmAmmoPropertiesPacket::new, ClientboundSyncFirearmAmmoPropertiesPacket::properties);

        static ClientboundSyncFirearmAmmoPropertiesPacket fromLoadedProperties() {
            Reference2ObjectOpenHashMap<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> unresolvedPropertiesByItem = new Reference2ObjectOpenHashMap<>();

            for (Map.Entry<Item, RFEFirearmProperties<RFEFirearmModeAmmoProperties>> entry : FIREARM_AMMO_PROPERTIES.entrySet()) {
                RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolved = entry.getValue();
                UnresolvedModeAmmoProperties defaultUnresolved = fromResolvedProperties(resolved.defaultProperties());
                ImmutableMap.Builder<String, UnresolvedModeAmmoProperties> unresolvedByMode = ImmutableMap.builder();
                for (Map.Entry<String, RFEFirearmModeAmmoProperties> modeEntry : resolved.propertiesByMode().entrySet())
                    unresolvedByMode.put(modeEntry.getKey(), fromResolvedProperties(modeEntry.getValue()));
                unresolvedPropertiesByItem.put(entry.getKey(), new RFEFirearmProperties<>(defaultUnresolved, unresolvedByMode.build()));
            }

            return new ClientboundSyncFirearmAmmoPropertiesPacket(unresolvedPropertiesByItem);
        }

        private static UnresolvedModeAmmoProperties fromResolvedProperties(RFEFirearmModeAmmoProperties resolved) {
            UnresolvedModeAmmoProperties unresolved = new UnresolvedModeAmmoProperties();
            for (Map.Entry<AmmoPredicate, RFEProjectileType> entry : resolved.primaryAmmo().entrySet()) {
                ResourceLocation loc = RFEProjectileTypeHandler.getProjectileTypeId(entry.getValue());
                if (loc != null)
                    unresolved.primaryAmmo.put(entry.getKey(), loc);
            }
            unresolved.magazines.addAll(resolved.magazines());
            unresolved.speedloaders.addAll(resolved.speedloaders());
            unresolved.secondaryAmmo.addAll(resolved.secondaryAmmo());
            if (resolved.unlimitedProjectile() != null)
                unresolved.unlimitedProjectile = RFEProjectileTypeHandler.getProjectileTypeId(resolved.unlimitedProjectile());
            unresolved.unlimitedPrimaryReloadItems.addAll(resolved.unlimitedPrimaryReloadItems());
            unresolved.unlimitedSecondaryReloadItems.addAll(resolved.unlimitedSecondaryReloadItems());
            return unresolved;
        }

        @Override
        public void handle(Executor exec, PacketListener listener, Player player) {
            FIREARM_AMMO_PROPERTIES.clear();
            for (Map.Entry<Item, RFEFirearmProperties<UnresolvedModeAmmoProperties>> entry : this.properties.entrySet()) {
                Item item = entry.getKey();
                RFEFirearmProperties<UnresolvedModeAmmoProperties> unresolved = entry.getValue();

                ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> resolvedPropertiesByMode = ImmutableMap.builder();
                for (Map.Entry<String, UnresolvedModeAmmoProperties> entry1 : unresolved.propertiesByMode().entrySet())
                    resolvedPropertiesByMode.put(entry1.getKey(), entry1.getValue().resolve(item));

                RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolved = new RFEFirearmProperties<>(unresolved.defaultProperties().resolve(item),
                        resolvedPropertiesByMode.build());
                FIREARM_AMMO_PROPERTIES.put(item, resolved);
            }
        }
    }

    private static class UnresolvedItemAmmoProperties {
        public UnresolvedModeAmmoProperties defaultModeProperties = new UnresolvedModeAmmoProperties();
        public Map<String, UnresolvedModeAmmoProperties> modeProperties = new Object2ObjectOpenHashMap<>();

        public static UnresolvedItemAmmoProperties fromCodec(UnresolvedModeAmmoProperties defaultModeProperties,
                                                             Map<String, UnresolvedModeAmmoProperties> modeProperties) {
            UnresolvedItemAmmoProperties properties = new UnresolvedItemAmmoProperties();
            properties.defaultModeProperties = defaultModeProperties;
            properties.modeProperties.putAll(modeProperties);
            return properties;
        }

        public RFEFirearmProperties<RFEFirearmModeAmmoProperties> resolve(Item item) {
            ImmutableMap.Builder<String, RFEFirearmModeAmmoProperties> resolvedModeProperties = ImmutableMap.builder();
            for (Map.Entry<String, UnresolvedModeAmmoProperties> entry : this.modeProperties.entrySet())
                resolvedModeProperties.put(entry.getKey(), entry.getValue().resolve(item));
            return new RFEFirearmProperties<>(this.defaultModeProperties.resolve(item), resolvedModeProperties.build());
        }
    }

    private static class UnresolvedModeAmmoProperties {
        private static final Codec<Map<AmmoPredicate, ResourceLocation>> PRIMARY_AMMO_CODEC = Codec.pair(
                AmmoPredicate.CODEC.fieldOf("ammo").codec(), ResourceLocation.CODEC.fieldOf("fires").codec()).listOf()
                .xmap(li -> li.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
                        map -> map.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).toList());

        public static final MapCodec<UnresolvedModeAmmoProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.BOOL.optionalFieldOf("replace_primary_ammo", false).forGetter(p -> p.replacePrimaryAmmo),
                PRIMARY_AMMO_CODEC.optionalFieldOf("primary_ammo", new LinkedHashMap<>()).forGetter(p -> p.primaryAmmo),
                Codec.BOOL.optionalFieldOf("replace_magazines", false).forGetter(p -> p.replaceMagazines),
                Codec.list(AmmoPredicate.CODEC).optionalFieldOf("magazines", new ArrayList<>()).forGetter(p -> p.magazines),
                Codec.BOOL.optionalFieldOf("replace_speedloaders", false).forGetter(p -> p.replaceSpeedloaders),
                Codec.list(AmmoPredicate.CODEC).optionalFieldOf("speedloaders", new ArrayList<>()).forGetter(p -> p.speedloaders),
                Codec.BOOL.optionalFieldOf("replace_secondary_ammo", false).forGetter(p -> p.replaceSecondaryAmmo),
                Codec.list(AmmoPredicate.CODEC).optionalFieldOf("secondary_ammo", new ArrayList<>()).forGetter(p -> p.secondaryAmmo),
                Codec.BOOL.optionalFieldOf("no_unlimited_projectile", false).forGetter(p -> p.noUnlimitedProjectile),
                ResourceLocation.CODEC.optionalFieldOf("unlimited_projectile").forGetter(p -> Optional.ofNullable(p.unlimitedProjectile)),
                Codec.BOOL.optionalFieldOf("replace_unlimited_primary_reload_items", false).forGetter(p -> p.replacePrimaryReloadItems),
                Codec.list(ItemStack.CODEC).optionalFieldOf("unlimited_primary_reload_items", new ArrayList<>()).forGetter(p -> p.unlimitedPrimaryReloadItems),
                Codec.BOOL.optionalFieldOf("replace_unlimited_secondary_reload_itesm", false).forGetter(p -> p.replaceUnlimitedSecondaryReloadItems),
                Codec.list(ItemStack.CODEC).optionalFieldOf("unlimited_secondary_reload_items", new ArrayList<>()).forGetter(p -> p.unlimitedSecondaryReloadItems)
        ).apply(o, UnresolvedModeAmmoProperties::fromCodec));

        public static final StreamCodec<RegistryFriendlyByteBuf, UnresolvedModeAmmoProperties> SYNC_STREAM_CODEC = RFEByteBufCodecUtils.composite7(
                ByteBufCodecs.map(LinkedHashMap::new, AmmoPredicate.STREAM_CODEC, ResourceLocation.STREAM_CODEC), p -> p.primaryAmmo,
                AmmoPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.magazines,
                AmmoPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.speedloaders,
                AmmoPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.secondaryAmmo,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), p -> Optional.ofNullable(p.unlimitedProjectile),
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.unlimitedPrimaryReloadItems,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), p -> p.unlimitedSecondaryReloadItems,
                UnresolvedModeAmmoProperties::fromStreamCodec);

        public boolean replacePrimaryAmmo = false;
        public Map<AmmoPredicate, ResourceLocation> primaryAmmo = new LinkedHashMap<>();
        public boolean replaceMagazines = false;
        public List<AmmoPredicate> magazines = new ArrayList<>();
        public boolean replaceSpeedloaders = false;
        public List<AmmoPredicate> speedloaders = new ArrayList<>();
        public boolean replaceSecondaryAmmo = false;
        public List<AmmoPredicate> secondaryAmmo = new ArrayList<>();
        public boolean noUnlimitedProjectile = false;
        public ResourceLocation unlimitedProjectile = null;
        public boolean replacePrimaryReloadItems = false;
        public List<ItemStack> unlimitedPrimaryReloadItems = new ArrayList<>();
        public boolean replaceUnlimitedSecondaryReloadItems = false;
        public List<ItemStack> unlimitedSecondaryReloadItems = new ArrayList<>();

        private static UnresolvedModeAmmoProperties fromCodec(boolean replacePrimaryAmmo, Map<AmmoPredicate, ResourceLocation> primaryAmmo,
                                                              boolean replaceMagazines, List<AmmoPredicate> magazines,
                                                              boolean replaceSpeedloaders, List<AmmoPredicate> speedloaders,
                                                              boolean replaceSecondaryAmmo, List<AmmoPredicate> secondaryAmmo,
                                                              boolean noUnlimitedProjectile, Optional<ResourceLocation> unlimitedProjectile,
                                                              boolean replaceUnlimitedPrimaryReloadItem, List<ItemStack> unlimitedPrimaryReloadItems,
                                                              boolean replaceUnlimitedSecondaryReloadItem, List<ItemStack> unlimitedSecondaryReloadItems) {
            UnresolvedModeAmmoProperties properties = new UnresolvedModeAmmoProperties();
            properties.replacePrimaryAmmo = replacePrimaryAmmo;
            properties.primaryAmmo.putAll(primaryAmmo);
            properties.replaceMagazines = replaceMagazines;
            properties.magazines.addAll(magazines);
            properties.replaceSpeedloaders = replaceSpeedloaders;
            properties.speedloaders.addAll(speedloaders);
            properties.replaceSecondaryAmmo = replaceSecondaryAmmo;
            properties.secondaryAmmo.addAll(secondaryAmmo);
            properties.noUnlimitedProjectile = noUnlimitedProjectile;
            properties.unlimitedProjectile = unlimitedProjectile.orElse(null);
            properties.replacePrimaryReloadItems = replaceUnlimitedPrimaryReloadItem;
            properties.unlimitedPrimaryReloadItems.addAll(unlimitedPrimaryReloadItems);
            properties.replaceUnlimitedSecondaryReloadItems = replaceUnlimitedSecondaryReloadItem;
            properties.unlimitedSecondaryReloadItems.addAll(unlimitedSecondaryReloadItems);
            return properties;
        }

        private static UnresolvedModeAmmoProperties fromStreamCodec(Map<AmmoPredicate, ResourceLocation> primaryAmmo,
                                                                    List<AmmoPredicate> magazines,
                                                                    List<AmmoPredicate> speedloaders,
                                                                    List<AmmoPredicate> secondaryAmmo,
                                                                    Optional<ResourceLocation> unlimitedProjectile,
                                                                    List<ItemStack> unlimitedPrimaryReloadItems,
                                                                    List<ItemStack> unlimitedSecondaryReloadItems) {
            UnresolvedModeAmmoProperties properties = new UnresolvedModeAmmoProperties();
            properties.primaryAmmo.putAll(primaryAmmo);
            properties.magazines.addAll(magazines);
            properties.speedloaders.addAll(speedloaders);
            properties.secondaryAmmo.addAll(secondaryAmmo);
            properties.unlimitedProjectile = unlimitedProjectile.orElse(null);
            properties.unlimitedPrimaryReloadItems = unlimitedPrimaryReloadItems;
            properties.unlimitedSecondaryReloadItems = unlimitedSecondaryReloadItems;
            return properties;
        }

        public UnresolvedModeAmmoProperties fork() {
            UnresolvedModeAmmoProperties newProperties = new UnresolvedModeAmmoProperties();
            newProperties.primaryAmmo = new LinkedHashMap<>(this.primaryAmmo);
            newProperties.magazines = new ArrayList<>(this.magazines);
            newProperties.speedloaders = new ArrayList<>(this.speedloaders);
            newProperties.secondaryAmmo = new ArrayList<>(this.secondaryAmmo);
            newProperties.unlimitedProjectile = this.unlimitedProjectile;
            newProperties.unlimitedPrimaryReloadItems = this.unlimitedPrimaryReloadItems;
            return newProperties;
        }

        public RFEFirearmModeAmmoProperties resolve(Item item) {
            ImmutableMap.Builder<AmmoPredicate, RFEProjectileType> primaryAmmo = ImmutableMap.builder();
            for (Map.Entry<AmmoPredicate, ResourceLocation> entry : this.primaryAmmo.entrySet()) {
                RFEProjectileType projectileType = loadProjectileTypeOrWarnIgnore(entry.getValue(), item);
                if (projectileType != null)
                    primaryAmmo.put(entry.getKey(), projectileType);
            }
            ImmutableList.Builder<AmmoPredicate> magazines = ImmutableList.builder();
            magazines.addAll(this.magazines);
            ImmutableList.Builder<AmmoPredicate> speedloaders = ImmutableList.builder();
            speedloaders.addAll(this.speedloaders);
            ImmutableList.Builder<AmmoPredicate> secondaryAmmo = ImmutableList.builder();
            secondaryAmmo.addAll(this.secondaryAmmo);
            RFEProjectileType unlimitedProjectile = null;
            if (this.unlimitedProjectile != null)
                unlimitedProjectile = loadProjectileTypeOrWarnIgnore(this.unlimitedProjectile, item);
            ImmutableMap<AmmoPredicate, RFEProjectileType> primaryAmmoBuilt = primaryAmmo.build();
            ImmutableList<AmmoPredicate> magazinesBuilt = magazines.build();
            ImmutableList<AmmoPredicate> speedloadersBuilt = speedloaders.build();
            ImmutableList<AmmoPredicate> secondariesBuilt = secondaryAmmo.build();
            ImmutableList<ItemStack> finalUnlimitedPrimaryReloadItems = validateUnlimitedPrimaryReloadItems(this.unlimitedPrimaryReloadItems,
                    RFEUtils.orAllPredicates(magazinesBuilt), RFEUtils.orAllPredicates(speedloadersBuilt),
                    RFEUtils.orAllPredicates(primaryAmmoBuilt.keySet()), RFEUtils.orAllPredicates(secondariesBuilt), item)
                    ? ImmutableList.copyOf(this.unlimitedPrimaryReloadItems) : ImmutableList.of();
            ImmutableList<ItemStack> finalUnlimitedSecondaryReloadItems = validateUnlimitedSecondaryReloadItems(this.unlimitedSecondaryReloadItems,
                    RFEUtils.orAllPredicates(secondariesBuilt), item) ? ImmutableList.copyOf(this.unlimitedSecondaryReloadItems) : ImmutableList.of();
            return new RFEFirearmModeAmmoProperties(primaryAmmoBuilt, magazinesBuilt, speedloadersBuilt, secondariesBuilt,
                    unlimitedProjectile, finalUnlimitedPrimaryReloadItems, finalUnlimitedSecondaryReloadItems);
        }

        @Nullable
        private static RFEProjectileType loadProjectileTypeOrWarnIgnore(ResourceLocation typeId, Item item) {
            RFEProjectileType type = RFEProjectileTypeHandler.getProjectileType(typeId);
            if (type == null) {
                LOGGER.warn("Missing projectile type {} in firearm item ammo properties for firearm item {}, skipping",
                        typeId, BuiltInRegistries.ITEM.getKey(item));
                return null;
            } else {
                return type;
            }
        }

        private static boolean validateUnlimitedPrimaryReloadItems(List<ItemStack> unlimitedReloadItems, Predicate<ItemStack> magazinePredicate,
                                                                   Predicate<ItemStack> speedloaderPredicate, Predicate<ItemStack> ammoPredicate,
                                                                   Predicate<ItemStack> secondaryPredicate, Item item) {
            for (ItemStack s : unlimitedReloadItems) {
                if (s.isEmpty()) {
                    LOGGER.warn("Cannot specify empty unlimited primary reload item for firearm item {}", item);
                    return false;
                }
                if (ammoPredicate.test(s))
                    continue;
                if (magazinePredicate.test(s) || speedloaderPredicate.test(s)) {
                    if (!(s.getItem() instanceof MagazineItem magazineItem)) {
                        LOGGER.warn("Unlimited primary reload item {} for firearm item {} is specified as a magazine/speedloader but is not actually a magazine/speedloader item (ritchiesfirearmengine:magazine, ritchiesfirearmengine:speedloader), skipping list", s, item);
                        return false;
                    }
                    List<ItemStack> storedAmmo = magazineItem.getStoredAmmo(s);
                    if (storedAmmo.isEmpty()) {
                        LOGGER.warn("Unlimited primary reload item {} for firearm item {} is an empty magazine/speedloader item, skipping list", s, item);
                        return false;
                    }
                    for (ItemStack ammoStack : storedAmmo) {
                        if (!ammoPredicate.test(ammoStack)) {
                            LOGGER.warn("Unlimited primary reload item {} for firearm item {} contains invalid primary ammo stack {}, skipping list", s, item, ammoStack);
                            return false;
                        }
                    }
                    List<ItemStack> storedSecondaryAmmo = magazineItem.getStoredSecondaryAmmo(s);
                    for (ItemStack ammoStack : storedSecondaryAmmo) {
                        if (!secondaryPredicate.test(ammoStack)) {
                            LOGGER.warn("Unlimited primary reload item {} for firearm item {} contains invalid secondary ammo stack {}, skipping list", s, item, ammoStack);
                            return false;
                        }
                    }
                } else {
                    LOGGER.warn("Unlimited primary reload item {} for firearm item {} is not consistent with loaded firearm item ammo properties (neither valid ammo nor valid magazine/speedloader with valid ammo), skipping list", s, item);
                    return false;
                }
            }
            return true;
        }

        private static boolean validateUnlimitedSecondaryReloadItems(List<ItemStack> unlimitedReloadItems, Predicate<ItemStack> secondaryPredicate, Item item) {
            for (ItemStack s : unlimitedReloadItems) {
                if (s.isEmpty()) {
                    LOGGER.warn("Cannot specify empty unlimited secondary reload item for firearm item {}", item);
                    return false;
                }
                if (secondaryPredicate.test(s))
                    continue;
                LOGGER.warn("Unlimited secondary reload item {} for firearm item {} is not a valid secondary ammo item, skipping list", unlimitedReloadItems, item);
                return false;
            }
            return true;
        }
    }

}
