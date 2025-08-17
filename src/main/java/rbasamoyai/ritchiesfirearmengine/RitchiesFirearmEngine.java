package rbasamoyai.ritchiesfirearmengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEPluginManager;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.pack_loading.RFEPackLoader;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

import java.util.function.BiConsumer;

@Mod(RitchiesFirearmEngine.MOD_ID)
public class RitchiesFirearmEngine {

    public static final String MOD_ID = "ritchiesfirearmengine";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RitchiesFirearmEngine() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        modBus.addListener(this::onRegisterObjects);
        modBus.addListener(this::commonSetup);
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

        RFEConfig.registerConfigs(modBus);

        RFEPackLoader.prepareResources();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RFEClientForge.init(modBus, forgeBus));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            RFENetwork.init();
        });
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
            BiConsumer<ResourceLocation, ParticleType<?>> cons = (loc, tab) -> event.register(Registries.PARTICLE_TYPE, loc, () -> tab);
            RFEPluginManager.loadParticleTypes(cons);
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

    private void onLevelTick(final TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            RFECommonEvents.onLevelTick(event.level);
    }

    private void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCancelable() && RFECommonEvents.onLeftClickBlock(event.getEntity()))
            event.setCanceled(true);
    }

    public static ResourceLocation resource(String path) { return RFEUtils.location(MOD_ID, path); }

}
