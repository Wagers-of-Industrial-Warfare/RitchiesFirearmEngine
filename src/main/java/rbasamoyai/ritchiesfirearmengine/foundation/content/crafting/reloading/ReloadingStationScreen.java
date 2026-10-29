package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.reloading;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

public class ReloadingStationScreen extends AbstractContainerScreen<ReloadingStationMenu> {

    private static final ResourceLocation MENU_TEXTURE = RitchiesFirearmEngine.resource("textures/gui/reloading_station.png");

    public ReloadingStationScreen(ReloadingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 173;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(MENU_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (!this.menu.getCarried().isEmpty() || this.hoveredSlot == null)
            return;
        if (this.hoveredSlot.getItem().isEmpty() && this.hoveredSlot instanceof ReloadingIngredientSlot ingredientSlot) {
            guiGraphics.renderTooltip(this.font, ingredientSlot.getEmptyText(), x, y);
        }
    }

}
