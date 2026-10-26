package com.citygrid.block;

import com.citygrid.Grid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Домашний потребитель (лампа, ТВ). ON - выключатель (ПКМ), LIT - реально горит:
 * включено И рядом есть электрощиток, на котором есть ток (от сети или от генератора).
 */
public abstract class HouseApplianceBlock extends Block {
    public static final BooleanProperty ON = BooleanProperty.of("on");

    protected HouseApplianceBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(ON, true).with(Properties.LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ON, Properties.LIT);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient && !oldState.isOf(this)) {
            world.scheduleBlockTick(pos, this, 5);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean lit = state.get(ON) && Grid.houseHasPower(world, pos);
        if (lit != state.get(Properties.LIT)) {
            world.setBlockState(pos, state.with(Properties.LIT, lit), Block.NOTIFY_ALL);
        }
        world.scheduleBlockTick(pos, this, 30 + random.nextInt(20));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockState next = state.cycle(ON);
            world.setBlockState(pos, next, Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5f,
                    next.get(ON) ? 0.8f : 0.6f);
            world.scheduleBlockTick(pos, this, 1);
        }
        return ActionResult.success(world.isClient);
    }
}
