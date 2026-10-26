package com.toiletmod.block;

import com.mojang.serialization.MapCodec;
import com.toiletmod.state.NetworkState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Роутер (маленький радиус) и интернет-станция (большой радиус). */
public class NetworkBlock extends Block {
	public static final int ROUTER_RANGE = 32;
	public static final int STATION_RANGE = 96;
	public static final VoxelShape ROUTER_SHAPE = Block.createCuboidShape(3, 0, 3, 13, 10, 13);
	public static final VoxelShape STATION_SHAPE = Block.createCuboidShape(5, 0, 5, 11, 16, 11);

	public static final MapCodec<NetworkBlock> CODEC =
			createCodec(s -> new NetworkBlock(s, ROUTER_RANGE, ROUTER_SHAPE));

	private final int range;
	private final VoxelShape shape;

	public NetworkBlock(Settings settings, int range, VoxelShape shape) {
		super(settings);
		this.range = range;
		this.shape = shape;
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	public int getRange() {
		return range;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shape;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onPlaced(world, pos, state, placer, stack);
		if (world instanceof ServerWorld sw) {
			NetworkState.get(sw.getServer()).add(sw, pos, range);
			if (placer instanceof PlayerEntity p) {
				p.sendMessage(Text.translatable("message.toiletmod.network_placed", range), true);
			}
		}
	}

	/** ПКМ: показать радиус и (пере)зарегистрировать блок, если он был поставлен командой. */
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world instanceof ServerWorld sw) {
			NetworkState.get(sw.getServer()).add(sw, pos, range);
			player.sendMessage(Text.translatable("message.toiletmod.network_info", range), true);
		}
		return ActionResult.SUCCESS;
	}
}
