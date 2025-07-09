package rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.elements;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

public record RFEHudIcon(ResourceLocation texture, int blitWidth, int blitHeight, int uOffset, int vOffset, int uWidth,
                         int vHeight, int texWidth, int texHeight) {

    public void blit(GuiGraphics graphics, int x, int y) {
        graphics.blit(this.texture, x, y, this.blitWidth, this.blitHeight, this.uOffset, this.vOffset, this.blitWidth, this.blitHeight, this.texWidth, this.texHeight);
    }

    public void blit(GuiGraphics graphics, int x, int y, int zOffset) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, zOffset);
        this.blit(graphics, x, y);
        poseStack.popPose();
    }

    public static RFEHudIcon fromJson(JsonObject obj) {
        ResourceLocation texture = RFEUtils.location(GsonHelper.getAsString(obj, "texture"));
        int width = Math.max(GsonHelper.getAsInt(obj, "width"), 0);
        int height = Math.max(GsonHelper.getAsInt(obj, "height"), 0);
        int blitWidth = Math.max(GsonHelper.getAsInt(obj, "blit_width", width), 0);
        int blitHeight = Math.max(GsonHelper.getAsInt(obj, "blit_height", height), 0);
        int uOffset = Math.max(GsonHelper.getAsInt(obj, "u_offset", 0), 0);
        int vOffset = Math.max(GsonHelper.getAsInt(obj, "v_offset", 0), 0);
        int uWidth = Math.max(GsonHelper.getAsInt(obj, "u_width", width), 0);
        int vHeight = Math.max(GsonHelper.getAsInt(obj, "v_height", height), 0);
        int texWidth = Math.max(GsonHelper.getAsInt(obj, "texture_width", width), 0);
        int texHeight = Math.max(GsonHelper.getAsInt(obj, "texture_height", height), 0);
        return new RFEHudIcon(texture, blitWidth, blitHeight, uOffset, vOffset, uWidth, vHeight, texWidth, texHeight);
    }

}
