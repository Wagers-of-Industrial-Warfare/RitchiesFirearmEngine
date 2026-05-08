package rbasamoyai.ritchiesfirearmengine.foundation.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;

public class RFEConfig {

    public static class Server {
        public final BooleanValue enableEntityPenetration;
        public final BooleanValue enableBlockPenetration;
        public final BooleanValue enableBlockBreaking;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("Ritchie's Firearm Engine server configuration settings")
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

    private static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = pair.getRight();
        SERVER = pair.getLeft();
    }

    public static class Client {
        public final ConfigValue<Integer> maxMagazineItemTypesDisplayed;
        public final BooleanValue renderCrosshairOnShoulderSurfingAim;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Ritchie's Firearm Engine client configuration settings")
                    .push("client");

            this.maxMagazineItemTypesDisplayed = builder
                    .comment("The maximum amount of item types displayed in item tooltips. Default 5, must be at least 1.")
                    .translation("ritchiesfirearmengine.configgui.maxMagazineItemTypesDisplayed")
                    .define("maxMagazineItemTypesDisplayed", 5, this::validateMaxMagazineItemTypesDisplayed);

            this.renderCrosshairOnShoulderSurfingAim = builder
                    .comment("Whether the firearm crosshair should render on aiming when Shoulder Surfing is installed and in the over-the-shoudler view. Default true.")
                    .translation("ritchiesfirearmengine.configgui.renderCrosshairOnShoulderSurfingAim")
                    .define("renderCrosshairOnShoulderSurfingAim", true);

            builder.pop();
        }

        private boolean validateMaxMagazineItemTypesDisplayed(Object o) {
            return o instanceof Integer i && i >= 1;
        }
    }

    private static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Client, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = pair.getRight();
        CLIENT = pair.getLeft();
    }

    public static void registerConfigs(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, RFEConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, RFEConfig.CLIENT_SPEC);

        modBus.addListener(RFEConfig::onLoad);
        modBus.addListener(RFEConfig::onReload);
    }

    private static void onLoad(final ModConfigEvent.Loading event) {

    }

    private static void onReload(final ModConfigEvent.Reloading event) {

    }

}
