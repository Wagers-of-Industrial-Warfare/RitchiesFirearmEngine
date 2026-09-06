package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.SlotConfig;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;

public class RFEIntegralAttachmentButton extends Button {

    private static final WidgetSprites SPRITES = new WidgetSprites(
            RitchiesFirearmEngine.resource("widget/integral_slot_enabled"),
            RitchiesFirearmEngine.resource("widget/integral_slot_disabled"),
            RitchiesFirearmEngine.resource("widget/integral_slot_enabled_highlighted"),
            RitchiesFirearmEngine.resource("widget/integral_slot_disabled_highlighted"));

    protected final SlotConfig slotConfig;
    protected final SlotEnabled slotEnabled;
    protected final SlotScrolled slotScrolled;
    protected final ResourceLocation slotId;
    protected final Supplier<ItemStack> parentStack;

    protected RFEIntegralAttachmentButton(int x, int y, Component message, OnPress onPress, CreateNarration createNarration,
                                          SlotConfig slotConfig, SlotEnabled slotEnabled, SlotScrolled slotScrolled,
                                          ResourceLocation slotId, Supplier<ItemStack> parentStack) {
        super(x, y, 18, 18, message, onPress, createNarration); // Same dimensions as item slot
        this.slotConfig = slotConfig;
        this.slotEnabled = slotEnabled;
        this.slotScrolled = slotScrolled;
        this.slotId = slotId;
        this.parentStack = parentStack;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean active = this.isActive();
        ResourceLocation buttonSprite = SPRITES.get(this.slotEnabled.get() && active, this.isHoveredOrFocused() && active);
        guiGraphics.blitSprite(buttonSprite, this.getX(), this.getY(), this.width, this.height);
        if (this.slotConfig.emptyIcon() != null) {
            TextureAtlasSprite iconSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(this.slotConfig.emptyIcon());
            guiGraphics.blit(this.getX() + 1, this.getY() + 1, 0, this.width - 2, this.height - 2, iconSprite);
        }
    }

    @Override
    public void onPress() {
        if (!this.slotConfig.disabled() && (this.getBlockingSlots(this.parentStack.get()).isEmpty() || this.slotEnabled.get()))
            super.onPress();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return !this.slotConfig.disabled() && this.slotScrolled.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return null;
    }

    public ResourceLocation getSlotId() { return this.slotId; }

    public static Builder builder(Component message, OnPress onPress, SlotConfig slotConfig, SlotEnabled slotEnabled,
                                  SlotScrolled slotScrolled, ResourceLocation slotId, Supplier<ItemStack> parentStack) {
        return new Builder(message, onPress, slotConfig, slotEnabled, slotScrolled, slotId, parentStack);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (this.slotConfig.disabled() || (!this.getBlockingSlots(this.parentStack.get()).isEmpty() && !this.slotEnabled.get()))
            return;
        // playDownSound is called before onPress, so inverting is a prediction of the next state
        boolean enabling = !this.slotEnabled.get();
        if (enabling && this.slotConfig.soundOnAdd() != null) {
            handler.play(SimpleSoundInstance.forUI(this.slotConfig.soundOnAdd(), 1.0f, 1.0f));
        } else if (!enabling && this.slotConfig.soundOnRemove() != null) {
            handler.play(SimpleSoundInstance.forUI(this.slotConfig.soundOnRemove(), 1.0f, 1.0f));
        }
    }

    @Nullable public String getSlotKey() { return this.slotConfig.emptyText(); }

    public Set<ResourceLocation> getBlockingSlots(ItemStack parentStack) {
        return parentStack.getItem() instanceof IHasRFEItemAttachments hasAttachments ?
                hasAttachments.getBlockingSlots(parentStack, this.slotId) : Set.of();
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private final SlotConfig slotConfig;
        private final SlotEnabled slotEnabled;
        private final SlotScrolled slotScrolled;
        private final ResourceLocation slotId;
        private final Supplier<ItemStack> parentStack;
        private int x;
        private int y;
        private CreateNarration createNarration = Button.DEFAULT_NARRATION;

        public Builder(Component message, OnPress onPress, SlotConfig slotConfig, SlotEnabled slotEnabled,
                       SlotScrolled slotScrolled, ResourceLocation slotId, Supplier<ItemStack> parentStack) {
            this.message = message;
            this.onPress = onPress;
            this.slotConfig = slotConfig;
            this.slotEnabled = slotEnabled;
            this.slotScrolled = slotScrolled;
            this.slotId = slotId;
            this.parentStack = parentStack;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder createNarration(CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public RFEIntegralAttachmentButton build() {
            return new RFEIntegralAttachmentButton(this.x, this.y, this.message, this.onPress, this.createNarration,
                    this.slotConfig, this.slotEnabled, this.slotScrolled, this.slotId, this.parentStack);
        }
    }

    @FunctionalInterface
    public interface SlotEnabled extends Supplier<Boolean> {
    }

    @FunctionalInterface
    public interface SlotScrolled {
        boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);
    }

}
