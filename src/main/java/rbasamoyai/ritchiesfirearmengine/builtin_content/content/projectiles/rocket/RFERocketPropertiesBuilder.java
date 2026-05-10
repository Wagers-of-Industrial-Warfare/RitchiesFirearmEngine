package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.Optional;

public class RFERocketPropertiesBuilder {

    public static final MapCodec<RFERocketPropertiesBuilder> CODEC = RecordCodecBuilder.mapCodec(o -> o.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("rocket_activation_time", 0).forGetter(t -> t.rocketActivationTime),
            Codec.DOUBLE.fieldOf("acceleration").forGetter(t -> t.acceleration),
            Codec.doubleRange(0d, Double.MAX_VALUE).fieldOf("terminal_velocity").forGetter(t -> t.terminalVelocity),
            Codec.BOOL.optionalFieldOf("rocket_smoke", false).forGetter(t -> t.rocketSmoke),
            ResourceLocation.CODEC.xmap(SoundEvent::createVariableRangeEvent, SoundEvent::getLocation).optionalFieldOf("rocket_sound").forGetter(t -> Optional.ofNullable(t.rocketSound))
    ).apply(o, RFERocketPropertiesBuilder::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, RFERocketPropertiesBuilder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, t -> t.rocketActivationTime,
            ByteBufCodecs.DOUBLE, t -> t.acceleration,
            ByteBufCodecs.DOUBLE, t -> t.terminalVelocity,
            ByteBufCodecs.BOOL, t -> t.rocketSmoke,
            ByteBufCodecs.optional(SoundEvent.STREAM_CODEC)
                    .map(o -> o.map(Holder::value).orElse(null), s -> s == null ? Optional.empty() : Optional.of(Holder.direct(s))), t -> t.rocketSound,
            RFERocketPropertiesBuilder::fromStreamCodec);

    public int rocketActivationTime;
    public double acceleration;
    public double terminalVelocity;
    public boolean rocketSmoke;
    @Nullable public SoundEvent rocketSound;

    public RFERocketPropertiesBuilder() {}

    private static RFERocketPropertiesBuilder fromCodec(int rocketActivationTime, double acceleration, double terminalVelocity,
                                                        boolean rocketSmoke, Optional<SoundEvent> rocketSound) {
        RFERocketPropertiesBuilder properties = new RFERocketPropertiesBuilder();
        properties.rocketActivationTime = rocketActivationTime;
        properties.acceleration = acceleration;
        properties.terminalVelocity = terminalVelocity;
        properties.rocketSmoke = rocketSmoke;
        properties.rocketSound = rocketSound.orElse(null);
        return properties;
    }

    private static RFERocketPropertiesBuilder fromStreamCodec(int rocketActivationTime, double acceleration, double terminalVelocity,
                                                              boolean rocketSmoke, @Nullable SoundEvent rocketSound) {
        RFERocketPropertiesBuilder properties = new RFERocketPropertiesBuilder();
        properties.rocketActivationTime = rocketActivationTime;
        properties.acceleration = acceleration;
        properties.terminalVelocity = terminalVelocity;
        properties.rocketSmoke = rocketSmoke;
        properties.rocketSound = rocketSound;
        return properties;
    }

}
