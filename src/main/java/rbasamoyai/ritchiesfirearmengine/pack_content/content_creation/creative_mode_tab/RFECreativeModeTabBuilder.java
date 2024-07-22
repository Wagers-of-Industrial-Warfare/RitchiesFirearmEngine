package rbasamoyai.ritchiesfirearmengine.pack_content.content_creation.creative_mode_tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Invalid item tag for icon of tab {}: {}", tabId, e);
                return ItemStack.EMPTY;
            }
        });

        String background = GsonHelper.getAsString(obj, "background", "items.png");
        builder.backgroundSuffix(background);

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
                } catch (CommandSyntaxException e) {
                    LOGGER.warn("Invalid item tag at index {} for item tab {}: {}", i, tabId, e);
                }
            }
        });

        return builder.build();
    }

    private static ItemStack readItem(JsonElement element) throws CommandSyntaxException {
        ResourceLocation loc;
        CompoundTag tag = null;
        if (GsonHelper.isStringValue(element)) {
            loc = RFEUtils.location(element.getAsString());
        } else if (element.isJsonObject()) {
            JsonObject detailedItemObj = element.getAsJsonObject();
            loc = RFEUtils.location(GsonHelper.getAsString(detailedItemObj, "item"));
            if (GsonHelper.isStringValue(detailedItemObj, "tag"))
                tag = TagParser.parseTag(GsonHelper.getAsString(detailedItemObj, "tag"));
        } else {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        ItemStack itemStack = new ItemStack(item);
        if (itemStack.isEmpty())
            return ItemStack.EMPTY;
        if (tag != null)
            itemStack.setTag(tag);
        return itemStack;
    }

    private static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }

    private RFECreativeModeTabBuilder() {}

}
