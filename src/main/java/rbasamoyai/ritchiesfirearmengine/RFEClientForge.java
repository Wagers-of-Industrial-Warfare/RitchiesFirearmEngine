package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPluginManager;

public class RFEClientForge {

    public static void init(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(RFEClientForge::onClientSetup);
        modBus.addListener(RFEClientForge::onRegisterClientReloadListeners);
        modBus.addListener(RFEClientForge::onRegisterKeyMappings);
        modBus.addListener(RFEClientForge::onRegisterParticleProviders);

        forgeBus.addListener(RFEClientForge::onMouseInput);
        forgeBus.addListener(RFEClientForge::onKeyInput);
        forgeBus.addListener(RFEClientForge::onComputeFov);
        forgeBus.addListener(RFEClientForge::onClientLogout);
        forgeBus.addListener(RFEClientForge::onRenderLevel);
        forgeBus.addListener(RFEClientForge::onRenderGuiOverlay);
        forgeBus.addListener(RFEClientForge::onSetupCamera);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(RFEClient::onClientSetup);
    }

    private static void onRegisterClientReloadListeners(final RegisterClientReloadListenersEvent event) {
        RFEClientPluginManager.registerResourceListeners((id, listener) -> event.registerReloadListener(listener));
    }

    private static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        RFEClient.onRegisterParticleProviders(new ForgeParticleRegistry(event));
    }

    private record ForgeParticleRegistry(RegisterParticleProvidersEvent event) implements RFEClient.ParticleRegistry {
        @Override
        public void registerSpecial(ParticleType<?> type, ParticleProvider<?> provider) {
            this.innerRegisterSpecial(type, provider);
        }

        @SuppressWarnings("unchecked")
        private <T extends ParticleOptions> void innerRegisterSpecial(ParticleType<T> type, ParticleProvider<?> provider) {
            this.event.registerSpecial(type, (ParticleProvider<T>) provider);
        }

        @Override
        public void registerSprite(ParticleType<?> type, ParticleProvider.Sprite<?> sprite) {
            this.innerRegisterSprite(type, sprite);
        }

        @SuppressWarnings("unchecked")
        private <T extends ParticleOptions> void innerRegisterSprite(ParticleType<T> type, ParticleProvider.Sprite<?> provider) {
            this.event.registerSprite(type, (ParticleProvider.Sprite<T>) provider);
        }

        @Override
        public void registerSpriteSet(ParticleType<?> type, ParticleEngine.SpriteParticleRegistration<?> spriteSet) {
            this.innerRegisterSpriteSet(type, spriteSet);
        }

        @SuppressWarnings("unchecked")
        private <T extends ParticleOptions> void innerRegisterSpriteSet(ParticleType<T> type, ParticleEngine.SpriteParticleRegistration<?> spriteSet) {
            this.event.registerSpriteSet(type, (ParticleEngine.SpriteParticleRegistration<T>) spriteSet);
        }
    }

    private static void onMouseInput(final InputEvent.MouseButton.Pre inputEvent) {
        RFEClient.onMouseInput(inputEvent.getButton(), inputEvent.getButton(), inputEvent.getModifiers());
    }

    private static void onKeyInput(final InputEvent.Key event) {
        RFEClient.onKeyInput(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
    }

    private static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        RFEClient.registerKeyMappings(event::register);
    }

    private static void onComputeFov(final ComputeFovModifierEvent event) {
        event.setNewFovModifier(RFEClient.modifyFov(event.getNewFovModifier(), event.getPlayer()));
    }

    private static void onClientLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        RFEClient.onClientLogout();
    }

    private static void onRenderLevel(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            RFEClient.renderAfterEntities(event.getPoseStack(), event.getProjectionMatrix(), event.getRenderTick(),
                    event.getPartialTick(), event.getCamera(), event.getFrustum());
        }
    }

    private static void onRenderGuiOverlay(final RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().overlay() == VanillaGuiOverlay.HOTBAR.type().overlay()) {
            RFEClient.renderHUDOverlay(event.getGuiGraphics(), event.getPartialTick());
        }
        // TODO crosshair
    }

    private static void onSetupCamera(final ViewportEvent.ComputeCameraAngles event) {
        RFEClient.modifyCameraAngles(new ForgeSetCameraAngles(event));
    }

    private record ForgeSetCameraAngles(ViewportEvent.ComputeCameraAngles event) implements RFEClient.SetCameraAngles {
        @Override public float getPitch() { return this.event.getPitch(); }
        @Override public void setPitch(float pitch) { this.event.setPitch(pitch); }

        @Override public float getYaw() { return this.event.getYaw(); }
        @Override public void setYaw(float yaw) { this.event.setYaw(yaw); }

        @Override public float getRoll() { return this.event.getRoll(); }
        @Override public void setRoll(float roll) { this.event.setRoll(roll); }
    }

}
