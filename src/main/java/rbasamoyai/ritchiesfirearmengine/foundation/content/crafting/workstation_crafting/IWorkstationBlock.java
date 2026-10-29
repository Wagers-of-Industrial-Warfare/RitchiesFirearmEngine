package rbasamoyai.ritchiesfirearmengine.foundation.content.crafting.workstation_crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public interface IWorkstationBlock<WBE extends AbstractWorkstationBlockEntity> extends EntityBlock {

    BlockEntityType<WBE> blockEntityType();

    @Override
    default @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.blockEntityType().create(pos, state);
    }

    @Override
    default @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, this.blockEntityType(), IWorkstationBlock::serverTicker);
    }

    @Nullable
    static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType, BlockEntityType<E> clientType, BlockEntityTicker<? super E> ticker
    ) {
        return clientType == serverType ? (BlockEntityTicker<A>)ticker : null;
    }

    static void serverTicker(Level level, BlockPos pos, BlockState state, AbstractWorkstationBlockEntity blockEntity) {
        blockEntity.tick();
    }

}
