package com.citygrid.block;

import com.citygrid.Grid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Подстанция - источник тока для всей сети. К ней подключаются провода и кабели. */
public class SubstationBlock extends Block implements BlockEntityProvider {
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);

    public SubstationBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WireNodeBlockEntity(pos, state);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world instanceof ServerWorld sw) Grid.register(sw, pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            WireNodeBlockEntity.removeAllLinks(world, pos);
            if (world instanceof ServerWorld sw) Grid.unregister(sw, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
