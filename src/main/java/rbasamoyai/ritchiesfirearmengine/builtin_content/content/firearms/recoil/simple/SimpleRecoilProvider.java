package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.simple;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;

public record SimpleRecoilProvider(float verticalRecoil, float horizontalRecoil, float shake, float recoilDecrease) implements RFERecoilProvider {

    @Override
    public RFERecoilInstance createRecoilInstance(ItemStack itemStack, LivingEntity entity, RandomSource random) {
        return new SimpleRecoilInstance(this.verticalRecoil, this.horizontalRecoil, this.shake, this.recoilDecrease);
    }

    @Override
    public RFERecoilProvider.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.RecoilProviders.SIMPLE;
    }

    public static class Serializer implements RFERecoilProvider.Serializer<SimpleRecoilProvider> {
        private static final MapCodec<SimpleRecoilProvider> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.FLOAT.fieldOf("vertical_recoil").forGetter(SimpleRecoilProvider::verticalRecoil),
                Codec.FLOAT.fieldOf("horizontal_recoil").forGetter(SimpleRecoilProvider::horizontalRecoil),
                Codec.floatRange(0f, Float.MAX_VALUE).optionalFieldOf("camera_shake", 0f).forGetter(SimpleRecoilProvider::shake),
                Codec.floatRange(0f, Float.MAX_VALUE).fieldOf("recoil_decrease").forGetter(SimpleRecoilProvider::recoilDecrease)
        ).apply(o, SimpleRecoilProvider::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SimpleRecoilProvider> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, SimpleRecoilProvider::verticalRecoil,
                ByteBufCodecs.FLOAT, SimpleRecoilProvider::horizontalRecoil,
                ByteBufCodecs.FLOAT, SimpleRecoilProvider::shake,
                ByteBufCodecs.FLOAT, SimpleRecoilProvider::recoilDecrease,
                SimpleRecoilProvider::new);

        @Override public MapCodec<SimpleRecoilProvider> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SimpleRecoilProvider> streamCodec() { return STREAM_CODEC; }
    }

}
