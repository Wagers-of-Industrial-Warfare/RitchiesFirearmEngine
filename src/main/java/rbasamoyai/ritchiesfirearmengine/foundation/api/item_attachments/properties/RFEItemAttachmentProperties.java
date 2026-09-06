package rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface RFEItemAttachmentProperties {

    MapCodec<RFEItemAttachmentProperties> CODEC = ResourceLocation.CODEC
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getItemAttachmentSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment type: " + e.getMessage());
                        }
                    },
                    ser -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getItemAttachmentSerializerId(ser));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving item attachment type id: " + e.getMessage());
                        }
                    })
            .dispatchMap(RFEItemAttachmentProperties::getSerializer, Serializer::codec);

    StreamCodec<RegistryFriendlyByteBuf, RFEItemAttachmentProperties> STREAM_CODEC = ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
            .<Serializer<?>>map(RFEContentBuilderRegistry::getItemAttachmentSerializer, RFEContentBuilderRegistry::getItemAttachmentSerializerId)
            .dispatch(RFEItemAttachmentProperties::getSerializer, Serializer::streamCodec);

    // TODO documentation
    default Optional<AttachmentMenuOptionsText> getAttachmentConfigTextOptions(ItemStack itemStack) { return Optional.empty(); }

    // TODO documentation
    default int getAttachmentConfigOption(ItemStack itemStack) { return -1; }

    // TODO documentation
    default boolean acceptAttachmentConfigOption(ItemStack itemStack, int option) { return false; }

    // TODO documentation
    default Optional<AttachmentMenuOptionsText> getIntegralAttachmentConfigTextOptions(DataComponentPatch data) { return Optional.empty(); }

    // TODO documentation
    default int getIntegralAttachmentConfigOption(DataComponentPatch data) { return -1; }

    // TODO documentation
    default Optional<DataComponentPatch> acceptIntegralAttachmentConfigOption(DataComponentPatch data, int option) { return Optional.empty(); }

    boolean overridesDefaults();

    default boolean isActive(DataComponentPatch data) { return true; }

    default boolean isActive(ItemStack itemStack) { return this.isActive(itemStack.getComponentsPatch()); }

    Serializer<?> getSerializer();

    interface Serializer<T extends RFEItemAttachmentProperties> {
        MapCodec<T> codec();
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
    }

    record AttachmentMenuOptionsText(Component heading, List<Component> optionComponents) {
    }

    record AttachmentTooltipContext(Item.TooltipContext wrapped, boolean overrideDefaults, boolean inAttachmentsScreen) implements Item.TooltipContext {
        @Nullable
        @Override
        public HolderLookup.Provider registries() { return this.wrapped.registries(); }

        @Override public float tickRate() { return this.wrapped.tickRate(); }

        @Nullable
        @Override
        public MapItemSavedData mapData(MapId mapId) { return this.wrapped.mapData(mapId); }
    }

}
