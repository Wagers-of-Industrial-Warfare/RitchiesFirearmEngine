package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
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
    private final boolean hideOverheating;
    private final boolean hideFirearmAmmoCount;
    private final boolean enlargeFirearmAmmoCount;
    private final boolean showInventoryCount;
    private final boolean countLooseRounds;
    @Nullable private final RFEHudIcon firearmIcon;
    private final Map<String, String> modeTranslations;

    public AmmoCounterHUDOverlayRenderer(boolean hideEntireAmmoCount, boolean hideOverheating, boolean hideFirearmAmmoCount, boolean enlargeFirearmAmmoCount,
                                         boolean showInventoryCount, boolean countLooseRounds, @Nullable RFEHudIcon firearmIcon,
                                         Map<String, String> modeTranslations) {
        this.minecraft = Minecraft.getInstance();
        this.font = this.minecraft.font;

        this.hideEntireAmmoCount = hideEntireAmmoCount;
        this.hideOverheating = hideOverheating;
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
        if (!this.hideOverheating) {
            float heat = RFEHudItemInfoProviders.getHeatFromItem(item).orElse(0f);
            float heatCapacity = RFEHudItemInfoProviders.getHeatCapacityFromItem(item).orElse(0f);
            boolean overheated = heat >= heatCapacity;
            float percentage = heat / heatCapacity;
            int rgb = Mth.hsvToRgb((1f - percentage) / 7.5f, 0.75f * Mth.clamp(percentage * 10, 0f, 1f), 1.0f);
            int r = rgb >> 16 & 255;
            int g = rgb >> 8 & 255;
            int b = rgb & 255;
            if (heat > 0) {
                if (this.firearmIcon != null) {
                    var x = originX - this.firearmIcon.blitWidth() / 2;
                    var y = originY - this.firearmIcon.blitHeight() - 4;
                    var totalWidth = this.firearmIcon.blitWidth();
                    int targetWidth = (int) (totalWidth * percentage);
                    RenderSystem.enableBlend();
                    graphics.setColor(r / 255f, g / 255f, b / 255f, 1f);
                    graphics.blit(this.firearmIcon.texture(), x + (totalWidth - targetWidth), y,
                            targetWidth, this.firearmIcon.blitHeight(),
                            this.firearmIcon.uOffset() - targetWidth, this.firearmIcon.vOffset(),
                            targetWidth, this.firearmIcon.blitHeight(),
                            this.firearmIcon.texWidth(), this.firearmIcon.texHeight()
                    );
                    graphics.setColor(1f, 1f, 1f, 1f);
                    RenderSystem.disableBlend();
                }
                Component modeText = overheated ? Component.translatable("gui.ritchiesfirearmengine.heat.overheated")
                        : Component.translatable("gui.ritchiesfirearmengine.heat", String.format("%.1f", percentage * 100f));
                int textWidth = this.font.width(modeText);
                graphics.drawString(this.font, modeText, originX - textWidth / 2, originY + this.font.lineHeight * 3 + 2, rgb, true);
            }
        }
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
        return itemStack.getItem() instanceof RFEFirearmItem firearm ? firearm.getCurrentMode(itemStack).getDisplayId() : "";
    }

    public static class Serializer implements RFEHudOverlayRenderer.Serializer {
        @Override
        public RFEHudOverlayRenderer apply(JsonObject obj) {
            boolean hideEntireAmmoCount = GsonHelper.getAsBoolean(obj, "hide_entire_ammo_count", false);
            boolean hideOverheating = GsonHelper.getAsBoolean(obj, "hide_overheating", false);
            boolean hideFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "hide_firearm_ammo", false);
            boolean enlargeFirearmAmmoCount = GsonHelper.getAsBoolean(obj, "enlarge_firearm_ammo", true);
            boolean showInventoryCount = GsonHelper.getAsBoolean(obj, "show_inventory_ammo", true);
            boolean countLooseRounds = GsonHelper.getAsBoolean(obj, "count_loose_rounds", true);

            RFEHudIcon firearmIcon = null;
            if (GsonHelper.isObjectNode(obj, "firearm_icon"))
                firearmIcon = RFEHudIcon.fromJson(GsonHelper.getAsJsonObject(obj, "firearm_icon"));

            Map<String, String> modeNames = new HashMap<>();
            if (GsonHelper.isObjectNode(obj, "mode_translations")) {
                JsonObject modeNamesObj = GsonHelper.getAsJsonObject(obj, "mode_translations");
                for (Map.Entry<String, JsonElement> entry : modeNamesObj.entrySet())
                    modeNames.put(entry.getKey(), entry.getValue().getAsString());
            } else {
                // throw new IllegalStateException("Missing translations for modes in ammo counter");
            }

            return new AmmoCounterHUDOverlayRenderer(hideEntireAmmoCount, hideOverheating, hideFirearmAmmoCount, enlargeFirearmAmmoCount,
                    showInventoryCount, countLooseRounds, firearmIcon, modeNames);
        }
    }

}
