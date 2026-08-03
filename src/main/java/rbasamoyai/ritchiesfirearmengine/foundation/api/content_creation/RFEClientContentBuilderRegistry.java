package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRenderer;

public class RFEClientContentBuilderRegistry {

    private static final Object2ReferenceMap<ResourceLocation, RFEProjectileRenderer.Serializer> PROJECTILE_RENDERER_SERIALIZERS = new Object2ReferenceOpenHashMap<>();

    public static void registerProjectileRendererType(ResourceLocation typeLoc, RFEProjectileRenderer.Serializer rendererSer) {
        if (PROJECTILE_RENDERER_SERIALIZERS.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE projectile renderer type with id '" + typeLoc + "'");
        PROJECTILE_RENDERER_SERIALIZERS.put(typeLoc, rendererSer);
    }

    @ApiStatus.Internal
    public static RFEProjectileRenderer.Serializer getProjectileRendererType(ResourceLocation typeLoc) {
        if (!PROJECTILE_RENDERER_SERIALIZERS.containsKey(typeLoc))
            throw new IllegalStateException("RFE projectile renderer type '" + typeLoc +"' not present");
        return PROJECTILE_RENDERER_SERIALIZERS.get(typeLoc);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFEHudOverlayRenderer.Serializer> HUD_OVERLAY_RENDERER_SERIALIZERS = new Object2ReferenceOpenHashMap<>();

    public static void registerHUDOverlayRendererType(ResourceLocation typeLoc, RFEHudOverlayRenderer.Serializer rendererSer) {
        if (HUD_OVERLAY_RENDERER_SERIALIZERS.containsKey(typeLoc))
            throw new IllegalStateException("Already registered RFE HUD overlay renderer type with id '" + typeLoc + "'");
        HUD_OVERLAY_RENDERER_SERIALIZERS.put(typeLoc, rendererSer);
    }

    @ApiStatus.Internal
    public static RFEHudOverlayRenderer.Serializer getHUDOverlayRendererType(ResourceLocation typeLoc) {
        if (!HUD_OVERLAY_RENDERER_SERIALIZERS.containsKey(typeLoc))
            throw new IllegalStateException("RFE HUD overlay renderer type '" + typeLoc + "' not present");
        return HUD_OVERLAY_RENDERER_SERIALIZERS.get(typeLoc);
    }

    private static final Object2ReferenceMap<ResourceLocation, RFEItemAttachmentRenderProperties.Serializer<?>> ITEM_ATTACHMENT_RENDERING_SERIALIZERS = new Object2ReferenceOpenHashMap<>();
    private static final Reference2ObjectMap<RFEItemAttachmentRenderProperties.Serializer<?>, ResourceLocation> ITEM_ATTACHMENT_RENDERING_SERIALIZERS_IDS = new Reference2ObjectOpenHashMap<>();

    public static void registerItemAttachmentRenderingSerializer(ResourceLocation id, RFEItemAttachmentRenderProperties.Serializer<?> prov) {
        if (ITEM_ATTACHMENT_RENDERING_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("Already registered RFE item attachment rendering properties serializer with id '" + id + "'");
        ITEM_ATTACHMENT_RENDERING_SERIALIZERS.put(id, prov);
        ITEM_ATTACHMENT_RENDERING_SERIALIZERS_IDS.put(prov, id);
    }

    public static RFEItemAttachmentRenderProperties.Serializer<?> getItemAttachmentRenderingSerializer(ResourceLocation id) {
        if (!ITEM_ATTACHMENT_RENDERING_SERIALIZERS.containsKey(id))
            throw new IllegalStateException("RFE item attachment rendering properties serializer of type '" + id + "' not present");
        return ITEM_ATTACHMENT_RENDERING_SERIALIZERS.get(id);
    }

    public static ResourceLocation getItemAttachmentRenderingSerializerId(RFEItemAttachmentRenderProperties.Serializer<?> prov) {
        if (!ITEM_ATTACHMENT_RENDERING_SERIALIZERS_IDS.containsKey(prov))
            throw new IllegalStateException("Unknown RFE item attachment rendering properties serializer " + prov);
        return ITEM_ATTACHMENT_RENDERING_SERIALIZERS_IDS.get(prov);
    }

}
