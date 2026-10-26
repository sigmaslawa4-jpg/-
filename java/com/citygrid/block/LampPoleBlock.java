package com.citygrid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.BlockView;

/** Простой металлический сегмент столба - для наращивания высоты фонаря. */
public class LampPoleBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(6, 0, 6, 10, 16, 10);

    public LampPoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
