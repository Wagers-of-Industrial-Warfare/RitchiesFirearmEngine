package rbasamoyai.ritchiesfirearmengine.foundation.index;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsMenu;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsScreen;

public class FoundationMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, RitchiesFirearmEngine.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<RFEItemAttachmentsMenu>> FIELD_ATTACHMENTS_MENU = MENU_TYPES.register("field_attachments_menu",
            () -> IMenuTypeExtension.create(RFEItemAttachmentsMenu::client));

    @EventBusSubscriber(modid = RitchiesFirearmEngine.MOD_ID)
    public static class Screens {
        @SubscribeEvent
        public static void onRegisterMenuScreens(final RegisterMenuScreensEvent evt) {
            evt.register(FIELD_ATTACHMENTS_MENU.get(), RFEItemAttachmentsScreen::new);
        }
    }

}
