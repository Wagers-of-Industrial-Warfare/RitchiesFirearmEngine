package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.foundation.gui.RFEItemSlotTextureDecorations;

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

    private RFEClientRemix() {}

}
