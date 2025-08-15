package rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.ritchiesfirearmengine.RitchiesFirearmEngine;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.bullet.RFEBulletProjectileType;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.RFEProjectileInstance;

import java.util.HashMap;

public class ProjClipContext extends ClipContext {
    public HashMap<BlockPos, BlockState> penetratedBlocks = new HashMap<>();
    RFEBulletProjectileType type;
    RFEProjectileInstance instance;
    public ProjClipContext(RFEBulletProjectileType type, RFEProjectileInstance instance, Vec3 pFrom, Vec3 pTo, Block pBlock, Fluid pFluid) {
        super(pFrom, pTo, pBlock, pFluid, null);
        this.type = type;
        this.instance = instance;
    }

    @Override
    public VoxelShape getBlockShape(BlockState pBlockState, BlockGetter pLevel, BlockPos pPos) {
        // TODO: AMMO TYPE HEALTH THINGY
        TagKey<net.minecraft.world.level.block.Block> penetrationTag = type.getBlockPenetrationTag();
        if (instance.health() != 0f && penetrationTag != null && pBlockState.is(penetrationTag)) {
            penetratedBlocks.put(new BlockPos(pPos), pBlockState);
            instance.removeHealth(0.25f);
            return Shapes.empty();
        }
        return super.getBlockShape(pBlockState, pLevel, pPos);
    }
}
