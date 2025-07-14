package rbasamoyai.ritchiesfirearmengine.foundation.api.spread;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface RFESpreadProvider {

    RFESpreadInstance createSpreadInstance(ItemStack itemStack, LivingEntity entity, RandomSource random);

    Serializer<?> getSerializer();

    interface Serializer<T extends RFESpreadProvider> {
        T fromJson(JsonObject obj);
        T fromNetwork(FriendlyByteBuf buf);
        void toNetwork(FriendlyByteBuf buf, T prov);
    }

}
