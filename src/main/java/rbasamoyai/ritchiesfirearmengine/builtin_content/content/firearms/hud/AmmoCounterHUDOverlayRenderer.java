package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.RFEFirearmItem;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudItemInfoProviders;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.elements.RFEHudIcon;
import rbasamoyai.ritchiesfirearmengine.utils.RFEItemUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmmoCounterHUDOverlayRenderer implements RFEHudOverlayRenderer {

    protected final Minecraft minecraft;
    protected final Font font;
    private final boolean hideEntireAmmoCount;
    private final boolean hideFirearmAmmoCount;
    private final boolean enlargeFirearmAmmoCount;
    private final boolean showInventoryCount;
    private final boolean countLooseRounds;
    @Nullable private final RFEHudIcon firearmIcon;
    private final Map<String, String> modeTranslations;

    public AmmoCounterHUDOverlayRenderer(boolean hideEntireAmmoCount, boolean hideFirearmAmmoCount, boolean enlargeFirearmAmmoCount,
                                         boolean showInventoryCount, boolean countLooseRounds, @Nullable RFEHudIcon firearmIcon,
                                         Map<String, String> modeTranslations) {
        this.minecraft = Minecraft.getInstance();
        this.font = this.minecraft.font;

        this.hideEntireAmmoCount = hideEntireAmmoCount;
        this.hideFirearmAmmoCount = hideFirearmAmmoCount;
        this.enlargeFirearmAmmoCount = enlargeFirearmAmmoCount;
        this.showInventoryCount = showInventoryCount;
        this.countLooseRounds = countLooseRounds;
        this.firearmIcon = firearmIcon;
        this.modeTranslations = modeTranslations;
    }

    @Override
    public void renderHUD(GuiGraphics graphics, float partialTicks, ItemStack item, Player player, boolean offhand) {
        // TODO offhand
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        PoseStack poseStack = graphics.pose();

        int originX = width - 100;
        int originY = height - 80;

        if (!this.hideEntireAmmoCount) {
            List<ItemStack> ammoList = RFEHudItemInfoProviders.getAmmoStacksFromItem(item);
            if (ammoList != null) { // if firearm item does not have infinite ammo
                String countText = this.hideFirearmAmmoCount ? "_" : Integer.toString(RFEItemUtils.countItems(ammoList));
                int textWidth = this.font.width(countText);

                if (this.enlargeFirearmAmmoCount) {
                    poseStack.pushPose();
                    poseStack.scale(2, 2, 1);
                    graphics.drawString(this.font, countText, (originX - textWidth * 2) / 2 - 2, originY / 2, 0xFFFFFF, true);
                    poseStack.popPose();
                } else {
                    countText += " ";
                    textWidth = this.font.width(countText);
                    graphics.drawString(this.font, countText, originX - textWidth, originY, 0xFFFFFF, true);
                }
                if (this.showInventoryCount) {
                    int ammoCount = RFEHudItemInfoProviders.getAmmoInventoryCount(item, player, this.countLooseRounds);
                    String inventoryCountText = "/ " + (ammoCount < 0 ? "∞" : Math.min(ammoCount, 9999));
                    graphics.drawString(this.font, inventoryCountText, originX, originY, 0xFFFFFF, true);
                }
            } else {
                int textWidth = this.font.width("∞");
                if (this.enlargeFirearmAmmoCount) {
                    poseStack.pushPose();
                    poseStack.scale(2, 2, 1);
                    graphics.drawString(this.font, "∞", (originX - textWidth) / 2, originY / 2, 0xFFFFFF, true);
                    poseStack.popPose();
                } else {
                    graphics.drawString(this.font, "∞", originX - textWidth / 2, originY, 0xFFFFFF, true);
                }
            }
        }
        if (this.firearmIcon != null) {
            this.firearmIcon.blit(graphics, originX - this.firearmIcon.blitWidth() / 2, originY - this.firearmIcon.blitHeight() - 4);
        }
        // TODO overheating
        // TODO secondary ammo
        String modeName = this.getModeName(item);
        if (this.modeTranslations.containsKey(modeName)) {
            String modeKey = this.modeTranslations.get(modeName);
            Component modeText = Component.translatable(modeKey);
            int textWidth = this.font.width(modeText);
            graphics.drawString(this.font, modeText, originX - textWidth / 2, originY + this.font.lineHeight * 2 + 2, 0xFFFFFF, true);
        }
    }

    protected String getModeName(ItemStack itemStack) {
        return itemStack.getItem() instanceof RFEFirearmItem firearm ? firearm.getCurrentMode(itemStack).getModeId() : "";
    }

    public static class Serializer implements RFEHudOverlayRenderer.Serializer {
        @Override
        public RFEHudOverlayRenderer apply(JsonObject obj) {
            boolean hideEntireAmmoCount = GsonHelper.getAsBoolean(obj, "hide_entire_ammo_count", false);
            boolean hideFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "hide_firearm_ammo", false);
            boolean enlargeFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "enlarge_firearm_ammo", true);
            boolean showInventoryCount = GsonHelper.getAsBoolean(obj, "show_inventory_ammo", true);
            boolean countLooseRounds = GsonHelper.getAsBoolean(obj, "count_loose_rounds", true);

            RFEHudIcon firearmIcon = null;
            if (GsonHelper.isObjectNode(obj, "firearm_icon"))
                firearmIcon = RFEHudIcon.fromJson(GsonHelper.getAsJsonObject(obj, "firearm_icon"));

            if (!GsonHelper.isObjectNode(obj, "mode_translations"))
                throw new IllegalStateException("Missing translations for modes in ammo counter");
            JsonObject modeNamesObj = GsonHelper.getAsJsonObject(obj, "mode_translations");
            Map<String, String> modeNames = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : modeNamesObj.entrySet())
                modeNames.put(entry.getKey(), entry.getValue().getAsString());

            return new AmmoCounterHUDOverlayRenderer(hideEntireAmmoCount, hideFirearmAmmoCount, enlargeFirearmAmmoCount,
                    showInventoryCount, countLooseRounds, firearmIcon, modeNames);
        }
    }

}
