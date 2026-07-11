package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.HashMap;
import java.util.Map;

public class RFEProjectileCollisionContext implements CollisionContext {

    public final Map<BlockPos, BlockState> penetratedBlocks = new HashMap<>();
    public final RFEBulletProjectileType type;
    public final RFEProjectileInstance instance;
    public final RandomSource random;

    public RFEProjectileCollisionContext(RFEBulletProjectileType type, RFEProjectileInstance instance, RandomSource random) {
        this.type = type;
        this.instance = instance;
        this.random = random;
    }

    @Override public boolean isDescending() { return false; }
    @Override public boolean isAbove(VoxelShape shape, BlockPos pos, boolean canAscend) { return false; }
    @Override public boolean isHoldingItem(Item item) { return false; }
    @Override public boolean canStandOnFluid(FluidState fluid1, FluidState fluid2) { return false; }

}
