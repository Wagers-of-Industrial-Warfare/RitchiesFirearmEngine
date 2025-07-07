package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
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

}
