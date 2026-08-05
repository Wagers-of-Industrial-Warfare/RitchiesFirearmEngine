package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import rbasamoyai.ritchiesfirearmengine.RFEClient;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles.BlackPowderSmokeParticle;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud.AmmoCounterHUDOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.hud.NoHUDOverlayRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes.ScopeAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.NoOpProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEModelProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFESpriteProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileRenderer;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEClientContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.gui.hud.RFEHudOverlayRendererPacksHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentRenderProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.RFEItemAttachmentsRenderingPacksHandler;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.rendering.SimpleSlotAttachmentRenderData;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.rendering.RFEProjectileRendererPacksHandler;

import java.util.function.BiConsumer;

public class BuiltInRFEClientPlugin implements RFEClientPlugin {

    @Override
    public void registerClient() {
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("no_op"), new NoOpProjectileRenderer.Serializer());
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("bullet"), new RFEBulletProjectileRenderer.Serializer());
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("model"), new RFEModelProjectileRenderer.Serializer());
        RFEClientContentBuilderRegistry.registerProjectileRendererType(RitchiesFirearmEngine.resource("sprite"), new RFESpriteProjectileRenderer.Serializer());

        RFEClientContentBuilderRegistry.registerHUDOverlayRendererType(RitchiesFirearmEngine.resource("no_hud"), new NoHUDOverlayRenderer.Serializer());
        RFEClientContentBuilderRegistry.registerHUDOverlayRendererType(RitchiesFirearmEngine.resource("ammo_counter"), new AmmoCounterHUDOverlayRenderer.Serializer());

        ItemAttachmentRendering.register();
    }

    @Override
    public void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
        registry.accept(RitchiesFirearmEngine.resource("projectile_renderers"), RFEProjectileRendererPacksHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("hud_overlay_renderers"), RFEHudOverlayRendererPacksHandler.ReloadListener.INSTANCE);
        registry.accept(RitchiesFirearmEngine.resource("item_attachment_renderers"), RFEItemAttachmentsRenderingPacksHandler.ReloadListener.INSTANCE);
    }

    @Override
    public void registerParticleProviders(RFEClient.ParticleRegistry registry) {
        registry.registerSpriteSet(BuiltInRFEPlugin.ParticleTypes.BLACK_POWDER_SMOKE, new BlackPowderSmokeParticle.SpriteRegistration());
    }

    public static class ItemAttachmentRendering {
        public static final RFEItemAttachmentRenderProperties.Serializer<SimpleSlotAttachmentRenderData> SIMPLE = register("simple", new SimpleSlotAttachmentRenderData.Serializer());
        public static final RFEItemAttachmentRenderProperties.Serializer<ScopeAttachmentRenderProperties> SCOPE = register("scope", new ScopeAttachmentRenderProperties.Serializer());

        public static void register() {}

        private static <T extends RFEItemAttachmentRenderProperties> RFEItemAttachmentRenderProperties.Serializer<T> register(String id, RFEItemAttachmentRenderProperties.Serializer<T> ser) {
            RFEClientContentBuilderRegistry.registerItemAttachmentRenderingSerializer(RitchiesFirearmEngine.resource(id), ser);
            return ser;
        }

        private ItemAttachmentRendering() {}
    }

}
