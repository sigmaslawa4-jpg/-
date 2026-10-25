package com.citygrid.block;

import com.citygrid.Faults;
import com.citygrid.GridConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.ItemActionResult;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.ChunkManager;

/** Деревянный электрический столб. Ставится в высоту; верхний сегмент получает траверсу с изоляторами. */
public class PowerPoleBlock extends Block {
    public static final BooleanProperty TOP = BooleanProperty.of("top");

    private static final VoxelShape POLE = Block.createCuboidShape(5, 0, 5, 11, 16, 11);
    private static final VoxelShape ARM = Block.createCuboidShape(0, 11, 6, 16, 13, 10);
    private static final VoxelShape TOP_SHAPE = VoxelShapes.union(POLE, ARM);

    public PowerPoleBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Faults.BROKEN, false).with(TOP, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Faults.BROKEN, TOP);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState below = world.getBlockState(pos.down());
        boolean broken = below.isOf(this) && below.get(Faults.BROKEN); // продолжение сломанного столба тоже сломано
        return getDefaultState()
                .with(Faults.BROKEN, broken)
                .with(TOP, !world.getBlockState(pos.up()).isOf(this));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.with(TOP, !neighborState.isOf(this));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(TOP) ? TOP_SHAPE : POLE;
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return !state.get(Faults.BROKEN);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Faults.tickFaults(world, pos, random, this);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(Faults.BROKEN) && state.get(TOP) && random.nextInt(6) == 0) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + random.nextDouble(), pos.getY() + 0.7, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }

    @Override
    public ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                          PlayerEntity player, Hand hand, BlockHitResult hit) {
        return Faults.tryRepair(stack, state, world, pos, player, hand);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return Faults.hint(state, world, player);
    }

    /** Есть ли рядом исправный электрический столб. */
    public static boolean hasPower(World world, BlockPos origin) {
        int r = GridConfig.POWER_RANGE;
        ChunkManager cm = world.getChunkManager();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int wx = origin.getX() + dx;
                int wz = origin.getZ() + dz;
                if (!cm.isChunkLoaded(wx >> 4, wz >> 4)) continue;
                for (int dy = -r; dy <= r; dy++) {
                    p.set(wx, origin.getY() + dy, wz);
                    BlockState s = world.getBlockState(p);
                    if (s.getBlock() instanceof PowerPoleBlock && !s.get(Faults.BROKEN)) return true;
                }
            }
        }
        return false;
    }
}
