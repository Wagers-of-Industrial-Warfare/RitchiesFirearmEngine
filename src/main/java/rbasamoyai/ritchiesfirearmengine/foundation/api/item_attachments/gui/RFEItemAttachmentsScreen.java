package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.logic.FirearmDataUtils;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.IHasRFEItemAttachments;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.MenuTypeSlotsConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsMenuSlotsHandler.SlotConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsScreenDisplayHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsScreenDisplayHandler.MenuTypeDisplayConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui.config.RFEItemAttachmentsScreenDisplayHandler.SlotDisplayConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentMenuOptionsText;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentTooltipContext;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundUpdateAttachmentOptionPacket;

import javax.annotation.Nullable;
import java.util.*;

public class RFEItemAttachmentsScreen extends AbstractContainerScreen<RFEItemAttachmentsMenu> {

    private static final ResourceLocation MENU_TEXTURE = RitchiesFirearmEngine.resource("textures/gui/field_attachments_menu.png");

    protected float angleX = 15;
    protected float angleY = -135;
    protected float itemScale = 1;

    protected float baseScale = 64;
    protected Map<ResourceLocation, SlotDisplayConfig> slotDisplayMap = new HashMap<>();
    protected List<RFEIntegralAttachmentButton> integralSlotButtons = new ArrayList<>();
    @Nullable protected RFEIntegralAttachmentButton hoveredIntegralSlot = null;

    protected final List<Rect2i> extraAreas = new ArrayList<>();

    public RFEItemAttachmentsScreen(RFEItemAttachmentsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 200;
        this.titleLabelY = -8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        ItemStack targetStack = this.menu.getFocusedAttachmentsItem();
        MenuTypeDisplayConfig screenConfig = RFEItemAttachmentsScreenDisplayHandler.getConfig(targetStack, this.menu.getType());
        this.baseScale = screenConfig.modelScale();
        this.slotDisplayMap = screenConfig.slotDisplayConfig();
        this.integralSlotButtons.clear();

        this.extraAreas.clear();
        if (targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments) {
            MenuTypeSlotsConfig menuConfig = RFEItemAttachmentsMenuSlotsHandler.getConfig(targetStack, this.menu.getType());
            ImmutableMap<ResourceLocation, SlotConfig> slotConfigById = menuConfig.slotConfig();
            Set<ResourceLocation> itemSlots = hasAttachments.getAttachmentSlots(targetStack);
            Set<ResourceLocation> integralSlots = hasAttachments.getIntegralAttachmentSlots(targetStack);
            int i = 0;
            for (Map.Entry<ResourceLocation, SlotConfig> entry : slotConfigById.entrySet()) {
                if (itemSlots.contains(entry.getKey()))
                    ++i;
            }
            for (Map.Entry<ResourceLocation, SlotConfig> entry : slotConfigById.entrySet()) {
                ResourceLocation slotId = entry.getKey();
                if (!integralSlots.contains(slotId))
                    continue;
                int x = this.leftPos + i / 5 * -18 - 18;
                int y = this.topPos + i % 5 * 18;
                SlotConfig slotConfig = entry.getValue();
                Component message = slotConfig.emptyText() != null ? Component.translatable(slotConfig.emptyText()) : Component.empty();
                this.integralSlotButtons.add(
                        this.addRenderableWidget(RFEIntegralAttachmentButton.builder(message,
                                        this::onPressIntegralSlotButton,
                                        slotConfig,
                                        () -> this.isIntegralSlotEnabled(slotId),
                                        (mouseX, mouseY, scrollX, scrollY) ->
                                                this.onScrollIntegralSlotButton(slotId, mouseX, mouseY, scrollX, scrollY),
                                        slotId, this.menu::getFocusedAttachmentsItem)
                        .pos(x, y).build()));
                ++i;
            }
            if (i > 0) {
                int cols = Mth.floorDiv(i, 5) + 2;
                this.extraAreas.add(new Rect2i(this.leftPos - 18 * cols, this.topPos - 18, 18 * cols, 108));
            }
        }
    }

    protected void onPressIntegralSlotButton(Button button) {
        if (!(button instanceof RFEIntegralAttachmentButton integralSlot))
            return;
        ItemStack targetStack = this.menu.getFocusedAttachmentsItem();
        ResourceLocation slotId = integralSlot.getSlotId();
        if (!integralSlot.getBlockingSlots(targetStack).isEmpty())
            return;
        if (!(targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments))
            return;
        RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getIntegralData(targetStack, slotId);
        if (attachmentProperties == null)
            return;
        DataComponentPatch data = hasAttachments.getIntegralAttachmentDataInSlot(targetStack, slotId);
        boolean removedToggle = !FirearmDataUtils.isAttachmentRemoved(data);
        hasAttachments.setIntegralAttachmentData(targetStack, slotId, FirearmDataUtils.setAttachmentRemoved(data, removedToggle));
        RFENetwork.sendToServer(ServerboundUpdateAttachmentOptionPacket.forIntegral(slotId,
                attachmentProperties.getIntegralAttachmentConfigOption(data), removedToggle));
    }

    protected boolean isIntegralSlotEnabled(ResourceLocation slotId) {
        ItemStack targetStack = this.menu.getFocusedAttachmentsItem();
        if (!(targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments))
            return false;
        RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getIntegralData(targetStack, slotId);
        if (attachmentProperties == null)
            return false;
        DataComponentPatch data = hasAttachments.getIntegralAttachmentDataInSlot(targetStack, slotId);
        return !FirearmDataUtils.isAttachmentRemoved(data);
    }

    protected boolean onScrollIntegralSlotButton(ResourceLocation slotId, double mouseX, double mouseY, double scrollX, double scrollY) {
        ItemStack targetStack = this.menu.getFocusedAttachmentsItem();
        if (!(targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments))
            return false;
        RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getIntegralData(targetStack, slotId);
        if (attachmentProperties == null)
            return false;
        DataComponentPatch data = hasAttachments.getIntegralAttachmentDataInSlot(targetStack, slotId);
        if (FirearmDataUtils.isAttachmentRemoved(data))
            return false; // Cannot modify removed integral attachments
        int attachmentOption = attachmentProperties.getIntegralAttachmentConfigOption(data);
        if (attachmentOption == -1)
            return false;
        int newOption = attachmentOption + (scrollY < 0 ? 1 : -1);
        Optional<DataComponentPatch> dataModOp = attachmentProperties.acceptIntegralAttachmentConfigOption(data, newOption);
        if (dataModOp.isEmpty())
            return false;
        hasAttachments.setIntegralAttachmentData(targetStack, slotId, dataModOp.get());
        RFENetwork.sendToServer(ServerboundUpdateAttachmentOptionPacket.forIntegral(slotId, newOption, false));
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 2f));
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int BACKGROUND_COLOR = RFEConfig.CLIENT.attachmentScreenBackgroundColor.getAsInt();
        guiGraphics.fillGradient(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight - 94, 0, 0x3F000000, 0x3F000000 | BACKGROUND_COLOR);
        guiGraphics.blit(MENU_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.menu.iterateAttachmentSlots(slot -> {
            int slotX = this.leftPos + slot.x;
            int slotY = this.topPos + slot.y;
            guiGraphics.blit(MENU_TEXTURE, slotX - 1, slotY - 1, this.imageWidth, 0, 18, 18);
        });
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredIntegralSlot = null;
        for (RFEIntegralAttachmentButton button : this.integralSlotButtons) {
            if (button.isHovered()) {
                this.hoveredIntegralSlot = button;
                break;
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);
        this.renderMainItem(guiGraphics, partialTick);
        this.renderSlotPointers(guiGraphics, partialTick);
        pose.popPose();
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        int slotX = slot.x;
        int slotY = slot.y;
        if (slot == this.menu.getTargetSlot()) {
            // Render attachment item focus highlight
            int ITEM_HIGHLIGHT_COLOR = RFEConfig.CLIENT.attachmentScreenItemColor.getAsInt();
            if (RFEConfig.CLIENT.attachmentScreenItemGradient.getAsBoolean()) {
                guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, ITEM_HIGHLIGHT_COLOR);
            } else {
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, ITEM_HIGHLIGHT_COLOR);
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        boolean hide = (this.hoveredSlot instanceof RFEItemAttachmentSlot || this.hoveredIntegralSlot != null) && Screen.hasControlDown();
        if (hide)
            return;
        super.renderTooltip(guiGraphics, x, y);
        if (!this.menu.getCarried().isEmpty())
            return;
        if (this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot && !this.hoveredSlot.hasItem()) {
            this.renderEmptyAttachmentSlotTooltip(guiGraphics, x, y, attachmentSlot);
        } else if (this.hoveredIntegralSlot != null) {
            this.renderIntegralAttachmentButtonTooltip(guiGraphics, x, y);
        }
    }

    protected void renderEmptyAttachmentSlotTooltip(GuiGraphics guiGraphics, int x, int y, RFEItemAttachmentSlot attachmentSlot) {
        List<Component> tooltip = new ArrayList<>();
        String text = attachmentSlot.getEmptyTextKey();
        if (text != null)
            tooltip.add(Component.translatable(text));
        this.addBlockedSlotsToTooltip(tooltip, attachmentSlot.getBlockingSlots());
        if (!tooltip.isEmpty())
            guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
    }

    protected void renderIntegralAttachmentButtonTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredIntegralSlot == null || !this.hoveredIntegralSlot.visible)
            return;
        List<Component> tooltip = new ArrayList<>();
        ItemStack targetStack = this.menu.getFocusedAttachmentsItem();
        ResourceLocation slotId = this.hoveredIntegralSlot.getSlotId();

        String slotKey = this.hoveredIntegralSlot.getSlotKey();
        if (slotKey != null)
            tooltip.add(Component.translatable(slotKey));

        Set<ResourceLocation> blockingSlots = this.hoveredIntegralSlot.getBlockingSlots(targetStack);
        this.addBlockedSlotsToTooltip(tooltip, blockingSlots);

        if (blockingSlots.isEmpty() && this.hoveredIntegralSlot.slotEnabled.get()) {
            RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getIntegralData(targetStack, slotId);
            if (attachmentProperties != null && targetStack.getItem() instanceof IHasRFEItemAttachments hasAttachments) {
                DataComponentPatch data = hasAttachments.getIntegralAttachmentDataInSlot(targetStack, slotId);
                Optional<AttachmentMenuOptionsText> op = attachmentProperties.getIntegralAttachmentConfigTextOptions(data);
                if (op.isPresent()) {
                    boolean currentlyModifying = Screen.hasShiftDown();
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("ritchiesfirearmengine.tooltip.item.hold_key.attachment_options",
                                    Component.translatable("ritchiesfirearmengine.tooltip.key_shift")
                                            .withStyle(currentlyModifying ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                            .withStyle(ChatFormatting.DARK_GRAY));
                    if (currentlyModifying) {
                        AttachmentMenuOptionsText options = op.get();
                        tooltip.add(Component.literal(" ").append(options.heading()));
                        for (Component text : options.optionComponents())
                            tooltip.add(Component.literal("  ").append(text));
                    }
                }
            }
        }
        if (!tooltip.isEmpty())
            guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
    }

    protected void addBlockedSlotsToTooltip(List<Component> tooltip, Set<ResourceLocation> blockingSlots) {
        if (blockingSlots.isEmpty())
            return;
        MenuTypeSlotsConfig menuConfig = RFEItemAttachmentsMenuSlotsHandler.getConfig(this.menu.getFocusedAttachmentsItem(), this.menu.getType());
        Map<ResourceLocation, SlotConfig> slotConfig = menuConfig.slotConfig();
        Map<ResourceLocation, String> otherKeys = menuConfig.otherSlotTextKeys();
        int blockedCount = 0;
        final int MAX_SLOTS_BLOCKED_MESSAGE = 5;
        int sz = blockingSlots.size();
        StringBuilder blockedText = new StringBuilder();
        for (ResourceLocation loc : blockingSlots) {
            String appendText;
            if (slotConfig.containsKey(loc) && slotConfig.get(loc).emptyText() != null) {
                appendText = I18n.get(slotConfig.get(loc).emptyText());
            } else if (otherKeys.containsKey(loc)) {
                appendText = I18n.get(otherKeys.get(loc));
            } else {
                appendText = loc.toString();
            }
            blockedText.append(appendText);
            ++blockedCount;
            if (blockedCount > MAX_SLOTS_BLOCKED_MESSAGE)
                break;
            if (blockedCount < sz)
                blockedText.append(", ");
        }
        if (blockedCount <= MAX_SLOTS_BLOCKED_MESSAGE) {
            tooltip.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.blocked_by",
                    blockedText.toString()).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.blocked_by.extra",
                    blockedText.toString(), sz - blockedCount).withStyle(ChatFormatting.RED));
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        ItemStack focusedItem = this.menu.getFocusedAttachmentsItem();
        RFEItemAttachmentProperties attachmentProperties = null;

        Item.TooltipContext context = new AttachmentTooltipContext(Item.TooltipContext.of(this.minecraft.level), false, true);
        if (this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot && this.hoveredSlot.getItem() == stack) {
            attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(focusedItem, stack, attachmentSlot.getSlotId());
            if (attachmentProperties != null)
                context = new AttachmentTooltipContext(context, attachmentProperties.overridesDefaults(), true);
        }
        TooltipFlag flag = ClientTooltipFlag.of(this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
        List<Component> tooltip = stack.getTooltipLines(context, this.minecraft.player, flag);

        if (focusedItem == stack) {
            final int ITEM_HIGHLIGHT_COLOR = RFEConfig.CLIENT.attachmentScreenItemColor.getAsInt() & 0xFFFFFF;
            tooltip.add(1, Component.translatable("gui.ritchiesfirearmengine.attachments_menu.tooltip.focused_item")
                    .withColor(ITEM_HIGHLIGHT_COLOR).withStyle(ChatFormatting.ITALIC));
        }
        if (attachmentProperties != null) {
            Optional<AttachmentMenuOptionsText> op = attachmentProperties.getAttachmentConfigTextOptions(stack);
            if (op.isPresent()) {
                boolean currentlyModifying = Screen.hasShiftDown();
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("ritchiesfirearmengine.tooltip.item.hold_key.attachment_options",
                                Component.translatable("ritchiesfirearmengine.tooltip.key_shift")
                                        .withStyle(currentlyModifying ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                        .withStyle(ChatFormatting.DARK_GRAY));
                if (currentlyModifying) {
                    AttachmentMenuOptionsText options = op.get();
                    tooltip.add(Component.literal(" ").append(options.heading()));
                    for (Component text : options.optionComponents())
                        tooltip.add(Component.literal("  ").append(text));
                }
            }
        }
        if (this.hoveredSlot instanceof RFEItemAttachmentSlot) {
            boolean hide = this.hoveredSlot instanceof RFEItemAttachmentSlot && Screen.hasControlDown();
            tooltip.add(Component.translatable("ritchiesfirearmengine.tooltip.item.hold_key.hide_attachment_tooltip",
                            Component.translatable("ritchiesfirearmengine.tooltip.key_ctrl").withStyle(hide ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return tooltip;
    }

    protected void renderValidSlotHighlights(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack carried = this.menu.getCarried();
        ItemStack focusedItem = this.menu.getFocusedAttachmentsItem();
        final int VALID_SLOT_COLOR = RFEConfig.CLIENT.attachmentScreenValidSlotColor.getAsInt();
        final boolean VALID_SLOT_GRADIENT = RFEConfig.CLIENT.attachmentScreenValidSlotGradient.getAsBoolean();
        final int BLOCKED_SLOT_COLOR = RFEConfig.CLIENT.attachmentScreenBlockedSlotColor.getAsInt();
        final int BLOCKING_SLOT_COLOR = RFEConfig.CLIENT.attachmentScreenBlockingSlotColor.getAsInt();
        final boolean BLOCKING_SLOT_GRADIENT = RFEConfig.CLIENT.attachmentScreenBlockingSlotGradient.getAsBoolean();

        Set<ResourceLocation> blockingSlots = Set.of();
        if (this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot) {
            blockingSlots = attachmentSlot.getBlockingSlots();
        } else if (this.hoveredIntegralSlot != null) {
            blockingSlots = this.hoveredIntegralSlot.getBlockingSlots(focusedItem);
        }

        for (Slot slot : this.menu.slots) {
            if (!slot.isActive())
                continue;
            int slotX = slot.x;
            int slotY = slot.y;
            if (slot instanceof RFEItemAttachmentSlot attachmentSlot) {
                ResourceLocation slotId = attachmentSlot.getSlotId();
                boolean attachmentIsEmpty = slot.getItem().isEmpty();
                if (blockingSlots.contains(slotId)) {
                    // Render blocking highlight
                    if (BLOCKING_SLOT_GRADIENT) {
                        guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, BLOCKING_SLOT_COLOR);
                    } else {
                        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, BLOCKING_SLOT_COLOR);
                    }
                } else if (!attachmentSlot.getBlockingSlots().isEmpty()) {
                    // Render blocked highlight. No gradient as we want to make clear the entire thing is blocked
                    guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, BLOCKED_SLOT_COLOR);
                } else if (attachmentIsEmpty && carried.isEmpty() && this.hoveredSlot != null && this.hoveredSlot != slot) {
                    // Render highlight on attachment slot when hovering eligible item outside of attachment slot
                    ItemStack hoveredStack = this.hoveredSlot.getItem();
                    RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(focusedItem, hoveredStack, slotId);
                    if (attachmentProperties != null) {
                        if (VALID_SLOT_GRADIENT) {
                            guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, VALID_SLOT_COLOR);
                        } else {
                            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, VALID_SLOT_COLOR);
                        }
                    }
                } else if (attachmentIsEmpty && !carried.isEmpty()) {
                    // Render highlight on attachment slot when carrying item
                    RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(focusedItem, carried, slotId);
                    if (attachmentProperties != null) {
                        if (VALID_SLOT_GRADIENT) {
                            guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, VALID_SLOT_COLOR);
                        } else {
                            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, VALID_SLOT_COLOR);
                        }
                    }
                }
            } else if (this.hoveredSlot != slot && this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot
                    && this.hoveredSlot.getItem().isEmpty() && attachmentSlot.getBlockingSlots().isEmpty() && carried.isEmpty()) {
                // Render highlight on slot if this item is eligible when hovering empty attachment slot
                ResourceLocation slotId = attachmentSlot.getSlotId();
                RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(focusedItem, slot.getItem(), slotId);
                if (attachmentProperties != null) {
                    if (VALID_SLOT_GRADIENT) {
                        guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, VALID_SLOT_COLOR);
                    } else {
                        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, VALID_SLOT_COLOR);
                    }
                }
            }
        }

        for (RFEIntegralAttachmentButton button : this.integralSlotButtons) {
            if (!button.visible)
                continue;
            int slotX = button.getX() - this.leftPos + 1;
            int slotY = button.getY() - this.topPos + 1;
            if (!blockingSlots.contains(button.getSlotId()))
                continue;
            if (BLOCKING_SLOT_GRADIENT) {
                guiGraphics.fillGradient(slotX, slotY, slotX + 16, slotY + 16, 100, 0, BLOCKING_SLOT_COLOR);
            } else {
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 100, BLOCKING_SLOT_COLOR);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.renderValidSlotHighlights(guiGraphics, mouseX, mouseY); // Labels rendered after hoveredSlot set
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    protected void renderMainItem(GuiGraphics guiGraphics, float partialTick) {
        Player player = this.minecraft.player;
        if (player == null)
            return;
        ItemStack attachmentsItem = this.menu.getFocusedAttachmentsItem();
        if (attachmentsItem.isEmpty())
            return;
        BakedModel bakedmodel = this.minecraft.getItemRenderer().getModel(attachmentsItem, this.minecraft.level, player, 0);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // TODO other model types
        boolean gui3d = bakedmodel.isGui3d();
        poseStack.translate(this.leftPos + this.imageWidth / 2f, this.topPos + 50, 150);

        float modelScale = this.baseScale * this.itemScale;
        poseStack.scale(modelScale, -modelScale, modelScale);

        poseStack.mulPose(new Quaternionf().rotationXYZ(this.angleX * Mth.DEG_TO_RAD, this.angleY * Mth.DEG_TO_RAD, 0));

        guiGraphics.enableScissor(this.leftPos + 2, this.topPos + 2, this.leftPos + 174, this.topPos + 98);

        boolean blockLight = !bakedmodel.usesBlockLight();
        if (blockLight)
            Lighting.setupForFlatItems();
        this.minecraft.getItemRenderer().render(attachmentsItem, ItemDisplayContext.NONE, false, poseStack,
                guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
        guiGraphics.flush();
        if (blockLight)
            Lighting.setupFor3DItems();

        guiGraphics.disableScissor();
        poseStack.popPose();
    }

    protected void renderSlotPointers(GuiGraphics graphics, float partialTick) {
        ResourceLocation slotId;
        int baseSlotX;
        int baseSlotY;
        if (this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot) {
            slotId = attachmentSlot.getSlotId();
            baseSlotX = this.hoveredSlot.x;
            baseSlotY = this.hoveredSlot.y;
        } else if (this.hoveredIntegralSlot != null) {
            slotId = this.hoveredIntegralSlot.getSlotId();
            baseSlotX = this.hoveredIntegralSlot.getX() - this.leftPos + 1;
            baseSlotY = this.hoveredIntegralSlot.getY() - this.topPos + 1;
        } else {
            return;
        }
        MenuTypeDisplayConfig menuConfig = RFEItemAttachmentsScreenDisplayHandler.getConfig(this.menu.getFocusedAttachmentsItem(), this.menu.getType());
        SlotDisplayConfig slotDisplay = menuConfig.slotDisplayConfig().get(slotId);
        if (slotDisplay == null)
            return;
        Vector3f modelOffset = new Vector3f(slotDisplay.modelPos());
        modelOffset.add(-8, -8, -8);
        float modelScale = this.baseScale * this.itemScale / 16f;
        modelOffset.mul(modelScale, -modelScale, -modelScale);
        modelOffset.rotate(new Quaternionf().rotationXYZ(this.angleX * Mth.DEG_TO_RAD, -this.angleY * Mth.DEG_TO_RAD, 0));
        PoseStack poseStack = graphics.pose();
        Vector4f screenPos = new Vector4f(modelOffset, 1f).mul(poseStack.last().pose(), new Vector4f());

        int MIN_X = this.leftPos + 2;
        int MIN_Y = this.topPos + 2;
        int MAX_X = this.leftPos + 174;
        int MAX_Y = this.topPos + 98;

        int pointX = Mth.floor(this.leftPos + this.imageWidth / 2f + screenPos.x);
        int pointY = Mth.floor(this.topPos + 50 + screenPos.y);

        if (MIN_X <= pointX && pointX < MAX_X && MIN_Y <= pointY && pointY < MAX_Y) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 400);
            int slotX = this.leftPos + baseSlotX + 8;
            int slotY = this.topPos + baseSlotY + 8;
            int diffX = pointX - slotX;
            int diffY = pointY - slotY;
            int diffYMag = Mth.abs(diffY);
            int diffXDraw = diffX - diffYMag;
            final int POINTER_COLOR = RFEConfig.CLIENT.attachmentScreenPointerColor.getAsInt();
            if (diffXDraw >= 0) {
                graphics.fill(slotX, slotY, slotX + diffXDraw + 1, slotY + 1, POINTER_COLOR);
                int lineX = slotX + diffXDraw + 1;
                int yD = diffY < 0 ? -1 : 1;
                int lineY = slotY + yD;
                for (int i = 0; i < diffYMag; ++i) {
                    graphics.fill(lineX, lineY, lineX + 1, lineY + 1, POINTER_COLOR);
                    ++lineX;
                    lineY += yD;
                }
            } else {
                graphics.fill(slotX, slotY, MIN_X, slotY + 1, POINTER_COLOR);
                diffYMag = pointX - MIN_X;
                int lineX = MIN_X;
                int yD = diffY < 0 ? -1 : 1;
                int lineY = slotY + yD;
                for (int i = 0; i < diffYMag; ++i) {
                    graphics.fill(lineX, lineY, lineX + 1, lineY + 1, POINTER_COLOR);
                    ++lineX;
                    lineY += yD;
                }
                graphics.fill(lineX, lineY, lineX + 1, pointY + 1, POINTER_COLOR);
            }
            poseStack.popPose();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.leftPos <= mouseX && mouseX < this.leftPos + this.imageWidth
                && this.topPos <= mouseY && mouseY < this.topPos + 100 && this.menu.getCarried().isEmpty()) {
            this.angleX = Mth.clamp(this.angleX + (float) dragY, -90f, 90f);
            this.angleY = Mth.wrapDegrees(this.angleY + (float) dragX);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.leftPos <= mouseX && mouseX < this.leftPos + this.imageWidth
                && this.topPos <= mouseY && mouseY < this.topPos + 100 && this.menu.getCarried().isEmpty()) {
            this.itemScale = Mth.clamp(this.itemScale + (float) scrollY / 16f, 0.5f, 2.0f);
        }
        this.handleAttachmentOptionScroll(scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected void handleAttachmentOptionScroll(double scrollY) {
        if (this.minecraft.player == null)
            return;
        ItemStack focusedItem = this.menu.getFocusedAttachmentsItem();
        if (!(this.hoveredSlot instanceof RFEItemAttachmentSlot attachmentSlot))
            return;
        ItemStack hoveredStack = attachmentSlot.getItem();
        RFEItemAttachmentProperties attachmentProperties = RFEItemAttachmentsPropertiesHandler.getData(focusedItem, hoveredStack, attachmentSlot.getSlotId());
        if (attachmentProperties == null)
            return;
        int attachmentOption = attachmentProperties.getAttachmentConfigOption(hoveredStack);
        if (attachmentOption == -1)
            return;
        int newOption = attachmentOption + (scrollY < 0 ? 1 : -1);
        if (!attachmentProperties.acceptAttachmentConfigOption(hoveredStack, newOption))
            return;
        this.hoveredSlot.set(hoveredStack);
        RFENetwork.sendToServer(ServerboundUpdateAttachmentOptionPacket.forItem(attachmentSlot.getSlotId(), newOption));
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 2f));
    }

    public List<Rect2i> getExtraAreas() { return this.extraAreas; }

}
