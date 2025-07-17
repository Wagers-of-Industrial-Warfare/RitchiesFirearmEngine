package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.no_recoil;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
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
        @Override public NoRecoilProvider fromJson(JsonObject obj) { return INSTANCE; }
        @Override public NoRecoilProvider fromNetwork(FriendlyByteBuf buf) { return INSTANCE; }
        @Override public void toNetwork(FriendlyByteBuf buf, NoRecoilProvider prov) {}
    }

}
