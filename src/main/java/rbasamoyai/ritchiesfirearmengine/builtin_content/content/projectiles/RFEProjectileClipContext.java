package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;

import java.util.HashMap;
import java.util.Map;

public class RFEProjectileClipContext extends ClipContext {

    public final Map<BlockPos, BlockState> penetratedBlocks = new HashMap<>();
    private final RFEBulletProjectileType type;
    private final RFEProjectileInstance instance;
    private final RandomSource random;

    public RFEProjectileClipContext(RFEBulletProjectileType type, RFEProjectileInstance instance, Vec3 from, Vec3 to,
                                    ClipContext.Block block, ClipContext.Fluid fluid, RandomSource random) {
        super(from, to, block, fluid, CollisionContext.empty());
        this.type = type;
        this.instance = instance;
        this.random = random;
    }

    @Override
    public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape blockShape = super.getBlockShape(state, level, pos);

        if (RFEConfig.SERVER.enableBlockPenetration.get() && this.instance.health() > 0f) {
            BlockHitResult hitResult = blockShape.clip(this.getFrom(), this.getTo(), pos);
            if (hitResult != null) {
                RFEProjectilePenetrationProperties penetrationProperties = this.type.getPenetrationProperties();
                RFEProjectilePenetrationProperties.PenetrationStats blockPenetration = penetrationProperties.getBlockPenetrationStats(state);
                if (state.getDestroySpeed(level, pos) != -1 && this.instance.health() >= blockPenetration.bulletDamage()
                        && this.random.nextFloat() < blockPenetration.chance()) {
                    this.penetratedBlocks.put(pos.immutable(), state);
                    this.instance.removeHealth(blockPenetration.bulletDamage());
                    return Shapes.empty();
                }
            }
        }
        return blockShape;
    }

}
