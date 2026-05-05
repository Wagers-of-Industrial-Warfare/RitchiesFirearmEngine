package rbasamoyai.ritchiesfirearmengine.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;

@EventBusSubscriber(modid = RitchiesFirearmEngine.MOD_ID)
public class RFENetworkNeoForge {

    public static final String VERSION = "1.0.0";

    @SubscribeEvent
    public static void onRegisterNetwork(RegisterPayloadHandlersEvent evt) {
        final PayloadRegistrar registrar = evt.registrar(VERSION)
            .executesOn(HandlerThread.MAIN);
        registrar.playBidirectional(RFENeoForgePacket.TYPE, RFENeoForgePacket.STREAM_CODEC, new MainThreadPayloadHandler<>(RFENeoForgePacket::handlePacket));
    }

}
