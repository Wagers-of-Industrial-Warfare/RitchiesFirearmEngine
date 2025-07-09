package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public interface RFEHudOverlayRenderer {

    void renderHUD(GuiGraphics graphics, float partialTicks, ItemStack item, Player player, boolean offhand);

    @FunctionalInterface
    interface Serializer extends Function<JsonObject, RFEHudOverlayRenderer> {
    }

}
