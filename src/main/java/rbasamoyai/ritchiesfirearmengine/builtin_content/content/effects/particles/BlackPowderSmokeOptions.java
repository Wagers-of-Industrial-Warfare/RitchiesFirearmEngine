package rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;

public record BlackPowderSmokeOptions(float scale) implements ParticleOptions {

    public static final MapCodec<BlackPowderSmokeOptions> CODEC = Codec.FLOAT.fieldOf("scale")
            .xmap(BlackPowderSmokeOptions::new, BlackPowderSmokeOptions::scale);

    public static final StreamCodec<RegistryFriendlyByteBuf, BlackPowderSmokeOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BlackPowderSmokeOptions::scale,
            BlackPowderSmokeOptions::new);

    @Override public ParticleType<?> getType() { return BuiltInRFEPlugin.ParticleTypes.BLACK_POWDER_SMOKE; }

}
