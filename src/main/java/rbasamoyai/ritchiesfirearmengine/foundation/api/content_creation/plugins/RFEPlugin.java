package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.function.BiConsumer;

public interface RFEPlugin {

    default void register() {}

    default void afterPackLoading() {}

    default void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {}

    record Info(String modId, String classPath) {
    }

}
