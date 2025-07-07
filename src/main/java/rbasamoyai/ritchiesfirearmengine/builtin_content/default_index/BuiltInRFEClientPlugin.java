package rbasamoyai.ritchiesfirearmengine.builtin_content.default_index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.plugins.RFEClientPlugin;

import java.util.function.BiConsumer;

public class BuiltInRFEClientPlugin implements RFEClientPlugin {

    @Override
    public void registerClient() {
    }

    @Override
    public void registerResourceListeners(BiConsumer<ResourceLocation, PreparableReloadListener> registry) {
    }

}
