package com.citygrid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** Домашняя лампа (подвесной светильник). */
public class HouseLampBlock extends HouseApplianceBlock {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(7, 10, 7, 9, 16, 9),
            Block.createCuboidShape(4, 4, 4, 12, 10, 12));

    public HouseLampBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
