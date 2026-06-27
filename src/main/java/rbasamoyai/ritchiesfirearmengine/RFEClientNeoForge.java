package rbasamoyai.ritchiesfirearmengine;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPluginManager;

@Mod(value = RitchiesFirearmEngine.MOD_ID, dist = Dist.CLIENT)
public class RFEClientNeoForge {

    public RFEClientNeoForge(IEventBus modBus, ModContainer container) {
        modBus.addListener(RFEClientNeoForge::onClientSetup);
        modBus.addListener(RFEClientNeoForge::onRegisterClientReloadListeners);
        modBus.addListener(RFEClientNeoForge::onRegisterKeyMappings);
        modBus.addListener(RFEClientNeoForge::onRegisterParticleProviders);
        modBus.addListener(RFEClientNeoForge::onModelRegistry);

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.addListener(RFEClientNeoForge::onMouseInput);
        forgeBus.addListener(RFEClientNeoForge::onKeyInput);
        forgeBus.addListener(RFEClientNeoForge::onComputeFov);
        forgeBus.addListener(RFEClientNeoForge::onClientLogout);
        forgeBus.addListener(RFEClientNeoForge::onRenderLevel);
        forgeBus.addListener(RFEClientNeoForge::onRenderGuiOverlay);
        forgeBus.addListener(RFEClientNeoForge::onSetupCamera);
        forgeBus.addListener(RFEClientNeoForge::onClientTick);
        forgeBus.addListener(RFEClientNeoForge::onGatherSkippedAttributeModifierTooltips);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(RFEClient::onClientSetup);
    }

    private static void onRegisterClientReloadListeners(final RegisterClientReloadListenersEvent event) {
        RFEClientPluginManager.registerResourceListeners((id, listener) -> event.registerReloadListener(listener));
    }

    private static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        RFEClient.onRegisterParticleProviders(new NeoForgeParticleRegistry(event));
    }

    private record NeoForgeParticleRegistry(RegisterParticleProvidersEvent event) implements RFEClient.ParticleRegistry {
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
                    event.getPartialTick().getGameTimeDeltaPartialTick(true), event.getCamera(), event.getFrustum());
        }
    }

    private static void onRenderGuiOverlay(final RenderGuiLayerEvent.Pre event) {
        if (event.getName() == VanillaGuiLayers.HOTBAR) {
            RFEClient.renderHUDOverlay(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        }
        // TODO crosshair
    }

    private static void onSetupCamera(final ViewportEvent.ComputeCameraAngles event) {
        RFEClient.modifyCameraAngles(new NeoForgeSetCameraAngles(event));
    }

    private record NeoForgeSetCameraAngles(ViewportEvent.ComputeCameraAngles event) implements RFEClient.SetCameraAngles {
        @Override public float getPitch() { return this.event.getPitch(); }
        @Override public void setPitch(float pitch) { this.event.setPitch(pitch); }

        @Override public float getYaw() { return this.event.getYaw(); }
        @Override public void setYaw(float yaw) { this.event.setYaw(yaw); }

        @Override public float getRoll() { return this.event.getRoll(); }
        @Override public void setRoll(float roll) { this.event.setRoll(roll); }
    }

    private static void onModelRegistry(final ModelEvent.RegisterAdditional event) {
        RFEClient.registerModels(event::register);
    }

    private static void onClientTick(final ClientTickEvent.Pre event) {
        RFEClient.onClientTickPre();
    }

    private static void onGatherSkippedAttributeModifierTooltips(final GatherSkippedAttributeTooltipsEvent event) {
        RFEClient.gatherSkippedAttributeModifierTooltips(event.getStack(), event);
    }

}
