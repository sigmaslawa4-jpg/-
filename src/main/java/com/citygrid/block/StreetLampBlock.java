package com.citygrid.block;

import com.citygrid.Faults;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemActionResult;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Уличный фонарь. Горит ночью (и в дождь), если рядом есть исправный электрический столб
 * и сам фонарь не сломан.
 */
public class StreetLampBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(6, 0, 6, 10, 12, 10),
            Block.createCuboidShape(3, 10, 3, 13, 16, 13));

    public StreetLampBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Faults.BROKEN, false).with(Properties.LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Faults.BROKEN, Properties.LIT);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) {
            world.scheduleBlockTick(pos, this, 5);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean lit = !state.get(Faults.BROKEN) && Faults.isNight(world) && PowerPoleBlock.hasPower(world, pos);
        if (lit != state.get(Properties.LIT)) {
            world.setBlockState(pos, state.with(Properties.LIT, lit), Block.NOTIFY_ALL);
        }
        world.scheduleBlockTick(pos, this, 40 + random.nextInt(20));
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
        if (state.get(Faults.BROKEN) && random.nextInt(5) == 0) {
            world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
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
}
