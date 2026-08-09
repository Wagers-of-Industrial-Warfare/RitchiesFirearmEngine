package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.items.RFEItemBuilder;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties.AttachmentTooltipContext;

import java.util.Collections;
import java.util.List;

public class ScopeItem extends Item {

    protected final ImmutableList<Float> defaultZoomLevels;
    protected final boolean defaultBlocksSpeedloaders;
    private final float minDisplayedZoom;
    private final float maxDisplayedZoom;

    public ScopeItem(Properties properties, ImmutableList<Float> defaultZoomLevels, boolean defaultBlocksSpeedloaders) {
        super(properties.stacksTo(1).component(BuiltInRFEPlugin.RFEDataComponents.ZOOM_LEVEL_INDEX, 0));
        this.defaultZoomLevels = defaultZoomLevels;
        this.defaultBlocksSpeedloaders = defaultBlocksSpeedloaders;
        this.minDisplayedZoom = Collections.min(this.defaultZoomLevels);
        this.maxDisplayedZoom = Collections.max(this.defaultZoomLevels);
    }

    public ImmutableList<Float> getDefaultZoomLevels() { return this.defaultZoomLevels; }
    public boolean blocksSpeedloadersByDefault() { return this.defaultBlocksSpeedloaders; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (context instanceof AttachmentTooltipContext atCtx && atCtx.overrideDefaults())
            return; // Do not display this

        int zoomIndex = stack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.ZOOM_LEVEL_INDEX, 0);
        if (this.defaultZoomLevels.isEmpty())
            return; // This shouldn't happen!
        if (this.defaultZoomLevels.size() == 1) {
            tooltipComponents.add(Component.translatable("ritchiesfirearmengine.tooltip.fixed_zoom", String.format("%.2f", this.defaultZoomLevels.getFirst())));
        } else {
            float currentZoomLevel = 0 <= zoomIndex && zoomIndex < this.defaultZoomLevels.size() ?
                    this.defaultZoomLevels.get(zoomIndex) : this.defaultZoomLevels.getFirst();
            tooltipComponents.add(Component.translatable("ritchiesfirearmengine.tooltip.current_zoom", String.format("%.2f", currentZoomLevel)));
            tooltipComponents.add(Component.translatable("ritchiesfirearmengine.tooltip.zoom_levels", String.format("%.2f", this.minDisplayedZoom), String.format("%.2f", this.maxDisplayedZoom)));
        }
    }

    public static class Builder implements RFEItemBuilder {
        @Override
        public Item apply(JsonObject jsonObject) {
            ImmutableList.Builder<Float> defaultZoomLevels = ImmutableList.builder();
            if (GsonHelper.isNumberValue(jsonObject, "zoom")) {
                float singleZoom = GsonHelper.getAsFloat(jsonObject, "zoom");
                if (singleZoom < 0.1f)
                    throw new IllegalStateException("Zoom level must be at least 0.1x; was " + singleZoom);
                defaultZoomLevels.add(singleZoom);
            } else if (GsonHelper.isArrayNode(jsonObject, "zoom_levels")) {
                for (JsonElement zoomEl : GsonHelper.getAsJsonArray(jsonObject, "zoom_levels")) {
                    if (!GsonHelper.isNumberValue(zoomEl))
                        throw new JsonParseException("Zoom level must be numerical value");
                    float zoom = zoomEl.getAsFloat();
                    if (zoom < 0.1f)
                        throw new IllegalStateException("Zoom level must be at least 0.1x; was " + zoom);
                    defaultZoomLevels.add(zoom);
                }
            } else {
                throw new JsonParseException("Must specify either numerical \"zoom\" value or numerical list \"zoom_levels\"");
            }
            boolean blocksSpeedloadersByDefault = GsonHelper.getAsBoolean(jsonObject, "blocks_speedloaders", true);
            return new ScopeItem(new Item.Properties(), defaultZoomLevels.build(), blocksSpeedloadersByDefault);
        }
    }

}
