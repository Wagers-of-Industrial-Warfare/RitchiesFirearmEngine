package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentMenuOptionsText;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentTooltipContext;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentsPropertiesHandler;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundUpdateAttachmentOptionPacket;

import java.util.List;
import java.util.Optional;

public class RFEItemAttachmentsScreen extends AbstractContainerScreen<RFEItemAttachmentsMenu> {

    private static final ResourceLocation MENU_TEXTURE = RitchiesFirearmEngine.resource("textures/gui/field_attachments_menu.png");

    protected float angleX = 15;
    protected float angleY = -135;
    protected float itemScale = 1;

    public RFEItemAttachmentsScreen(RFEItemAttachmentsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fillGradient(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight - 94, 0, 0x3F000000, 0x3F1F7FFF);
        guiGraphics.blit(MENU_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.menu.iterateAttachmentSlots(slot -> {
            guiGraphics.blit(MENU_TEXTURE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, this.imageWidth, 0, 18, 18);
            if (this.menu.getCarried().isEmpty() && this.hoveredSlot == slot && !slot.hasItem()) {
                String text = slot.getEmptyTextKey();
                if (text != null)
                    guiGraphics.renderTooltip(this.font, Component.translatable(text), mouseX, mouseY);
            }
        });

        int itemX = this.leftPos + 8 + this.menu.getSelectedIndex() * 18;
        int itemY = this.topPos + this.imageHeight - 24;
        guiGraphics.fillGradient(itemX, itemY, itemX + 16, itemY + 16, 100, 0, 0x7F1F7FFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderMainItem(guiGraphics, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // TODO render item pointer overlays
        // TODO render object options
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

        if (focusedItem == stack)
            tooltip.add(1, Component.translatable("gui.ritchiesfirearmengine.attachments_menu.tooltip.focused_item")
                    .withColor(0x1F7FFF).withStyle(ChatFormatting.ITALIC));
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
        return tooltip;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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

        // TODO temporary, need to switch to assets folder thing
        // TODO mouse-manipulable scaling
        Vector3f guiScaleOriginal = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GUI).scale;
        Vector3f guiScaleCopy = guiScaleOriginal.mul(64.0f * this.itemScale, new Vector3f());
        poseStack.scale(guiScaleCopy.x, -guiScaleCopy.y, guiScaleCopy.z);

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
            // TODO scroll limits config
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
        RFENetwork.sendToServer(new ServerboundUpdateAttachmentOptionPacket(attachmentSlot.getSlotId(), newOption));
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 2f));
    }

}
