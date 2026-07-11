package rbasamoyai.ritchiesfirearmengine.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rbasamoyai.ritchiesfirearmengine.builtin_content.content.projectiles.RFEProjectileCollisionContext;
import rbasamoyai.ritchiesfirearmengine.foundation.api.projectiles.penetration.RFEProjectilePenetrationProperties;
import rbasamoyai.ritchiesfirearmengine.foundation.config.RFEConfig;

@Mixin(ClipContext.class)
public class ClipContextMixin {

    @Shadow @Final private CollisionContext collisionContext;
    @Shadow @Final private Vec3 from;
    @Shadow @Final private Vec3 to;

    @WrapMethod(method = "getBlockShape")
    private VoxelShape ritchiesfirearmengine$getBlockShape(BlockState blockState, BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        VoxelShape blockShape = original.call(blockState, level, pos);
        if (this.collisionContext instanceof RFEProjectileCollisionContext rfeContext) {
            if (RFEConfig.SERVER.enableBlockPenetration.get() && rfeContext.instance.health() > 0f) {
                BlockHitResult hitResult = blockShape.clip(this.from, this.to, pos);
                if (hitResult != null) {
                    RFEProjectilePenetrationProperties penetrationProperties = rfeContext.type.getPenetrationProperties();
                    RFEProjectilePenetrationProperties.PenetrationStats blockPenetration = penetrationProperties.getBlockPenetrationStats(blockState);
                    if (blockState.getDestroySpeed(level, pos) != -1 && rfeContext.instance.health() >= blockPenetration.bulletDamage()
                            && rfeContext.random.nextFloat() < blockPenetration.chance()) {
                        rfeContext.penetratedBlocks.put(pos.immutable(), blockState);
                        rfeContext.instance.removeHealth(blockPenetration.bulletDamage());
                        return Shapes.empty();
                    }
                }
            }
        }
        return blockShape;
    }

}
