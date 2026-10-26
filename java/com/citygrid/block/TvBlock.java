package com.citygrid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Телевизор: экран светится, только если в доме есть электричество. */
public class TvBlock extends HouseApplianceBlock {
    private static final VoxelShape SHAPE_Z = Block.createCuboidShape(1, 0, 5, 15, 14, 11);
    private static final VoxelShape SHAPE_X = Block.createCuboidShape(5, 0, 1, 11, 14, 15);

    public TvBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(Properties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }
}
