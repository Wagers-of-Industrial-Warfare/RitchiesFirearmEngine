package rbasamoyai.ritchiesfirearmengine;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.config.RFEConfig;

@Mod(RitchiesFirearmEngine.MODID)
public class RitchiesFirearmEngine {

    public static final String MODID = "ritchiesfirearmengine";
    private static final Logger LOGGER = LogUtils.getLogger();


    public RitchiesFirearmEngine() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        modBus.addListener(this::commonSetup);
        forgeBus.register(this);

        RFEConfig.registerConfigs(modBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

}
