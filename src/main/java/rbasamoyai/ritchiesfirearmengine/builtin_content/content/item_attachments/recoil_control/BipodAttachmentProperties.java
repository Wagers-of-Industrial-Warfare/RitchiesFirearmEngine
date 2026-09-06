package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.recoil_control;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin.RFEDataComponents;
import rbasamoyai.ritchiesfirearmengine.foundation.api.RFEFirearmProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BipodAttachmentProperties(Optional<RFEFirearmProperties<RFERecoilProvider>> recoilProperties,
                                        Optional<RFEFirearmProperties<RFESpreadProvider>> spreadProperties) implements RFEItemAttachmentProperties {

    @Override public boolean overridesDefaults() { return true; }

    @Override
    public boolean isActive(DataComponentPatch data) {
        Optional<? extends Boolean> op = data.get(RFEDataComponents.DEPLOYED_SETTING);
        return op != null && op.isPresent() ? op.get() : false;
    }

    @Override
    public Optional<AttachmentMenuOptionsText> getAttachmentConfigTextOptions(ItemStack itemStack) {
        return innerConfigText(this.isActive(itemStack));
    }

    @Override
    public Optional<AttachmentMenuOptionsText> getIntegralAttachmentConfigTextOptions(DataComponentPatch data) {
        return innerConfigText(this.isActive(data));
    }

    private static Optional<AttachmentMenuOptionsText> innerConfigText(boolean deployed) {
        List<Component> options = new ArrayList<>();
        options.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.generic.retracted")
                .withStyle(deployed ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
        options.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.generic.deployed")
                .withStyle(deployed ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        return Optional.of(new AttachmentMenuOptionsText(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.bipod")
                .withStyle(ChatFormatting.GRAY), options));
    }

    @Override
    public int getAttachmentConfigOption(ItemStack itemStack) {
        return this.isActive(itemStack) ? 1 : 0;
    }

    @Override
    public int getIntegralAttachmentConfigOption(DataComponentPatch data) {
        return this.isActive(data) ? 1 : 0;
    }

    @Override
    public boolean acceptAttachmentConfigOption(ItemStack itemStack, int option) {
        if (option != 0 && option != 1)
            return false;
        itemStack.set(RFEDataComponents.DEPLOYED_SETTING, option == 1);
        return true;
    }

    @Override
    public Optional<DataComponentPatch> acceptIntegralAttachmentConfigOption(DataComponentPatch data, int option) {
        if (option != 0 && option != 1)
            return Optional.empty();
        PatchedDataComponentMap patched = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, data);
        if (option == 1) {
            patched.set(RFEDataComponents.DEPLOYED_SETTING, true);
        } else {
            patched.remove(RFEDataComponents.DEPLOYED_SETTING);
        }
        return Optional.of(patched.asPatch());
    }

    @Override public RFEItemAttachmentProperties.Serializer<?> getSerializer() { return BuiltInRFEPlugin.AttachmentSlots.BIPOD; }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<BipodAttachmentProperties> {
        private static final MapCodec<BipodAttachmentProperties> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                RFEFirearmProperties.makeCodec(RFERecoilProvider.CODEC).optionalFieldOf("recoil").forGetter(BipodAttachmentProperties::recoilProperties),
                RFEFirearmProperties.makeCodec(RFESpreadProvider.CODEC).optionalFieldOf("spread").forGetter(BipodAttachmentProperties::spreadProperties)
        ).apply(o, BipodAttachmentProperties::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BipodAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(RFEFirearmProperties.makeStreamCodec(RFERecoilProvider.STREAM_CODEC)), BipodAttachmentProperties::recoilProperties,
                ByteBufCodecs.optional(RFEFirearmProperties.makeStreamCodec(RFESpreadProvider.STREAM_CODEC)), BipodAttachmentProperties::spreadProperties,
                BipodAttachmentProperties::new);

        @Override public MapCodec<BipodAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BipodAttachmentProperties> streamCodec() { return STREAM_CODEC; }
    }

}
