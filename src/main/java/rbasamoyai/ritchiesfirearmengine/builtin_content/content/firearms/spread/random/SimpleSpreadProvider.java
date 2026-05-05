package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.random;

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
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

import java.util.Optional;

public record SimpleSpreadProvider(float radius, float unaimedRadius, boolean tighten) implements RFESpreadProvider {

    @Override
    public RFESpreadInstance createSpreadInstance(ItemStack itemStack, LivingEntity entity, RandomSource random) {
        return new SimpleSpreadInstance(this.radius, this.unaimedRadius, this.tighten);
    }

    @Override
    public RFESpreadProvider.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.SpreadProviders.SIMPLE;
    }

    public static class Serializer implements RFESpreadProvider.Serializer<SimpleSpreadProvider> {
        private static final MapCodec<SimpleSpreadProvider> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("spread").forGetter(SimpleSpreadProvider::radius),
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("unaimed_spread").forGetter(p -> Optional.of(p.unaimedRadius)),
                Codec.BOOL.optionalFieldOf("tighten", false).forGetter(SimpleSpreadProvider::tighten)
        ).apply(o, (r, ur, t) -> new SimpleSpreadProvider(r, ur.orElse(r), t)));

        private static final StreamCodec<RegistryFriendlyByteBuf, SimpleSpreadProvider> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, SimpleSpreadProvider::radius,
                ByteBufCodecs.FLOAT, SimpleSpreadProvider::unaimedRadius,
                ByteBufCodecs.BOOL, SimpleSpreadProvider::tighten,
                SimpleSpreadProvider::new);

        @Override public MapCodec<SimpleSpreadProvider> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SimpleSpreadProvider> streamCodec() { return STREAM_CODEC; }
    }

}
