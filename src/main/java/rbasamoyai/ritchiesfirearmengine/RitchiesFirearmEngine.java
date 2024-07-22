package rbasamoyai.ritchiesfirearmengine;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.pack_content.resources.RFEPackLoader;
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
        forgeBus.register(this);

        RFEConfig.registerConfigs(modBus);

        RFEPackLoader.prepareResources();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RFEClient.init(modBus, forgeBus));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void onRegisterObjects(final RegisterEvent event) {
        if (event.getRegistryKey() == Registries.ITEM) {
            RFEPackLoader.loadItems();
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            RFEPackLoader.loadCreativeModeTabs();
        }
    }

    private void onAddPackFinders(final AddPackFindersEvent event) {
        RFEPackLoader.addPacks(event.getPackType(), event::addRepositorySource);
    }

    public static ResourceLocation resource(String path) { return RFEUtils.location(MOD_ID, path); }

}
