package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.recoil_control;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
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

    public boolean isDeployed(ItemStack itemStack) {
        return itemStack.getOrDefault(BuiltInRFEPlugin.RFEDataComponents.DEPLOYED_SETTING, false);
    }

    @Override
    public Optional<AttachmentMenuOptionsText> getAttachmentConfigTextOptions(ItemStack itemStack) {
        List<Component> options = new ArrayList<>();
        boolean deployed = this.isDeployed(itemStack);
        options.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.generic.retracted")
                .withStyle(deployed ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
        options.add(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.generic.deployed")
                .withStyle(deployed ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        return Optional.of(new AttachmentMenuOptionsText(Component.translatable("gui.ritchiesfirearmengine.attachments_menu.option.bipod")
                .withStyle(ChatFormatting.GRAY), options));
    }

    @Override
    public int getAttachmentConfigOption(ItemStack itemStack) {
        return this.isDeployed(itemStack) ? 1 : 0;
    }

    @Override
    public boolean acceptAttachmentConfigOption(ItemStack itemStack, int option) {
        if (option != 0 && option != 1)
            return false;
        itemStack.set(BuiltInRFEPlugin.RFEDataComponents.DEPLOYED_SETTING, option == 1);
        return true;
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
