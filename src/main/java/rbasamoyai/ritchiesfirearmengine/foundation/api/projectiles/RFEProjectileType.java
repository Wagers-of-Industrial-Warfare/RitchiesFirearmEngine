package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;

import java.util.List;

public interface RFEProjectileType {

    void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack, LivingEntity entity,
               RFESpreadInstance spreadInstance);

    void tick(Level level, RFEProjectileInstance instance);

    AABB getAABB(Level level, RFEProjectileInstance instance);

    Serializer<?> getSerializer();

    default RFEProjectileInstance createInstance() { return new RFEProjectileInstance(this); }

    interface Serializer<T extends RFEProjectileType> {
        T fromJson(JsonObject obj);
        T fromNetwork(FriendlyByteBuf buf);
        void toNetwork(FriendlyByteBuf buf, T type);
    }

    interface HasCombinedProjectiles {
        List<RFEProjectileType> getSubprojectileTypes();
    }

}
