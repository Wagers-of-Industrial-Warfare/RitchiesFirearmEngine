package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.scopes;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.AttachmentSlots;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record ScopeAttachmentProperties(boolean overrideScopeDefaults, ImmutableList<Float> zoomLevels, boolean blocksSpeedloaders) implements RFEItemAttachmentProperties {

    public static final ScopeAttachmentProperties DONT_OVERRIDE_DEFAULTS = new ScopeAttachmentProperties(false, ImmutableList.of(), false);

    @Override
    public Optional<AttachmentMenuOptionsText> getAttachmentConfigTextOptions(ItemStack itemStack) {
        List<Float> zoomLevels;
        if (this.overrideScopeDefaults) {
            zoomLevels = this.zoomLevels;
        } else if (itemStack.getItem() instanceof ScopeItem scopeItem) {
            zoomLevels = scopeItem.getDefaultZoomLevels();
        } else {
            return Optional.empty();
        }
        if (zoomLevels.size() <= 1) // Do not provide options for fixed-power scopes
            return Optional.empty();
        int zoomIndex = itemStack.getOrDefault(RFEDataComponents.ZOOM_LEVEL_INDEX, 0);
        if (zoomIndex < 0 || zoomLevels.size() <= zoomIndex)
            zoomIndex = 0;
        List<Component> options = new ArrayList<>();
        int start = Math.max(zoomIndex - 2, 0);
        int excessStart = Math.max(0, 2 - zoomIndex);
        int end = Math.min(zoomIndex + 3 + excessStart, zoomLevels.size());
        start -= Math.max(0, zoomIndex + 3 + excessStart - zoomLevels.size());
        start = Math.max(0, start);
        boolean oversize = zoomLevels.size() > 5;
        if (start > 0 && oversize) {
            options.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
            ++start;
        }
        if (end < zoomLevels.size())
            --end;
        for (int i = start; i < end; ++i) {
            float zoomLevel = zoomLevels.get(i);
            boolean selected = i == zoomIndex;
            MutableComponent optionText = Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.zoom.value", String.format("%.2f", zoomLevel))
                    .withStyle(selected ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            if (selected)
                optionText.withStyle(ChatFormatting.UNDERLINE);
            options.add(optionText);
        }
        if (end < zoomLevels.size())
            options.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        return Optional.of(new AttachmentMenuOptionsText(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.zoom")
                .withStyle(ChatFormatting.GRAY), options));
    }

    @Override
    public int getAttachmentConfigOption(ItemStack itemStack) {
        int sz;
        if (this.overrideScopeDefaults) {
            sz = this.zoomLevels.size();
        } else if (itemStack.getItem() instanceof ScopeItem scopeItem) {
            sz = scopeItem.getDefaultZoomLevels().size();
        } else {
            return -1;
        }
        return sz <= 1 ? -1 : Mth.clamp(itemStack.getOrDefault(RFEDataComponents.ZOOM_LEVEL_INDEX, 0), 0, sz - 1);
    }

    @Override
    public boolean acceptAttachmentConfigOption(ItemStack itemStack, int option) {
        int sz;
        if (this.overrideScopeDefaults) {
            sz = this.zoomLevels.size();
        } else if (itemStack.getItem() instanceof ScopeItem scopeItem) {
            sz = scopeItem.getDefaultZoomLevels().size();
        } else {
            return false;
        }
        if (sz <= 1) // Don't change fixed-power scopes
            return false;
        int finalOption = Mth.clamp(option, 0, sz - 1);
        itemStack.set(RFEDataComponents.ZOOM_LEVEL_INDEX, finalOption);
        return finalOption == option; // Sync only if not restricted
    }

    @Override public boolean overridesDefaults() { return this.overrideScopeDefaults; }

    @Override public RFEItemAttachmentProperties.Serializer<?> getSerializer() { return AttachmentSlots.SCOPE; }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<ScopeAttachmentProperties> {
        private static final MapCodec<ScopeAttachmentProperties> OVERRIDE_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.mapEither(Codec.FLOAT.fieldOf("zoom"),
                                Codec.FLOAT.listOf(1, Integer.MAX_VALUE).xmap(ImmutableList::copyOf, Function.identity()).fieldOf("zoom_levels"))
                        .xmap(either -> Either.unwrap(either.mapLeft(ImmutableList::of)), Either::right)
                        .forGetter(ScopeAttachmentProperties::zoomLevels),
                Codec.BOOL.optionalFieldOf("blocks_speedloaders", true).forGetter(ScopeAttachmentProperties::blocksSpeedloaders)
        ).apply(o, (zoomLevels, blocksSpeedloaders) -> new ScopeAttachmentProperties(true, zoomLevels, blocksSpeedloaders)));

        private static final MapCodec<ScopeAttachmentProperties> CODEC = Codec.mapEither(OVERRIDE_CODEC, MapCodec.unit(DONT_OVERRIDE_DEFAULTS)).xmap(Either::unwrap, Either::left);

        private static final StreamCodec<RegistryFriendlyByteBuf, ScopeAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ScopeAttachmentProperties::overrideScopeDefaults,
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).map(ImmutableList::copyOf, Function.identity()), ScopeAttachmentProperties::zoomLevels,
                ByteBufCodecs.BOOL, ScopeAttachmentProperties::blocksSpeedloaders,
                ScopeAttachmentProperties::new);

        @Override public MapCodec<ScopeAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ScopeAttachmentProperties> streamCodec() { return STREAM_CODEC; }
    }

}
