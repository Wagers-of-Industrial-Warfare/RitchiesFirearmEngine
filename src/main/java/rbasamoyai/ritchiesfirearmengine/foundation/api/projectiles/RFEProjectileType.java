package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

public interface RFEProjectileType {

    void tick(Level level, RFEProjectileInstance instance);

    Serializer<?> getSerializer();

    interface Serializer<T extends RFEProjectileType> {
        T fromJson(JsonObject obj);
        T fromNetwork(FriendlyByteBuf buf);
        void toNetwork(FriendlyByteBuf buf, T type);
    }

}
