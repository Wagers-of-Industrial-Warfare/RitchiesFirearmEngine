package rbasamoyai.ritchiesfirearmengine.foundation.pack_compatibility;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import rbasamoyai.ritchiesfirearmengine.foundation.gui.ScrollPanel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapted from Forge's mod mismatch screen
 */
public class RFEPackMismatchDisconnectScreen extends Screen {

    private final Screen parent;
    private final Component reason;
    private final Map<String, Tuple<String, String>> mismatchedVersions;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private int textHeight;
    private final Path packDir;
    private final Path logFile;
    private final int listHeight = 140;

    public RFEPackMismatchDisconnectScreen(Screen parent, Component title, Component reason, Map<String, Tuple<String, String>> mismatchedVersions) {
        super(title);
        this.parent = parent;
        this.reason = reason;
        this.packDir = Path.of(".", "rfe_packs").normalize().toAbsolutePath();
        this.logFile = Path.of(".").normalize().toAbsolutePath().resolve(Paths.get("logs","latest.log"));
        this.mismatchedVersions = mismatchedVersions;
    }

    @Override
    protected void init() {
        this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
        this.textHeight = this.message.getLineCount() * 9;

        int listLeft = Math.max(8, this.width / 2 - 220);
        int listWidth = Math.min(440, this.width - 16);
        int upperButtonHeight = Math.min((this.height + this.listHeight + this.textHeight) / 2 + 10, this.height - 50);
        int lowerButtonHeight = Math.min((this.height + this.listHeight + this.textHeight) / 2 + 35, this.height - 25);
        this.addRenderableWidget(new MismatchInfoPanel(this.minecraft, listWidth, this.listHeight, (this.height - this.listHeight) / 2, listLeft));

        int buttonWidth = Math.min(210, this.width / 2 - 20);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ritchiesfirearmengine.pack_mismatch.open.log", this.logFile.getFileName()),
                        button -> Util.getPlatform().openFile(this.logFile.toFile()))
                .bounds(Math.max(this.width / 4 - buttonWidth / 2, listLeft), upperButtonHeight, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ritchiesfirearmengine.pack_mismatch.open.packs"),
                        button -> Util.getPlatform().openFile(this.packDir.toFile()))
                .bounds(Math.min(this.width * 3 / 4 - buttonWidth / 2, listLeft + listWidth - buttonWidth), upperButtonHeight, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.toMenu"), button -> this.minecraft.setScreen(this.parent))
                .bounds((this.width - buttonWidth) / 2, lowerButtonHeight, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, (this.height - this.listHeight - this.textHeight) / 2 - 36, 0xAAAAAA);
        this.message.renderCentered(guiGraphics, this.width / 2, (this.height - this.listHeight - this.textHeight) / 2 - 18);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    class MismatchInfoPanel extends ScrollPanel {
        private final List<Tuple<FormattedCharSequence, Tuple<FormattedCharSequence, FormattedCharSequence>>> lineTable;
        private final int contentSize;
        private final int nameIndent = 10;
        private final int tableWidth = this.width - this.border * 2 - 6 - this.nameIndent;
        private final int nameWidth = this.tableWidth * 3 / 5;
        private final int versionWidth = (this.tableWidth - this.nameWidth) / 2;

        public MismatchInfoPanel(Minecraft client, int width, int height, int top, int left) {
            super(client, width, height, top, left);

            List<Tuple<MutableComponent, Tuple<String, String>>> rawTable = new ArrayList<>();
            // Header
            rawTable.add(new Tuple<>(Component.translatable("gui.ritchiesfirearmengine.pack_mismatch.pack.header").withStyle(ChatFormatting.GRAY), null));
            // Columns
            rawTable.add(new Tuple<>(Component.translatable("gui.ritchiesfirearmengine.pack_mismatch.pack.name").withStyle(ChatFormatting.UNDERLINE),
                    new Tuple<>(I18n.get("gui.ritchiesfirearmengine.pack_mismatch.pack.client"), I18n.get("gui.ritchiesfirearmengine.pack_mismatch.pack.server"))));
            int i = 0;
            for (Map.Entry<String, Tuple<String, String>> mismatchEntry : RFEPackMismatchDisconnectScreen.this.mismatchedVersions.entrySet()) {
                String packName = mismatchEntry.getKey();
                Tuple<String, String> versions = mismatchEntry.getValue();
                rawTable.add(new Tuple<>(this.toPackLineComponent(packName, i),
                        new Tuple<>(versions.getA() == null ? "(missing)" : versions.getA(), versions.getB() == null ? "(missing)" : versions.getB())));
                if (++i >= 20) {
                    rawTable.add(new Tuple<>(Component.translatable("gui.ritchiesfirearmengine.pack_mismatch.pack.additional",
                                    RFEPackMismatchDisconnectScreen.this.mismatchedVersions.size() - i).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), null));
                    break;
                }
            }
            rawTable.add(new Tuple<>(Component.literal(" "), null)); //padding
            this.lineTable = rawTable.stream().flatMap(p -> this.splitLineToWidth(p.getA(), p.getB()).stream()).collect(Collectors.toList());
            this.contentSize = this.lineTable.size();
        }

        private List<Tuple<FormattedCharSequence, Tuple<FormattedCharSequence, FormattedCharSequence>>> splitLineToWidth(
                MutableComponent name, Tuple<String, String> versions) {
            Style style = name.getStyle();
            int versionColumns = versions == null ? 0 : (versions.getA().isEmpty() ? (versions.getB().isEmpty() ? 0 : 1) : 2);
            //the name width may be expanded when the version column string is missing
            int adaptedNameWidth = this.nameWidth + this.versionWidth * (2 - versionColumns) - 4;
            List<FormattedCharSequence> nameLines = RFEPackMismatchDisconnectScreen.this.font.split(name, adaptedNameWidth);
            List<FormattedCharSequence> clientVersionLines = RFEPackMismatchDisconnectScreen.this.font.split(
                    Component.literal(versions != null ? versions.getA() : "").setStyle(style), this.versionWidth - 4);
            List<FormattedCharSequence> serverVersionLines = RFEPackMismatchDisconnectScreen.this.font.split(
                    Component.literal(versions != null ? versions.getB() : "").setStyle(style), this.versionWidth - 4);
            List<Tuple<FormattedCharSequence, Tuple<FormattedCharSequence, FormattedCharSequence>>> splitLines = new ArrayList<>();
            int rowsOccupied = Math.max(nameLines.size(), Math.max(clientVersionLines.size(), serverVersionLines.size()));
            for (int i = 0; i < rowsOccupied; i++)
                splitLines.add(new Tuple<>(i < nameLines.size() ? nameLines.get(i) : FormattedCharSequence.EMPTY,
                        versions == null ? null : new Tuple<>(i < clientVersionLines.size() ? clientVersionLines.get(i)
                                : FormattedCharSequence.EMPTY, i < serverVersionLines.size() ? serverVersionLines.get(i) : FormattedCharSequence.EMPTY)));
            return splitLines;
        }

        private MutableComponent toPackLineComponent(String packId, int color) {
            return Component.literal(packId).withStyle(color % 2 == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        }

        @Override
        protected int getContentHeight() {
            return Math.max(this.contentSize * (RFEPackMismatchDisconnectScreen.this.font.lineHeight + 3), this.border - this.top - 4);
        }

        @Override
        protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
            int i = 0;

            for (Tuple<FormattedCharSequence, Tuple<FormattedCharSequence, FormattedCharSequence>> line : this.lineTable) {
                FormattedCharSequence name = line.getA();
                Tuple<FormattedCharSequence, FormattedCharSequence> versions = line.getB();
                //Since font#draw does not respect the color of the given component, we have to read it out here and then use it as the last parameter
                int color = Optional.ofNullable(RFEPackMismatchDisconnectScreen.this.font.getSplitter().componentStyleAtWidth(name, 0))
                        .map(Style::getColor).map(TextColor::getValue).orElse(0xFFFFFF);
                //Only indent the given name if a version string is present. This makes it easier to distinguish table section headers and mod entries
                int nameLeft = this.left + this.border + (versions == null ? 0 : this.nameIndent);
                guiGraphics.drawString(RFEPackMismatchDisconnectScreen.this.font, name, nameLeft, relativeY + i * 12, color, false);
                if (versions != null) {
                    guiGraphics.drawString(RFEPackMismatchDisconnectScreen.this.font, versions.getA(),
                            this.left + this.border + this.nameIndent + this.nameWidth, relativeY + i * 12, color, false);
                    guiGraphics.drawString(RFEPackMismatchDisconnectScreen.this.font, versions.getB(),
                            this.left + this.border + this.nameIndent + this.nameWidth + this.versionWidth, relativeY + i * 12, color, false);
                }
                i++;
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            Style style = this.getComponentStyleAt(mouseX, mouseY);
            if (style != null && style.getHoverEvent() != null)
                guiGraphics.renderComponentHoverEffect(RFEPackMismatchDisconnectScreen.this.font, style, mouseX, mouseY);
        }

        public Style getComponentStyleAt(double x, double y) {
            if (!this.isMouseOver(x, y))
                return null;
            double relativeY = y - this.top + this.scrollDistance - this.border;
            int slotIndex = (int)(relativeY + (this.border / 2d)) / 12;
            if (slotIndex >= this.contentSize)
                return null;
            //The relative x needs to take the potentially missing indent of the row into account. It does that by checking if the line has a version associated to it
            double relativeX = x - this.left - this.border - (this.lineTable.get(slotIndex).getB() == null ? 0 : this.nameIndent);
            return relativeX >= 0 ? RFEPackMismatchDisconnectScreen.this.font.getSplitter().componentStyleAtWidth(this.lineTable.get(slotIndex).getA(), (int)relativeX) : null;
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            Style style = this.getComponentStyleAt(mouseX, mouseY);
            if (style == null)
                return super.mouseClicked(mouseX, mouseY, button);
            RFEPackMismatchDisconnectScreen.this.handleComponentClicked(style);
            return true;
        }

        @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
        @Override public void updateNarration(NarrationElementOutput output) {}
    }

}
