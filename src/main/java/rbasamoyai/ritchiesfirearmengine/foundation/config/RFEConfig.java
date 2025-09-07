package rbasamoyai.ritchiesfirearmengine.foundation.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
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

    private static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = pair.getRight();
        SERVER = pair.getLeft();
    }

    public static class Client {
        public final ConfigValue<Integer> maxMagazineItemTypesDisplayed;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Ritchie's Firearm Engine client configuration settings")
                    .push("client");

            this.maxMagazineItemTypesDisplayed = builder
                    .comment("The maximum amount of item types displayed in item tooltips. Default 5, must be at least 1.")
                    .translation("ritchiesfirearmengine.configgui.maxMagazineItemTypesDisplayed")
                    .define("maxMagazineItemTypesDisplayed", 5, this::validateMaxMagazineItemTypesDisplayed);

            builder.pop();
        }

        private boolean validateMaxMagazineItemTypesDisplayed(Object o) {
            return o instanceof Integer i && i >= 1;
        }
    }

    private static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = pair.getRight();
        CLIENT = pair.getLeft();
    }

    public static void registerConfigs(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RFEConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RFEConfig.CLIENT_SPEC);

        modBus.addListener(RFEConfig::onLoad);
        modBus.addListener(RFEConfig::onReload);
    }

    private static void onLoad(final ModConfigEvent.Loading event) {

    }

    private static void onReload(final ModConfigEvent.Reloading event) {

    }

}
