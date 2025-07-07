package rbasamoyai.ritchiesfirearmengine;

import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPluginManager;

public class RFEClientForge {

    public static void init(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(RFEClientForge::onClientSetup);
        modBus.addListener(RFEClientForge::onRegisterClientReloadListeners);
        modBus.addListener(RFEClientForge::onRegisterKeyMappings);

        forgeBus.addListener(RFEClientForge::onMouseInput);
        forgeBus.addListener(RFEClientForge::onKeyInput);
        forgeBus.addListener(RFEClientForge::onComputeFov);
        forgeBus.addListener(RFEClientForge::onClientLogout);
        forgeBus.addListener(RFEClientForge::onRenderLevel);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(RFEClient::onClientSetup);
    }

    private static void onRegisterClientReloadListeners(final RegisterClientReloadListenersEvent event) {
        RFEClientPluginManager.registerResourceListeners((id, listener) -> event.registerReloadListener(listener));
    }

    private static void onMouseInput(final InputEvent.MouseButton inputEvent) {
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

}
