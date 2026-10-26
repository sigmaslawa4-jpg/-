package com.citygrid.block;

import com.citygrid.Grid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/** Подземный кабель в трубе: сам стыкуется с соседними кабелями, столбами, щитками и подстанциями. */
public class CableBlock extends Block {
    private static final Direction[] DIRS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
    private static final BooleanProperty[] PROPS = {
            Properties.NORTH, Properties.EAST, Properties.SOUTH, Properties.WEST, Properties.UP, Properties.DOWN};
    private static final VoxelShape CORE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARMS = {
            Block.createCuboidShape(5, 5, 0, 11, 11, 5),
            Block.createCuboidShape(11, 5, 5, 16, 11, 11),
            Block.createCuboidShape(5, 5, 11, 11, 11, 16),
            Block.createCuboidShape(0, 5, 5, 5, 11, 11),
            Block.createCuboidShape(5, 11, 5, 11, 16, 11),
            Block.createCuboidShape(5, 0, 5, 11, 5, 11)};

    public CableBlock(Settings settings) {
        super(settings);
        BlockState s = getStateManager().getDefaultState();
        for (BooleanProperty p : PROPS) s = s.with(p, false);
        setDefaultState(s);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PROPS);
    }

    private static int index(Direction d) {
        for (int i = 0; i < DIRS.length; i++) if (DIRS[i] == d) return i;
        return 0;
    }

    private static boolean connects(BlockState s) {
        Block b = s.getBlock();
        return b instanceof CableBlock || b instanceof PowerPoleBlock
                || b instanceof MeterBoxBlock || b instanceof SubstationBlock;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState s = getDefaultState();
        for (int i = 0; i < DIRS.length; i++) {
            s = s.with(PROPS[i], connects(world.getBlockState(pos.offset(DIRS[i]))));
        }
        return s;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state.with(PROPS[index(direction)], connects(neighborState));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE;
        for (int i = 0; i < DIRS.length; i++) {
            if (state.get(PROPS[i])) shape = VoxelShapes.union(shape, ARMS[i]);
        }
        return shape;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        Grid.markDirty(world);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) Grid.markDirty(world);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
