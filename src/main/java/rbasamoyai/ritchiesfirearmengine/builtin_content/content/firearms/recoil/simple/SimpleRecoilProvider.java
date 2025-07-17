package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.recoil.simple;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
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
        @Override
        public SimpleRecoilProvider fromJson(JsonObject obj) {
            float verticalRecoil = GsonHelper.getAsFloat(obj, "vertical_recoil");
            float horizontalRecoil = GsonHelper.getAsFloat(obj, "horizontal_recoil");
            float shake = GsonHelper.getAsFloat(obj, "camera_shake", 0);
            float recoilDecrease = Math.max(0, GsonHelper.getAsFloat(obj, "recoil_decrease"));
            return new SimpleRecoilProvider(verticalRecoil, horizontalRecoil, shake, recoilDecrease);
        }

        @Override
        public SimpleRecoilProvider fromNetwork(FriendlyByteBuf buf) {
            return new SimpleRecoilProvider(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SimpleRecoilProvider prov) {
            buf.writeFloat(prov.verticalRecoil)
                    .writeFloat(prov.horizontalRecoil)
                    .writeFloat(prov.shake)
                    .writeFloat(prov.recoilDecrease);
        }
    }

}
