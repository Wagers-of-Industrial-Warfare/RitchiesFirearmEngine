package rbasamoyai.ritchiesfirearmengine.builtin_content.content.firearms.spread.no_spread;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadProvider;

public class NoSpreadProvider implements RFESpreadProvider {

    public static final NoSpreadProvider INSTANCE = new NoSpreadProvider();

    @Override
    public RFESpreadInstance createSpreadInstance(ItemStack itemStack, LivingEntity entity, RandomSource random) {
        return NoSpreadInstance.INSTANCE;
    }

    @Override
    public RFESpreadProvider.Serializer<?> getSerializer() {
        return BuiltInRFEPlugin.SpreadProviders.NO_SPREAD;
    }

    public static class Serializer implements RFESpreadProvider.Serializer<NoSpreadProvider> {
        @Override public NoSpreadProvider fromJson(JsonObject obj) { return INSTANCE; }
        @Override public NoSpreadProvider fromNetwork(FriendlyByteBuf buf) { return INSTANCE; }
        @Override public void toNetwork(FriendlyByteBuf buf, NoSpreadProvider prov) {}
    }

}
