package rbasamoyai.ritchiesfirearmengine.remix;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import rbasamoyai.ritchiesfirearmengine.RFEModsNeoForge;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.HoldAttackKeyInteraction;
import rbasamoyai.ritchiesfirearmengine.foundation.compat.shoulder_surfing.ShoulderSurfingCompat;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;
import rbasamoyai.ritchiesfirearmengine.foundation.gui.RFEItemSlotTextureDecorations;
import rbasamoyai.ritchiesfirearmengine.network.RFENetwork;
import rbasamoyai.ritchiesfirearmengine.network.ServerboundSetAttackKeyPacket;

import java.util.function.Function;

public class RFEClientRemix {

    private static final Material MISSING_BLOCK_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation());

    public static void renderSlotOverlays(GuiGraphics graphics, Font font, ItemStack itemStack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(itemStack, mc.level, mc.player, 0);
        TextureAtlasSprite overlay0 = RFEItemSlotTextureDecorations.getSlotOverlay0ForModel(model);
        if (overlay0 != null)
            graphics.blit(x, y, 210, 16, 16, overlay0);
    }

    public static void loadSlotOverlaysFromBlockModel(BlockModel model, BakedModel baked, Function<Material, TextureAtlasSprite> spriteGetter) {
        Material slotOverlay0 = model.getMaterial(RFEItemSlotTextureDecorations.SLOT_OVERLAY0);
        if (!MISSING_BLOCK_TEXTURE.equals(slotOverlay0))
            RFEItemSlotTextureDecorations.registerSlotOverlay0(baked, spriteGetter.apply(slotOverlay0));
    }

    public static void handleAttackKeybinds() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // TODO offhand item
        ItemStack mainhandItem = mc.player.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction hold && !hold.isHoldingAttackKey(mainhandItem, mc.player)) {
            RFENetwork.sendToServer(new ServerboundSetAttackKeyPacket(true));
            hold.onPressAttackKey(mainhandItem, mc.player);
        }
    }

    public static void interruptAttack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainhandItem = mc.player.getMainHandItem();
        if (mainhandItem.getItem() instanceof HoldAttackKeyInteraction hold && hold.isHoldingAttackKey(mainhandItem, mc.player)) {
            RFENetwork.sendToServer(new ServerboundSetAttackKeyPacket(false));
            hold.onReleaseAttackKey(mainhandItem, mc.player);
        }
    }

    public static boolean blockCrosshairRenderingBlock() {
        if (RFEModsNeoForge.SHOULDERSURFING.runIfInstalled(() -> () -> ShoulderSurfingCompat.isShoulderSurfing()).orElse(false)
            && RFEConfig.CLIENT.renderCrosshairOnShoulderSurfingAim.get())
            return true;
        return false;
    }

    public static void handleItemCameraTransforms(PoseStack poseStack, BakedModel model, ItemDisplayContext context, boolean leftHand) {
        ClientHooks.handleCameraTransforms(poseStack, model, context, leftHand);
    }

    public static void renderValidAmmoHighlight(GuiGraphics graphics, Slot slot) {
        graphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16, 100, 0x7F00FF00, 0x7F00FF00);
        //AbstractContainerScreen.renderSlotHighlight(graphics, slot.x, slot.y, 0, 0x7F00FF00);
    }

    private RFEClientRemix() {}

}
