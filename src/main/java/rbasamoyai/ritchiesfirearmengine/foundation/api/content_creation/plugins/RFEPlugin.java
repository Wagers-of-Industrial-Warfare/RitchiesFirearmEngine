package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;

public interface RFEPlugin {

    default void register() {}

    default void afterPackLoading() {}

    default void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {}

    default void registerPluginItems(BiConsumer<ResourceLocation, Item> registry) {}

    default void registerPluginCreativeModeTabs(BiConsumer<ResourceLocation, CreativeModeTab> registry) {}

    default void registerPluginParticleTypes(BiConsumer<ResourceLocation, ParticleType<?>> registry) {}

    record Info(String modId, String classPath) {
    }

}
