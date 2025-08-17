package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import rbasamoyai.ritchiesfirearmengine.RFEClient;

import java.util.function.BiConsumer;

public interface RFEClientPlugin {

    default void registerClient() {}

    default void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {}

    default void registerParticleProviders(RFEClient.ParticleRegistry registry) {}

    default void onClientSetup() {}

}
