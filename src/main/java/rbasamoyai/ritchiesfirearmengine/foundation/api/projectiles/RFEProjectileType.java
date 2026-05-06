package rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import rbasamoyai.ritchiesfirearmengine.foundation.api.content_creation.RFEContentBuilderRegistry;
import rbasamoyai.ritchiesfirearmengine.foundation.api.spread.RFESpreadInstance;

import java.util.Map;

public interface RFEProjectileType {

    Codec<RFEProjectileType> CODEC = ResourceLocation.CODEC
            .<Serializer<?>>flatXmap(
                    rl -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getProjectileTypeSerializer(rl));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving projectile type serializer: " + e.getMessage());
                        }
                    },
                    ser -> {
                        try {
                            return DataResult.success(RFEContentBuilderRegistry.getProjectileTypeSerializerId(ser));
                        } catch (Exception e) {
                            return DataResult.error(() -> "Error retrieving projectile type serializer id: " + e.getMessage());
                        }
                    })
            .dispatch(RFEProjectileType::getSerializer, Serializer::codec);

    StreamCodec<RegistryFriendlyByteBuf, RFEProjectileType> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                    .<Serializer<?>>map(RFEContentBuilderRegistry::getProjectileTypeSerializer, RFEContentBuilderRegistry::getProjectileTypeSerializerId)
                    .dispatch(RFEProjectileType::getSerializer, Serializer::streamCodec);

    void shoot(RFEProjectileInstance instance, double dx, double dy, double dz, ItemStack itemStack, LivingEntity entity,
               RFESpreadInstance spreadInstance);

    void shootWithoutEntity(RFEProjectileInstance instance, double dx, double dy, double dz, Level level);

    void tick(Level level, RFEProjectileInstance instance);

    AABB getAABB(Level level, RFEProjectileInstance instance);

    Serializer<?> getSerializer();

    default RFEProjectileInstance createInstance() { return new RFEProjectileInstance(this); }

    interface Serializer<T extends RFEProjectileType> {
        MapCodec<T> codec();
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
    }

    interface HasCombinedProjectiles {
        Map<String, RFEProjectileType> getSubprojectileTypes();
    }

}
