package rbasamoyai.ritchiesfirearmengine;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class RFEClientForge {

    public static void init(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(RFEClientForge::onClientSetup);
        modBus.addListener(RFEClientForge::onRegisterKeyMappings);

        forgeBus.addListener(RFEClientForge::onMouseInput);
        forgeBus.addListener(RFEClientForge::onKeyInput);
        forgeBus.addListener(RFEClientForge::onComputeFov);
        forgeBus.addListener(RFEClientForge::onClientLogout);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(RFEClient::onClientSetup);
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

}
