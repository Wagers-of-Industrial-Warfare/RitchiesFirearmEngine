package rbasamoyai.ritchiesfirearmengine.builtin_content.content.item_attachments.bayonets;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.item_attachments.properties.RFEItemAttachmentProperties;

public record BayonetAttachmentProperties(boolean overrideBayonetDefaults, float addedAttackDamage, float addedAttackSpeed,
                                          float addedAttackRange, boolean blocksShooting) implements RFEItemAttachmentProperties {

    public static final BayonetAttachmentProperties DONT_OVERRIDE_DEFAULTS = new BayonetAttachmentProperties(false, 0, 0, 0, false);
    
    @Override public boolean overridesDefaults() { return this.overrideBayonetDefaults; }

    @Override
    public RFEItemAttachmentProperties.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.AttachmentSlots.BAYONET;
    }

    public static class Serializer implements RFEItemAttachmentProperties.Serializer<BayonetAttachmentProperties> {
        private static final MapCodec<BayonetAttachmentProperties> OVERRIDE_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.FLOAT.fieldOf("added_attack_damage").forGetter(BayonetAttachmentProperties::addedAttackDamage),
                Codec.FLOAT.fieldOf("added_attack_speed").forGetter(BayonetAttachmentProperties::addedAttackSpeed),
                Codec.FLOAT.optionalFieldOf("added_attack_range", 0f).forGetter(BayonetAttachmentProperties::addedAttackRange),
                Codec.BOOL.optionalFieldOf("blocks_shooting", false).forGetter(BayonetAttachmentProperties::blocksShooting)
        ).apply(o, (addedAttackDamage, addedAttackSpeed, addedAttackRange, blocksShooting) ->
                new BayonetAttachmentProperties(true, addedAttackDamage, addedAttackSpeed, addedAttackRange, blocksShooting)));

        private static final MapCodec<BayonetAttachmentProperties> CODEC = Codec.mapEither(OVERRIDE_CODEC, MapCodec.unit(DONT_OVERRIDE_DEFAULTS)).xmap(Either::unwrap, Either::left);

        private static final StreamCodec<RegistryFriendlyByteBuf, BayonetAttachmentProperties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, BayonetAttachmentProperties::overrideBayonetDefaults,
                ByteBufCodecs.FLOAT, BayonetAttachmentProperties::addedAttackDamage,
                ByteBufCodecs.FLOAT, BayonetAttachmentProperties::addedAttackSpeed,
                ByteBufCodecs.FLOAT, BayonetAttachmentProperties::addedAttackRange,
                ByteBufCodecs.BOOL, BayonetAttachmentProperties::blocksShooting,
                BayonetAttachmentProperties::new);

        @Override public MapCodec<BayonetAttachmentProperties> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BayonetAttachmentProperties> streamCodec() { return STREAM_CODEC; }
    }

}
