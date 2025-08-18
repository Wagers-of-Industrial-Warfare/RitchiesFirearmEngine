package rbasamoyai.ritchiesfirearmengine.foundation.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

public class RFEConfig {

    public static class Server {
        public final BooleanValue enableEntityPenetration;
        public final BooleanValue enableBlockPenetration;
        public final BooleanValue enableBlockBreaking;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Ritchies Firearm Engine server configuration settings")
                    .push("server");

            this.enableEntityPenetration = builder
                    .comment("Set this to true to globally enable entity projectile penetration (where supported). Default true.")
                    .translation("ritchiesfirearmengine.configgui.enableEntityPenetration")
                    .define("enableEntityPenetration", true);

            this.enableBlockPenetration = builder
                    .comment("Set this to true to globally enable block projectile penetration (where supported). Default true.")
                    .translation("ritchiesfirearmengine.configgui.enableBlockPenetration")
                    .define("enableBlockPenetration", true);

            this.enableBlockBreaking = builder
                    .comment("Set this to true to globally enable block projectile breaking (where supported). Default true.")
                    .translation("ritchiesfirearmengine.configgui.enableBlockBreaking")
                    .define("enableBlockBreaking", true);

            builder.pop();
        }
    }

    private static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = pair.getRight();
        SERVER = pair.getLeft();
    }

    public static void registerConfigs(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RFEConfig.SERVER_SPEC);

        modBus.addListener(RFEConfig::onLoad);
        modBus.addListener(RFEConfig::onReload);
    }

    private static void onLoad(final ModConfigEvent.Loading event) {

    }

    private static void onReload(final ModConfigEvent.Reloading event) {

    }

}
