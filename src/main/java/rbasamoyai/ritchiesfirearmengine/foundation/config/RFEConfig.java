package rbasamoyai.ritchiesfirearmengine.foundation.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

public class RFEConfig {

    public static class Server {
        public final BooleanValue enableEntityPenetration;
        public final BooleanValue enableBlockPenetration;
        public final BooleanValue enableBlockBreaking;
        public final IntValue fieldAttachmentsMenuOpenTime;

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

            this.fieldAttachmentsMenuOpenTime = builder
                    .comment("[in Ticks]", "Time to open the attachments screen in the field (that is, without a workbench).")
                    .translation("ritchiesfirearmengine.configgui.fieldAttachmentsMenuOpenTime")
                    .defineInRange("fieldAttachmentsMenuOpenTime", 40, 0, Integer.MAX_VALUE);

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
        public final IntValue maxMagazineItemTypesDisplayed;
        public final BooleanValue renderCrosshairOnShoulderSurfingAim;
        public final IntValue compatibleAmmoHighlightColor;
        public final IntValue incompatibleAmmoHighlightColor;
        public final BooleanValue compatibleAmmoHighlightGradient;
        public final DoubleValue zoomTurnModifierIntensity;
        public final IntValue tooltipProgressBarLength;
        public final IntValue attachmentScreenBackgroundColor;
        public final IntValue attachmentScreenItemColor;
        public final BooleanValue attachmentScreenItemGradient;
        public final IntValue attachmentScreenPointerColor;
        public final IntValue attachmentScreenValidSlotColor;
        public final BooleanValue attachmentScreenValidSlotGradient;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Ritchie's Firearm Engine client configuration settings")
                    .push("client");

            this.maxMagazineItemTypesDisplayed = builder
                    .comment("The maximum amount of item types displayed in item tooltips. Default 5, must be at least 1.")
                    .translation("ritchiesfirearmengine.configgui.maxMagazineItemTypesDisplayed")
                    .defineInRange("maxMagazineItemTypesDisplayed", 5, 1, Integer.MAX_VALUE);

            this.renderCrosshairOnShoulderSurfingAim = builder
                    .comment("Whether the firearm crosshair should render on aiming when Shoulder Surfing is installed and in the over-the-shoudler view. Default true.")
                    .translation("ritchiesfirearmengine.configgui.renderCrosshairOnShoulderSurfingAim")
                    .define("renderCrosshairOnShoulderSurfingAim", true);

            this.compatibleAmmoHighlightColor = builder
                    .comment("The color that is shown when highlighting currently compatible firearm ammo in ARGB.")
                    .translation("ritchiesfirearmengine.configgui.compatibleAmmoHighlightColor")
                    .defineInRange("compatibleAmmoHighlightColor", 0x7F_00FF00, Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.incompatibleAmmoHighlightColor = builder
                    .comment("The color that is shown when highlighting currently incompatible firearm ammo in ARGB.")
                    .translation("ritchiesfirearmengine.configgui.incompatibleAmmoHighlightColor")
                    .defineInRange("incompatibleAmmoHighlightColor", 0x7F_FFDF00, Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.compatibleAmmoHighlightGradient = builder
                    .comment("Set to true to render the compatible ammo highlight as a gradient, false as a solid color.")
                    .translation("ritchiesfirearmengine.configgui.compatibleAmmoHighlightGradient")
                    .define("compatibleAmmoHighlightGradient", true);

            this.zoomTurnModifierIntensity = builder
                    .comment("The amount that aim down sights zoom affects turn sensitivity. 0.0 (0%) disables turn modification, while 1.0 (100%) completely enables it.")
                    .translation("ritchiesfirearmengine.configgui.zoomTurnModifierIntensity")
                    .defineInRange("zoomTurnModifierIntensity", 1d, 0d, 1d);

            this.tooltipProgressBarLength = builder
                    .comment("The number of \"|\" characters in a tooltip progress bar, such as when opening the field attachments menu.")
                    .translation("ritchiesfirearmengine.configgui.tooltipProgressBarLength")
                    .defineInRange("tooltipProgressBarLength", 64, 1, 256);

            this.attachmentScreenBackgroundColor = builder
                    .comment("The base color of the attachment screen background gradient in RGB.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenBackgroundColor")
                    .defineInRange("attachmentScreenBackgroundColor", 0x1F7FFF, 0x000000, 0xFFFFFF);

            this.attachmentScreenItemColor = builder
                    .comment("The base color of the attachment screen item slot gradient in ARGB.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenItemColor")
                    .defineInRange("attachmentScreenItemColor", 0x7F_1F7FFF, Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.attachmentScreenItemGradient = builder
                    .comment("Set to true to render the attachment screen item highlight as a gradient, false as a solid color.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenItemGradient")
                    .define("attachmentScreenItemGradient", true);

            this.attachmentScreenPointerColor = builder
                    .comment("The color of the attachment screen pointer in ARGB.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenPointerColor")
                    .defineInRange("attachmentScreenPointerColor", 0x7F_5FDFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.attachmentScreenValidSlotColor = builder
                    .comment("The base color of the attachment screen valid slot gradient in ARGB.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenItemColor")
                    .defineInRange("attachmentScreenItemColor", 0x7F_00FF00, Integer.MIN_VALUE, Integer.MAX_VALUE);

            this.attachmentScreenValidSlotGradient = builder
                    .comment("Set to true to render the attachment screen valid slot highlight as a gradient, false as a solid color.")
                    .translation("ritchiesfirearmengine.configgui.attachmentScreenItemGradient")
                    .define("attachmentScreenItemGradient", true);

            builder.pop();
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
