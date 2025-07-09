package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudItemInfoProviders;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.elements.RFEHudIcon;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.List;

public class AmmoCounterHUDOverlayRenderer implements RFEHudOverlayRenderer {

    protected final Minecraft minecraft;
    protected final Font font;
    private final boolean hideFirearmAmmoCount;
    private final boolean enlargeFirearmAmmoCount;
    private final boolean showInventoryCount;
    @Nullable private final RFEHudIcon firearmIcon;

    public AmmoCounterHUDOverlayRenderer(boolean hideFirearmAmmoCount, boolean enlargeFirearmAmmoCount, boolean showInventoryCount,
                                         @Nullable RFEHudIcon firearmIcon) {
        this.minecraft = Minecraft.getInstance();
        this.font = this.minecraft.font;

        this.hideFirearmAmmoCount = hideFirearmAmmoCount;
        this.enlargeFirearmAmmoCount = enlargeFirearmAmmoCount;
        this.showInventoryCount = showInventoryCount;
        this.firearmIcon = firearmIcon;
    }

    @Override
    public void renderHUD(GuiGraphics graphics, float partialTicks, ItemStack item, Player player, boolean offhand) {
        // TODO offhand
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        PoseStack poseStack = graphics.pose();

        int originX = width - 100;
        int originY = height - 80;

        List<ItemStack> ammoList = RFEHudItemInfoProviders.getAmmoStacksFromItem(item);
        if (ammoList != null) { // if firearm item does not have infinite ammo
            String countText = this.hideFirearmAmmoCount ? "_" : Integer.toString(RFEItemUtils.countItems(ammoList));
            int textWidth = this.font.width(countText);

            if (this.enlargeFirearmAmmoCount) {
                poseStack.pushPose();
                poseStack.scale(2, 2, 2);
                graphics.drawString(this.font, countText, (originX - textWidth * 2) / 2 - 2, originY / 2, 0xFFFFFF, true);
                poseStack.popPose();
            } else {
                countText += " ";
                textWidth = this.font.width(countText);
                graphics.drawString(this.font, countText, originX - textWidth, originY, 0xFFFFFF, true);
            }
            if (this.showInventoryCount) {
                int ammoCount = RFEHudItemInfoProviders.getAmmoInventoryCount(item, player);
                String inventoryCountText = "/ " + Math.min(ammoCount, 9999);
                graphics.drawString(this.font, inventoryCountText, originX, originY, 0xFFFFFF, true);
            }
        }
        if (this.firearmIcon != null) {
            this.firearmIcon.blit(graphics, originX - this.firearmIcon.blitWidth() / 2, originY - this.firearmIcon.blitHeight() - 4);
        }
        // TODO overheating
        // TODO infinite ammo
    }

    public static class Serializer implements RFEHudOverlayRenderer.Serializer {
        @Override
        public RFEHudOverlayRenderer apply(JsonObject obj) {
            boolean hideFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "hide_firearm_ammo", false);
            boolean enlargeFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "enlarge_firearm_ammo", true);
            boolean showInventoryCount = GsonHelper.getAsBoolean(obj, "show_inventory_ammo", true);

            RFEHudIcon firearmIcon = null;
            if (GsonHelper.isObjectNode(obj, "firearm_icon"))
                firearmIcon = RFEHudIcon.fromJson(GsonHelper.getAsJsonObject(obj, "firearm_icon"));

            return new AmmoCounterHUDOverlayRenderer(hideFirearmAmmoCount, enlargeFirearmAmmoCount, showInventoryCount,
                    firearmIcon);
        }
    }

}
