package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRenderer;

public class NoHUDOverlayRenderer implements RFEHudOverlayRenderer {

    public static final NoHUDOverlayRenderer INSTANCE = new NoHUDOverlayRenderer();

    private NoHUDOverlayRenderer() {}

    @Override public void renderHUD(GuiGraphics graphics, float partialTicks, ItemStack item, Player player, boolean offhand) {}

    public static class Serializer implements RFEHudOverlayRenderer.Serializer {
        @Override public RFEHudOverlayRenderer apply(JsonObject obj) { return INSTANCE; }
    }

}
