package com.citygrid.item;

import com.citygrid.GridConfig;
import com.citygrid.block.MeterBoxBlock;
import com.citygrid.block.PowerPoleBlock;
import com.citygrid.block.SubstationBlock;
import com.citygrid.block.WireNodeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Катушка провода. ПКМ по столбу / щитку / подстанции - выбрать первый конец, ПКМ по второму - протянуть провод.
 * Shift + ПКМ по узлу - срезать все его провода.
 */
public class WireSpoolItem extends Item {
    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();

    public WireSpoolItem(Settings settings) {
        super(settings);
    }

    private static boolean isNode(BlockState s) {
        return s.getBlock() instanceof PowerPoleBlock
                || s.getBlock() instanceof MeterBoxBlock
                || s.getBlock() instanceof SubstationBlock;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        PlayerEntity player = ctx.getPlayer();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (player == null || !isNode(state)) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;

        // у столба провод крепится к верхнему сегменту
        if (state.getBlock() instanceof PowerPoleBlock) {
            while (world.getBlockState(pos.up()).getBlock() instanceof PowerPoleBlock) pos = pos.up();
        }

        if (player.isSneaking()) {
            WireNodeBlockEntity.removeAllLinks(world, pos);
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.translatable("message.citygrid.wire_cut"), true);
            return ActionResult.SUCCESS;
        }

        BlockPos first = PENDING.get(player.getUuid());
        if (first == null || first.equals(pos) || !isNode(world.getBlockState(first))) {
            PENDING.put(player.getUuid(), pos.toImmutable());
            player.sendMessage(Text.translatable("message.citygrid.wire_first"), true);
            return ActionResult.SUCCESS;
        }

        double max = GridConfig.WIRE_MAX;
        if (pos.getSquaredDistance(first) > max * max) {
            player.sendMessage(Text.translatable("message.citygrid.wire_far", GridConfig.WIRE_MAX), true);
            return ActionResult.SUCCESS;
        }

        if (WireNodeBlockEntity.link(world, first, pos)) {
            ctx.getStack().damage(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.translatable("message.citygrid.wire_linked"), true);
        } else {
            player.sendMessage(Text.translatable("message.citygrid.wire_fail"), true);
        }
        return ActionResult.SUCCESS;
    }
}
