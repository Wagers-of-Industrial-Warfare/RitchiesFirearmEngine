package rbasamoyai.ritchiesfirearmengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPluginManager;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.index.FoundationDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.BiConsumer;

@Mod(RitchiesFirearmEngine.MOD_ID)
public class RitchiesFirearmEngine {

    public static final String MOD_ID = "ritchiesfirearmengine";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RitchiesFirearmEngine(IEventBus modBus, Dist dist, ModContainer container) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;

        modBus.addListener(this::onRegisterObjects);
        modBus.addListener(this::onAddPackFinders);

        forgeBus.addListener(this::onAddReloadListeners);
        forgeBus.addListener(this::onSyncDatapack);
        forgeBus.addListener(this::onLevelLoad);
        forgeBus.addListener(this::onLevelUnload);
        forgeBus.addListener(this::onEntityJoinLevel);
        forgeBus.addListener(this::onEntityLeaveLevel);
        forgeBus.addListener(this::onPlayerLoggedIn);
        forgeBus.addListener(this::onLevelTick);
        forgeBus.addListener(this::onLeftClickBlock);

        RFEConfig.registerConfigs(modBus, container);

        RFEPackLoader.prepareResources();

        RFENetwork.init();
    }

    private void onRegisterObjects(final RegisterEvent event) {
        ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
        if (key == Registries.ITEM) {
            BiConsumer<ResourceLocation, Item> cons = (loc, item) -> event.register(Registries.ITEM, loc, () -> item);
            RFEPackLoader.loadItems(cons);
            RFEPluginManager.loadItems(cons);
        } else if (key == Registries.CREATIVE_MODE_TAB) {
            BiConsumer<ResourceLocation, CreativeModeTab> cons = (loc, tab) -> event.register(Registries.CREATIVE_MODE_TAB, loc, () -> tab);
            RFEPackLoader.loadCreativeModeTabs(cons);
            RFEPluginManager.loadCreativeModeTabs(cons);
        } else if (key == Registries.PARTICLE_TYPE) {
            BiConsumer<ResourceLocation, ParticleType<?>> cons = (loc, type) -> event.register(Registries.PARTICLE_TYPE, loc, () -> type);
            RFEPluginManager.loadParticleTypes(cons);
        } else if (key == Registries.DATA_COMPONENT_TYPE) {
            BiConsumer<ResourceLocation, DataComponentType<?>> cons = (loc, type) -> event.register(Registries.DATA_COMPONENT_TYPE, loc, () -> type);
            FoundationDataComponents.register(cons);
            RFEPluginManager.loadDataComponentTypes(cons);
        }
    }

    private void onAddPackFinders(final AddPackFindersEvent event) {
        RFEPackLoader.addPacks(event.getPackType(), event::addRepositorySource);
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event) {
        RFEPluginManager.registerResourceListeners((id, listener) -> event.addListener(listener));
    }

    private void onSyncDatapack(final OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        boolean singleplayer = server.isSingleplayer() && !server.isPublished();
        if (event.getPlayer() == null) {
            RFECommonEvents.onDatapackReload(singleplayer);
        } else {
            RFECommonEvents.onDatapackSync(event.getPlayer(), singleplayer);
        }
    }

    private void onLevelLoad(final LevelEvent.Load event) {
        RFECommonEvents.onLevelLoad(event.getLevel());
    }

    private void onLevelUnload(final LevelEvent.Unload event) {
        RFECommonEvents.onLevelUnload(event.getLevel());
    }

    private void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        RFECommonEvents.onEntityJoin(event.getEntity(), event.getLevel());
    }

    private void onEntityLeaveLevel(final EntityLeaveLevelEvent event) {
        RFECommonEvents.onEntityRemoved(event.getEntity());
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        RFECommonEvents.onPlayerLoggedIn(event.getEntity());
    }

    private void onLevelTick(final LevelTickEvent.Post event) {
        RFECommonEvents.onLevelTickEnd(event.getLevel());
    }

    private void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (RFECommonEvents.onLeftClickBlock(event.getEntity()))
            event.setCanceled(true);
    }

    public static ResourceLocation resource(String path) { return RFEUtils.location(MOD_ID, path); }

}
