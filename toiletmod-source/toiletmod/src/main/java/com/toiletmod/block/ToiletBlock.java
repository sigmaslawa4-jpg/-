package com.toiletmod.block;

import com.mojang.serialization.MapCodec;
import com.toiletmod.state.NeedsState;
import com.toiletmod.NeedsLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ToiletBlock extends HorizontalFacingBlock {
	public static final MapCodec<ToiletBlock> CODEC = createCodec(ToiletBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 14, 14);

	public ToiletBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;

		ServerWorld sw = (ServerWorld) world;
		float[] n = NeedsState.get(sw.getServer()).of(player.getUuid());

		if (n[0] < 5f && n[1] < 5f) {
			player.sendMessage(Text.translatable("message.toiletmod.not_needed"), true);
			return ActionResult.SUCCESS;
		}

		boolean pee = n[0] >= 5f;
		boolean poop = n[1] >= 5f;
		if (pee) n[0] = 0f;
		if (poop) n[1] = 0f;

		world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 1.0f, 1.0f);
		world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.BLOCKS, 0.8f, 0.9f);

		String key = pee && poop ? "message.toiletmod.relief_both"
				: pee ? "message.toiletmod.relief_pee" : "message.toiletmod.relief_poop";
		player.sendMessage(Text.translatable(key), true);

		if (player instanceof ServerPlayerEntity sp) {
			NeedsLogic.sync(sp, n);
		}
		return ActionResult.SUCCESS;
	}
}
