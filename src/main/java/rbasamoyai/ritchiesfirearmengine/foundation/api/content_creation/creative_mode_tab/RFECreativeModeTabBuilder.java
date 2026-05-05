package rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.creative_mode_tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

public class RFECreativeModeTabBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static CreativeModeTab buildTab(ResourceLocation tabId, JsonObject obj) {
        final JsonObject copy = obj.deepCopy(); // For lazily evaluated objects
        CreativeModeTab.Builder builder = builder();

        String titleId = GsonHelper.getAsString(obj, "title", "itemGroup." + tabId.getNamespace() + "." + tabId.getPath());

        builder.title(Component.translatable(titleId));
        builder.icon(() -> {
            try {
                return readItem(copy.get("icon"));
            } catch (IllegalStateException e) {
                LOGGER.warn("Invalid item tag for icon of tab {}: {}", tabId, e);
                return ItemStack.EMPTY;
            }
        });

        String background = GsonHelper.getAsString(obj, "background", "minecraft:textures/gui/container/creative_inventory/tab_items.png");
        ResourceLocation bgLoc = RFEUtils.location(background);
        builder.backgroundTexture(bgLoc);

        builder.displayItems((params, output) -> {
            JsonArray tabContentsJson = GsonHelper.getAsJsonArray(copy, "contents");
            int sz = tabContentsJson.size();
            for (int i = 0; i < sz; ++i) {
                try {
                    ItemStack itemStack = readItem(tabContentsJson.get(i));
                    if (itemStack.isEmpty()) {
                        LOGGER.warn("Encountered invalid item entry at index {} for item tab {}", i, tabId);
                        continue;
                    }
                    output.accept(itemStack);
                } catch (IllegalStateException e) {
                    LOGGER.warn("Invalid item tag at index {} for item tab {}: {}", i, tabId, e);
                }
            }
        });

        return builder.build();
    }

    private static ItemStack readItem(JsonElement element) throws IllegalStateException {
        if (GsonHelper.isStringValue(element)) {
            ResourceLocation loc = RFEUtils.location(element.getAsString());
            return new ItemStack(BuiltInRegistries.ITEM.getOptional(loc).orElseThrow(() -> new IllegalStateException("Missing item " + loc)));
        } else if (element.isJsonObject()) {
            return ItemStack.STRICT_SINGLE_ITEM_CODEC.parse(JsonOps.INSTANCE, element.getAsJsonObject()).getOrThrow();
        } else {
            return ItemStack.EMPTY;
        }
    }

    private static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }

    private RFECreativeModeTabBuilder() {}

}
