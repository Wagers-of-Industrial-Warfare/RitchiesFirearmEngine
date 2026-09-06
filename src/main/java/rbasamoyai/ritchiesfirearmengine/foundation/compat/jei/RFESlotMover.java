package rbasamoyai.ritchiesfirearmengine.foundation.compat.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.RFEItemAttachmentsScreen;

import java.util.List;

public class RFESlotMover implements IGuiContainerHandler<RFEItemAttachmentsScreen> {

    @Override
    public List<Rect2i> getGuiExtraAreas(RFEItemAttachmentsScreen containerScreen) {
        return containerScreen.getExtraAreas();
    }

}
