package rbasamoyai.ritchiesfirearmengine.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class RFEConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void registerConfigs(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RFEConfig.SPEC);

        modBus.addListener(RFEConfig::onLoad);
    }

    private static void onLoad(final ModConfigEvent event) {

    }

}
