package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.random;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

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
        @Override
        public SimpleSpreadProvider fromJson(JsonObject obj) {
            float spread = GsonHelper.getAsFloat(obj, "spread");
            float unaimedSpread = GsonHelper.getAsFloat(obj, "unaimed_spread", spread);
            boolean tighten = GsonHelper.getAsBoolean(obj, "tighten", false);
            return new SimpleSpreadProvider(spread, unaimedSpread, tighten);
        }

        @Override
        public SimpleSpreadProvider fromNetwork(FriendlyByteBuf buf) {
            return new SimpleSpreadProvider(buf.readFloat(), buf.readFloat(), buf.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SimpleSpreadProvider prov) {
            buf.writeFloat(prov.radius)
                    .writeFloat(prov.unaimedRadius)
                    .writeBoolean(prov.tighten);
        }
    }

}
