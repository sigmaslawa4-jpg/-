package com.citygrid.block;

import com.citygrid.CityGrid;
import com.citygrid.Grid;
import com.citygrid.GridConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Резервный генератор. Сам включается, когда на щитке рядом пропал ток из сети, и работает на угле.
 * Топливо подкидывается вручную (ПКМ углём), расходуется только во время аварии.
 */
public class GeneratorBlock extends Block implements BlockEntityProvider {
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 3, 15, 12, 13);

    public GeneratorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Properties.LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.LIT);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return (w, p, s, be) -> {
            if (w instanceof ServerWorld sw && be instanceof GeneratorBlockEntity gen) {
                gen.serverTick(sw, p, s);
            }
        };
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world instanceof ServerWorld sw) Grid.register(sw, pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld sw) Grid.unregister(sw, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private static int fuelValue(ItemStack stack) {
        if (stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL)) return GridConfig.FUEL_COAL;
        if (stack.isOf(Items.COAL_BLOCK)) return GridConfig.FUEL_COAL_BLOCK;
        return 0;
    }

    @Override
    public ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                          PlayerEntity player, Hand hand, BlockHitResult hit) {
        int value = fuelValue(stack);
        if (value <= 0) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!world.isClient && world.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            gen.addFuel(value);
            if (!player.isCreative()) stack.decrement(1);
            player.sendMessage(Text.translatable("message.citygrid.gen_fuel", gen.getFuel() / 20), true);
        }
        return ItemActionResult.success(world.isClient);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            player.sendMessage(Text.translatable("message.citygrid.gen_fuel", gen.getFuel() / 20), true);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(Properties.LIT) && random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.8, pos.getY() + 1.0, pos.getZ() + 0.35, 0.0, 0.05, 0.0);
        }
    }
}
