package com.toiletmod.block;

import com.mojang.serialization.MapCodec;
import com.toiletmod.state.NetworkState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Телевизор: включается только если рядом есть интернет, и гаснет, если интернет пропал. */
public class TvBlock extends HorizontalFacingBlock {
	public static final MapCodec<TvBlock> CODEC = createCodec(TvBlock::new);
	public static final BooleanProperty ON = BooleanProperty.of("on");
	private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 4, 16, 15, 12);
	private static final int CHECK_TICKS = 40;

	public TvBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(ON, false));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ON);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(ON, false);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		ServerWorld sw = (ServerWorld) world;

		if (state.get(ON)) {
			world.setBlockState(pos, state.with(ON, false), Block.NOTIFY_ALL);
			world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8f, 0.6f);
			player.sendMessage(Text.translatable("message.toiletmod.tv_off"), true);
			return ActionResult.SUCCESS;
		}

		if (!NetworkState.get(sw.getServer()).hasInternet(sw, pos)) {
			world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.7f, 1.0f);
			player.sendMessage(Text.translatable("message.toiletmod.tv_no_internet"), true);
			return ActionResult.SUCCESS;
		}

		world.setBlockState(pos, state.with(ON, true), Block.NOTIFY_ALL);
		world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8f, 1.0f);
		world.scheduleBlockTick(pos, this, CHECK_TICKS);
		player.sendMessage(Text.translatable("message.toiletmod.tv_on"), true);
		return ActionResult.SUCCESS;
	}

	/** Периодически проверяем, не пропал ли интернет. */
	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.get(ON)) return;
		if (NetworkState.get(world.getServer()).hasInternet(world, pos)) {
			world.scheduleBlockTick(pos, this, CHECK_TICKS);
		} else {
			world.setBlockState(pos, state.with(ON, false), Block.NOTIFY_ALL);
			world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.7f, 0.8f);
		}
	}
}
