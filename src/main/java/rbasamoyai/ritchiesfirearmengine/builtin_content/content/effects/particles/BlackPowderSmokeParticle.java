package rbasamoyai.ritchiesfirearmengine.builtin_content.content.effects.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public class BlackPowderSmokeParticle extends TextureSheetParticle {

    private final float scale;

    BlackPowderSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float scale) {
        super(level, x, y, z);
        this.scale = scale;
        this.setParticleSpeed(dx, dy, dz);
        this.quadSize = this.scale * 0.1f;
        this.alpha = 1;
        this.lifetime = 140 + (int) Math.ceil(level.random.nextFloat() * 20);
        this.gravity = 5e-5f;
        //this.friction = 0.99f;
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize = Math.min(this.scale, this.quadSize * 3f);
        this.alpha *= 0.99f;
        if (this.age > 40)
            this.friction = 1;
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public static class SpriteRegistration implements ParticleEngine.SpriteParticleRegistration<BlackPowderSmokeOptions> {
        @Override public ParticleProvider<BlackPowderSmokeOptions> create(SpriteSet sprites) { return new Provider(sprites); }
    }

    private record Provider(SpriteSet sprites) implements ParticleProvider<BlackPowderSmokeOptions> {
        @Override
        public Particle createParticle(BlackPowderSmokeOptions type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            BlackPowderSmokeParticle particle = new BlackPowderSmokeParticle(level, x, y, z, dx, dy, dz, type.scale());
            particle.pickSprite(this.sprites);
            return particle;
        }
    }

}
