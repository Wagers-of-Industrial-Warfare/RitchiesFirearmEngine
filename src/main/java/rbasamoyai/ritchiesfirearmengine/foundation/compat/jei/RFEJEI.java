package rbasamoyai.ritchiesfirearmengine.foundation.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsScreen;

@JeiPlugin
@SuppressWarnings("unused")
public class RFEJEI implements IModPlugin {

    private static final ResourceLocation ID = RitchiesFirearmEngine.resource("jei_plugin");

    @Override public ResourceLocation getPluginUid() { return ID; }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(RFEItemAttachmentsScreen.class, new RFESlotMover());
    }

}
