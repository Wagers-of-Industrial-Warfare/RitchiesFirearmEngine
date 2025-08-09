package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import rbasamoyai.ritchiesfirearmengine.utils.RFEUtils;

public class RFEBaseProjectilePropertiesBuilder {

    public boolean fullHitscan = false;
    public double muzzleVelocity;
    public double drag;
    public boolean quadraticDrag = true;
    public double gravity;
    public int maxAge;
    public float knockback;
    public RFEProjectileDamageModel damageModel;
    public ResourceKey<DamageType> damageTypeKey;

    public static RFEBaseProjectilePropertiesBuilder fromJson(JsonObject obj) {
        RFEBaseProjectilePropertiesBuilder builder = new RFEBaseProjectilePropertiesBuilder();
        builder.fullHitscan = GsonHelper.getAsBoolean(obj, "full_hitscan", false);
        builder.muzzleVelocity = GsonHelper.getAsDouble(obj, builder.fullHitscan ? "hitscan_range" : "muzzle_velocity");
        builder.drag = builder.fullHitscan ? 0 : Mth.clamp(GsonHelper.getAsDouble(obj, "drag"), 0, 1);
        builder.quadraticDrag = !builder.fullHitscan && GsonHelper.getAsBoolean(obj, "quadratic_drag", true);
        builder.gravity = builder.fullHitscan ? 0 : GsonHelper.getAsDouble(obj, "gravity");
        builder.maxAge = builder.fullHitscan ? 0 : GsonHelper.getAsInt(obj, "max_age", 400);
        builder.knockback = Math.max(GsonHelper.getAsFloat(obj, "knockback", 0), 0);

        RFEProjectileDamageModel damageModel = new RFEProjectileDamageModel();
        JsonArray dmgModelArr = GsonHelper.getAsJsonArray(obj, "damage_model");
        for (JsonElement el : dmgModelArr) {
            if (!el.isJsonObject())
                throw new JsonParseException("Expected JSON object for projectile damage model point");
            JsonObject pointObj = el.getAsJsonObject();
            double distance = GsonHelper.getAsDouble(pointObj, "distance");
            double damage = GsonHelper.getAsDouble(pointObj, "damage");
            damageModel.addPoint(distance, damage);
        }
        damageModel.validateDamageModel();
        builder.damageModel = damageModel;

        builder.damageTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, RFEUtils.location(GsonHelper.getAsString(obj, "damage_type")));

        return builder;
    }

    public static RFEBaseProjectilePropertiesBuilder fromNetwork(FriendlyByteBuf buf) {
        RFEBaseProjectilePropertiesBuilder builder = new RFEBaseProjectilePropertiesBuilder();
        builder.fullHitscan = buf.readBoolean();
        builder.muzzleVelocity = buf.readDouble();
        builder.drag = buf.readDouble();
        builder.quadraticDrag = buf.readBoolean();
        builder.gravity = buf.readDouble();
        builder.maxAge = buf.readVarInt();
        builder.knockback = buf.readFloat();
        builder.damageModel = RFEProjectileDamageModel.fromNetwork(buf);
        builder.damageTypeKey = buf.readResourceKey(Registries.DAMAGE_TYPE);
        return builder;
    }
    
    public static void toNetwork(FriendlyByteBuf buf, RFEBaseProjectilePropertiesBuilder builder) {
        buf.writeBoolean(builder.fullHitscan)
                .writeDouble(builder.muzzleVelocity)
                .writeDouble(builder.drag)
                .writeBoolean(builder.quadraticDrag)
                .writeDouble(builder.gravity);
        buf.writeVarInt(builder.maxAge)
                .writeFloat(builder.knockback);
        RFEProjectileDamageModel.toNetwork(buf, builder.damageModel);
        buf.writeResourceKey(builder.damageTypeKey);
    }

}
