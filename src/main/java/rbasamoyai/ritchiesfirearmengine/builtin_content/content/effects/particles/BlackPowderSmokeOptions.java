package rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import rbasamoyai.ritchiesfirearmengine.builtin_content.default_index.BuiltInRFEPlugin;

public record BlackPowderSmokeOptions(float scale) implements ParticleOptions {

    public static final ParticleOptions.Deserializer<BlackPowderSmokeOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public BlackPowderSmokeOptions fromCommand(ParticleType<BlackPowderSmokeOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return new BlackPowderSmokeOptions(reader.readFloat());
        }

        @Override
        public BlackPowderSmokeOptions fromNetwork(ParticleType<BlackPowderSmokeOptions> type, FriendlyByteBuf buf) {
            return new BlackPowderSmokeOptions(buf.readFloat());
        }
    };

    public static final Codec<BlackPowderSmokeOptions> CODEC = Codec.FLOAT.fieldOf("scale")
            .xmap(BlackPowderSmokeOptions::new, BlackPowderSmokeOptions::scale).codec();

    @Override public ParticleType<?> getType() { return BuiltInRFEPlugin.ParticleTypes.BLACK_POWDER_SMOKE; }

    @Override public void writeToNetwork(FriendlyByteBuf buf) { buf.writeFloat(this.scale); }

    @Override public String writeToString() { return String.format("%f", this.scale); }

}
