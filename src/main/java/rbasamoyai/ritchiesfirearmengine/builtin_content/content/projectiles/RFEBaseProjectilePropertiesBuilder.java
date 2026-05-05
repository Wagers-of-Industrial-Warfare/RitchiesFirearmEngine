package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class RFEBaseProjectilePropertiesBuilder {

    private static final MapCodec<RFEBaseProjectilePropertiesBuilder> COMMON_FIELDS_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("knockback", 0f).forGetter(b -> b.knockback),
            RFEProjectileDamageModel.CODEC.fieldOf("damage_model").forGetter(b -> b.damageModel),
            ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf("damage_type").forGetter(b -> b.damageTypeKey),
            ResourceLocation.CODEC.optionalFieldOf("hit_multiplier").forGetter(b -> Optional.ofNullable(b.hitMultiplierId)),
            ResourceLocation.CODEC.optionalFieldOf("penetration").forGetter(b -> Optional.ofNullable(b.penetrationId)),
            Codec.floatRange(0f, 10f).optionalFieldOf("smoke", 0f).forGetter(b -> b.smoke),
            ResourceLocation.CODEC.xmap(SoundEvent::createVariableRangeEvent, SoundEvent::getLocation).optionalFieldOf("pass_sound").forGetter(b -> Optional.ofNullable(b.passSound))
    ).apply(o, RFEBaseProjectilePropertiesBuilder::common));

    private static final MapCodec<RFEBaseProjectilePropertiesBuilder> NO_HITSCAN_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
            Codec.DOUBLE.fieldOf("muzzle_velocity").forGetter(b -> b.muzzleVelocity),
            Codec.doubleRange(0d, 1d).fieldOf("drag").forGetter(b -> b.drag),
            Codec.BOOL.optionalFieldOf("quadratic_drag", true).forGetter(b -> b.quadraticDrag),
            Codec.DOUBLE.fieldOf("gravity").forGetter(b -> b.gravity),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max_age", 400).forGetter(b -> b.maxAge),
            COMMON_FIELDS_CODEC.forGetter(Function.identity())).apply(o, RFEBaseProjectilePropertiesBuilder::noHitscan));

    private static final MapCodec<RFEBaseProjectilePropertiesBuilder> WITH_HITSCAN_CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
            Codec.DOUBLE.fieldOf("hitscan_range").forGetter(b -> b.muzzleVelocity),
            COMMON_FIELDS_CODEC.forGetter(Function.identity())).apply(o, RFEBaseProjectilePropertiesBuilder::withHitscan));

    public static final MapCodec<RFEBaseProjectilePropertiesBuilder> CODEC = Codec.mapEither(
            Codec.BOOL.dispatchMap("full_hitscan", b -> b.fullHitscan, hitscan -> hitscan ? WITH_HITSCAN_CODEC : NO_HITSCAN_CODEC), NO_HITSCAN_CODEC)
            .xmap(Either::unwrap, b -> b.fullHitscan ? Either.left(b) : Either.right(b));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFEBaseProjectilePropertiesBuilder> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RFEBaseProjectilePropertiesBuilder decode(RegistryFriendlyByteBuf buf) {
                    RFEBaseProjectilePropertiesBuilder builder = new RFEBaseProjectilePropertiesBuilder();
                    builder.fullHitscan = buf.readBoolean();
                    builder.muzzleVelocity = buf.readDouble();
                    builder.drag = buf.readDouble();
                    builder.quadraticDrag = buf.readBoolean();
                    builder.gravity = buf.readDouble();
                    builder.maxAge = buf.readVarInt();
                    builder.knockback = buf.readFloat();
                    builder.damageModel = RFEProjectileDamageModel.STREAM_CODEC.decode(buf);
                    builder.damageTypeKey = buf.readResourceKey(Registries.DAMAGE_TYPE);
                    builder.hitMultiplierId = buf.readBoolean() ? buf.readResourceLocation() : null;
                    builder.penetrationId = buf.readBoolean() ? buf.readResourceLocation() : null;
                    builder.smoke = buf.readFloat();
                    builder.passSound = buf.readBoolean() ? SoundEvent.createVariableRangeEvent(buf.readResourceLocation()) : null;
                    return builder;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RFEBaseProjectilePropertiesBuilder builder) {
                    buf.writeBoolean(builder.fullHitscan)
                            .writeDouble(builder.muzzleVelocity)
                            .writeDouble(builder.drag)
                            .writeBoolean(builder.quadraticDrag)
                            .writeDouble(builder.gravity);
                    buf.writeVarInt(builder.maxAge)
                            .writeFloat(builder.knockback);
                    RFEProjectileDamageModel.STREAM_CODEC.encode(buf, builder.damageModel);
                    buf.writeResourceKey(builder.damageTypeKey);
                    buf.writeBoolean(builder.hitMultiplierId != null);
                    if (builder.hitMultiplierId != null)
                        buf.writeResourceLocation(builder.hitMultiplierId);
                    buf.writeBoolean(builder.penetrationId != null);
                    if (builder.penetrationId != null)
                        buf.writeResourceLocation(builder.penetrationId);
                    buf.writeFloat(builder.smoke)
                            .writeBoolean(builder.passSound != null);
                    if (builder.passSound != null)
                        buf.writeResourceLocation(builder.passSound.getLocation());
                }
            };

    public boolean fullHitscan = false;
    public double muzzleVelocity;
    public double drag;
    public boolean quadraticDrag = true;
    public double gravity;
    public int maxAge;
    public float knockback;
    public RFEProjectileDamageModel damageModel;
    public ResourceKey<DamageType> damageTypeKey;
    @Nullable public ResourceLocation hitMultiplierId = null;
    @Nullable public ResourceLocation penetrationId = null;
    public float smoke;
    @Nullable public SoundEvent passSound = null;

    public RFEBaseProjectilePropertiesBuilder() {}

    private RFEBaseProjectilePropertiesBuilder(boolean fullHitscan, double muzzleVelocity, double drag,
                                               boolean quadraticDrag, double gravity, int maxAge, float knockback,
                                               RFEProjectileDamageModel damageModel, ResourceKey<DamageType> damageTypeKey,
                                               Optional<ResourceLocation> hitMultiplierId, Optional<ResourceLocation> penetrationId,
                                               float smoke, Optional<SoundEvent> passSound) {
        this.fullHitscan = fullHitscan;
        this.muzzleVelocity = muzzleVelocity;
        this.drag = drag;
        this.quadraticDrag = quadraticDrag;
        this.gravity = gravity;
        this.maxAge = maxAge;
        this.knockback = knockback;
        this.damageModel = damageModel;
        this.damageTypeKey = damageTypeKey;
        this.hitMultiplierId = hitMultiplierId.orElse(null);
        this.penetrationId = penetrationId.orElse(null);
        this.smoke = smoke;
        this.passSound = passSound.orElse(null);
    }

    private static RFEBaseProjectilePropertiesBuilder noHitscan(double muzzleVelocity, double drag, boolean quadraticDrag,
                                                                double gravity, int maxAge, RFEBaseProjectilePropertiesBuilder builder) {
        builder.fullHitscan = false;
        builder.muzzleVelocity = muzzleVelocity;
        builder.drag = drag;
        builder.quadraticDrag = quadraticDrag;
        builder.gravity = gravity;
        builder.maxAge = maxAge;
        return builder;
    }

    private static RFEBaseProjectilePropertiesBuilder withHitscan(double hitscanRange, RFEBaseProjectilePropertiesBuilder builder) {
        builder.fullHitscan = true;
        builder.muzzleVelocity = hitscanRange;
        builder.drag = 0;
        builder.quadraticDrag = false;
        builder.gravity = 0;
        builder.maxAge = 0;
        return builder;
    }

    private static RFEBaseProjectilePropertiesBuilder common(float knockback, RFEProjectileDamageModel damageModel,
                                                             ResourceKey<DamageType> damageTypeKey,
                                                             Optional<ResourceLocation> hitMultiplierId,
                                                             Optional<ResourceLocation> penetrationId,
                                                             float smoke, Optional<SoundEvent> passSound) {
        RFEBaseProjectilePropertiesBuilder builder = new RFEBaseProjectilePropertiesBuilder();
        builder.knockback = knockback;
        builder.damageModel = damageModel;
        builder.damageTypeKey = damageTypeKey;
        builder.hitMultiplierId = hitMultiplierId.orElse(null);
        builder.penetrationId = penetrationId.orElse(null);
        builder.smoke = smoke;
        builder.passSound = passSound.orElse(null);
        return builder;
    }

}
