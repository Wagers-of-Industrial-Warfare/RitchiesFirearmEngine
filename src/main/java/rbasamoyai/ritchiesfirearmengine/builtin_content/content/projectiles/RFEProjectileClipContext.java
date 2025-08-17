package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.HashMap;
import java.util.Map;

public class RFEProjectileClipContext extends ClipContext {

    public final Map<BlockPos, BlockState> penetratedBlocks = new HashMap<>();
    private final RFEBulletProjectileType type;
    private final RFEProjectileInstance instance;

    public RFEProjectileClipContext(RFEBulletProjectileType type, RFEProjectileInstance instance, Vec3 from, Vec3 to,
                                    ClipContext.Block block, ClipContext.Fluid fluid) {
        super(from, to, block, fluid, null);
        this.type = type;
        this.instance = instance;
    }

    @Override
    public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
        // TODO: AMMO TYPE HEALTH THINGY
        TagKey<net.minecraft.world.level.block.Block> penetrationTag = this.type.getBlockPenetrationTag();
        if (this.instance.health() > 0f && penetrationTag != null && state.is(penetrationTag)) {
            this.penetratedBlocks.put(new BlockPos(pos), state);
            this.instance.removeHealth(0.25f);
            return Shapes.empty();
        }
        return super.getBlockShape(state, level, pos);
    }

}
