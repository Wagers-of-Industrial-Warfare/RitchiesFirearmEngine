package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public interface RFEProjectileType {

    void shoot(RFEProjectileInstance instance, double dx, double dy, double dz /* TODO spread provider */);

    void tick(Level level, RFEProjectileInstance instance);

    AABB getAABB(Level level, RFEProjectileInstance instance);

    Serializer<?> getSerializer();

    default RFEProjectileInstance createInstance() { return new RFEProjectileInstance(this); }

    interface Serializer<T extends RFEProjectileType> {
        T fromJson(JsonObject obj);
        T fromNetwork(FriendlyByteBuf buf);
        void toNetwork(FriendlyByteBuf buf, T type);
    }

}
