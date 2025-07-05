package rbasamoyai.ritchiesfirearmengine.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

/**
 * Copied from Forge ScrollPanel
 */
public abstract class ScrollPanel extends AbstractContainerEventHandler implements Renderable, NarratableEntry
{
    private final Minecraft client;
    protected final int width;
    protected final int height;
    protected final int top;
    protected final int bottom;
    protected final int right;
    protected final int left;
    private boolean scrolling;
    protected float scrollDistance;
    protected boolean captureMouse = true;
    protected final int border;

    private final int barWidth;
    private final int barLeft;
    private final int bgColorFrom;
    private final int bgColorTo;
    private final int barBgColor;
    private final int barColor;
    private final int barBorderColor;

    public ScrollPanel(Minecraft client, int width, int height, int top, int left) {
        this(client, width, height, top, left, 4);
    }

    public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border) {
        this(client, width, height, top, left, border, 6);
    }

    public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth) {
        this(client, width, height, top, left, border, barWidth, 0xC0101010, 0xD0101010);
    }

    public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColor) {
        this(client, width, height, top, left, border, barWidth, bgColor, bgColor);
    }

    public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColorFrom, int bgColorTo) {
        this(client, width, height, top, left, border, barWidth, bgColorFrom, bgColorTo, 0xFF000000, 0xFF808080, 0xFFC0C0C0);
    }

    public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int bgColorFrom,
                       int bgColorTo, int barBgColor, int barColor, int barBorderColor) {
        this.client = client;
        this.width = width;
        this.height = height;
        this.top = top;
        this.left = left;
        this.bottom = height + this.top;
        this.right = width + this.left;
        this.barLeft = this.left + this.width - barWidth;
        this.border = border;
        this.barWidth = barWidth;
        this.bgColorFrom = bgColorFrom;
        this.bgColorTo = bgColorTo;
        this.barBgColor = barBgColor;
        this.barColor = barColor;
        this.barBorderColor = barBorderColor;
    }

    protected abstract int getContentHeight();

    /**
     * Draws the background of the scroll panel. This runs AFTER Scissors are enabled.
     */
    protected void drawBackground(GuiGraphics guiGraphics, Tesselator tess, float partialTick) {
        BufferBuilder worldr = tess.getBuilder();

        if (this.client.level != null)
        {
            this.drawGradientRect(guiGraphics, this.left, this.top, this.right, this.bottom, bgColorFrom, bgColorTo);
        }
        else // Draw dark dirt background
        {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, Screen.BACKGROUND_LOCATION);
            final float texScale = 32.0F;
            worldr.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(this.left,  this.bottom, 0.0D).uv(this.left  / texScale, (this.bottom + (int)this.scrollDistance) / texScale).color(0x20, 0x20, 0x20, 0xFF).endVertex();
            worldr.vertex(this.right, this.bottom, 0.0D).uv(this.right / texScale, (this.bottom + (int)this.scrollDistance) / texScale).color(0x20, 0x20, 0x20, 0xFF).endVertex();
            worldr.vertex(this.right, this.top,    0.0D).uv(this.right / texScale, (this.top    + (int)this.scrollDistance) / texScale).color(0x20, 0x20, 0x20, 0xFF).endVertex();
            worldr.vertex(this.left,  this.top,    0.0D).uv(this.left  / texScale, (this.top    + (int)this.scrollDistance) / texScale).color(0x20, 0x20, 0x20, 0xFF).endVertex();
            tess.end();
        }
    }

    /**
     * Draw anything special on the screen. Scissor (RenderSystem.enableScissor) is enabled
     * for anything that is rendered outside the view box. Do not mess with Scissor unless you support this.
     */
    protected abstract void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY);

    protected boolean clickPanel(double mouseX, double mouseY, int button) { return false; }

    private int getMaxScroll() { return this.getContentHeight() - (this.height - this.border); }

    private void applyScrollLimits() {
        int max = this.getMaxScroll();
        if (max < 0)
            max /= 2;
        this.scrollDistance = Mth.clamp(this.scrollDistance, 0, max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll == 0)
            return false;
        this.scrollDistance += (float) (-scroll * this.getScrollAmount());
        this.applyScrollLimits();
        return true;
    }

    protected int getScrollAmount() { return 20; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.left && mouseX <= this.left + this.width && mouseY >= this.top && mouseY <= this.bottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        this.scrolling = button == 0 && mouseX >= barLeft && mouseX < barLeft + barWidth;
        if (this.scrolling)
            return true;
        int mouseListY = ((int)mouseY) - this.top - this.getContentHeight() + (int)this.scrollDistance - border;
        if (mouseX >= left && mouseX <= right && mouseListY < 0)
            return this.clickPanel(mouseX - left, mouseY - this.top + (int)this.scrollDistance - border, button);
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button))
            return true;
        boolean ret = this.scrolling;
        this.scrolling = false;
        return ret;
    }

    private int getBarHeight() {
        return Mth.clamp((this.height * this.height) / this.getContentHeight(), 32, this.height - this.border * 2);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.scrolling)
            return false;
        int maxScroll = this.height - this.getBarHeight();
        double moved = deltaY / maxScroll;
        this.scrollDistance += (float) (this.getMaxScroll() * moved);
        this.applyScrollLimits();
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder worldr = tess.getBuilder();

        double scale = this.client.getWindow().getGuiScale();
        RenderSystem.enableScissor((int)(this.left * scale), (int)(this.client.getWindow().getHeight() - (this.bottom * scale)),
                                   (int)(this.width * scale), (int)(this.height * scale));

        this.drawBackground(guiGraphics, tess, partialTick);

        int baseY = this.top + this.border - (int) this.scrollDistance;
        this.drawPanel(guiGraphics, this.right, baseY, tess, mouseX, mouseY);

        RenderSystem.disableDepthTest();

        int extraHeight = (this.getContentHeight() + this.border) - this.height;
        if (extraHeight > 0) {
            int barHeight = getBarHeight();
            int barTop = Math.max((int) this.scrollDistance * (this.height - barHeight) / extraHeight + this.top, this.top);

            int barBgAlpha = this.barBgColor >> 24 & 0xff;
            int barBgRed   = this.barBgColor >> 16 & 0xff;
            int barBgGreen = this.barBgColor >>  8 & 0xff;
            int barBgBlue  = this.barBgColor       & 0xff;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            worldr.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            worldr.vertex(this.barLeft,                 this.bottom, 0.0D).color(barBgRed, barBgGreen, barBgBlue, barBgAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, this.bottom, 0.0D).color(barBgRed, barBgGreen, barBgBlue, barBgAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, this.top,    0.0D).color(barBgRed, barBgGreen, barBgBlue, barBgAlpha).endVertex();
            worldr.vertex(this.barLeft,                 this.top,    0.0D).color(barBgRed, barBgGreen, barBgBlue, barBgAlpha).endVertex();
            tess.end();

            int barAlpha = this.barColor >> 24 & 0xff;
            int barRed   = this.barColor >> 16 & 0xff;
            int barGreen = this.barColor >>  8 & 0xff;
            int barBlue  = this.barColor       & 0xff;

            worldr.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            worldr.vertex(this.barLeft,                 barTop + barHeight, 0.0D).color(barRed, barGreen, barBlue, barAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, barTop + barHeight, 0.0D).color(barRed, barGreen, barBlue, barAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth, barTop,             0.0D).color(barRed, barGreen, barBlue, barAlpha).endVertex();
            worldr.vertex(this.barLeft,                 barTop,             0.0D).color(barRed, barGreen, barBlue, barAlpha).endVertex();
            tess.end();

            int barBorderAlpha = this.barBorderColor >> 24 & 0xff;
            int barBorderRed   = this.barBorderColor >> 16 & 0xff;
            int barBorderGreen = this.barBorderColor >>  8 & 0xff;
            int barBorderBlue  = this.barBorderColor       & 0xff;

            worldr.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            worldr.vertex(this.barLeft,                     barTop + barHeight - 1, 0.0D).color(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth - 1, barTop + barHeight - 1, 0.0D).color(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha).endVertex();
            worldr.vertex(this.barLeft + this.barWidth - 1, barTop,                 0.0D).color(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha).endVertex();
            worldr.vertex(this.barLeft,                     barTop,                 0.0D).color(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha).endVertex();
            tess.end();
        }

        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
    }

    protected void drawGradientRect(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color1, int color2) {
        guiGraphics.fillGradient(left, top, right, bottom, color1, color2);
    }

    @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }

}
