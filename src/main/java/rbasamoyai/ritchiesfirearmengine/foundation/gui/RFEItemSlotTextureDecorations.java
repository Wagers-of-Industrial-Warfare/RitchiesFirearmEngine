package rbasamoyai.ritchiesfirearmengine.foundation.gui;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

import javax.annotation.Nullable;

public class RFEItemSlotTextureDecorations {

    private static final Reference2ReferenceMap<BakedModel, TextureAtlasSprite> SLOT_OVERLAYS_0 = new Reference2ReferenceOpenHashMap<>();
    public static final String SLOT_OVERLAY0 = RitchiesFirearmEngine.MOD_ID + ":slot_overlay0";

    public static void clear() {
        SLOT_OVERLAYS_0.clear();
    }

    public static void registerSlotOverlay0(BakedModel model, TextureAtlasSprite sprite) { SLOT_OVERLAYS_0.put(model, sprite); }
    @Nullable public static TextureAtlasSprite getSlotOverlay0ForModel(BakedModel model) { return SLOT_OVERLAYS_0.get(model); }

}
