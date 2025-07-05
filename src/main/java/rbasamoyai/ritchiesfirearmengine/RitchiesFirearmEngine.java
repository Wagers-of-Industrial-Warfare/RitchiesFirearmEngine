package rbasamoyai.ritchiesfirearmengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
        forgeBus.addListener(this::onPlayerLoggedIn);
        forgeBus.addListener(this::onLevelTick);

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
        if (event.getRegistryKey() == Registries.ITEM) {
            RFEPackLoader.loadItems((loc, item) -> event.register(Registries.ITEM, loc, () -> item));
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            RFEPackLoader.loadCreativeModeTabs((loc, tab) -> event.register(Registries.CREATIVE_MODE_TAB, loc, () -> tab));
        }
    }

    private void onAddPackFinders(final AddPackFindersEvent event) {
        RFEPackLoader.addPacks(event.getPackType(), event::addRepositorySource);
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event) {
        RFEPluginManager.registerResourceListeners((id, listener) -> event.addListener(listener));
    }

    private void onSyncDatapack(final OnDatapackSyncEvent event) {
        boolean singleplayer = event.getPlayerList().getServer().isSingleplayer();
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

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        RFECommonEvents.onPlayerLoggedIn(event.getEntity());
    }

    private void onLevelTick(final TickEvent.LevelTickEvent event) {
        RFECommonEvents.onLevelTick(event.level);
    }

    public static ResourceLocation resource(String path) { return RFEUtils.location(MOD_ID, path); }

}
