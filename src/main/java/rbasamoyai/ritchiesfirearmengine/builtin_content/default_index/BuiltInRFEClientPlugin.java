package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.NoOpProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEClientContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRendererPacksHandler;

import java.util.function.BiConsumer;

public class BuiltInRFEClientPlugin implements RFEClientPlugin {

    @Override
    public void registerClient() {
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("no_op"), new NoOpProjectileRenderer.Serializer());
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("bullet"), new RFEBulletProjectileRenderer.Serializer());
    }

    @Override
    public void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        registry.accept(RitchiesFirearmEngine.resource("projectile_renderers"), RFEProjectileRendererPacksHandler.ReloadListener.INSTANCE);
    }

}
