package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.recoil.RFERecoilProvider;

public class NoRecoilProvider implements RFERecoilProvider {

    public static final NoRecoilProvider INSTANCE = new NoRecoilProvider();

    private NoRecoilProvider() {}

    @Override
    public RFERecoilInstance createRecoilInstance(ItemStack itemStack, LivingEntity entity, RandomSource random) {
        return NoRecoilInstance.INSTANCE;
    }

    @Override
    public RFERecoilProvider.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.RecoilProviders.NO_RECOIL;
    }

    public static class Serializer implements RFERecoilProvider.Serializer<NoRecoilProvider> {
        private static final MapCodec<NoRecoilProvider> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, NoRecoilProvider> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override public MapCodec<NoRecoilProvider> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, NoRecoilProvider> streamCodec() { return STREAM_CODEC; }
    }

}
