package com.citygrid;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ItemActionResult;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Общая логика поломок и ремонта для всех блоков мода. */
public final class Faults {
    private Faults() {}

    public static final BooleanProperty BROKEN = BooleanProperty.of("broken");

    public static boolean isFaultable(BlockState state) {
        return state.contains(BROKEN);
    }

    public static boolean isNight(World world) {
        long t = world.getTimeOfDay() % 24000L;
        return (t >= 12500L && t <= 23500L) || world.isRaining();
    }

    /** Ломает / чинит всю вертикальную колонну одинаковых блоков (чтобы не чинить каждый сегмент столба). */
    public static void setBroken(World world, BlockPos pos, boolean broken) {
        BlockState state = world.getBlockState(pos);
        if (!isFaultable(state) || state.get(BROKEN) == broken) return;
        Block block = state.getBlock();

        BlockPos.Mutable m = pos.mutableCopy();
        while (world.getBlockState(m.down()).isOf(block)) m.move(Direction.DOWN);

        while (true) {
            BlockState s = world.getBlockState(m);
            if (!s.isOf(block)) break;
            BlockState n = s.with(BROKEN, broken);
            if (broken && n.contains(Properties.LIT)) n = n.with(Properties.LIT, false);
            world.setBlockState(m, n, Block.NOTIFY_ALL);
            if (broken && world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.1);
            }
            m.move(Direction.UP);
        }
        if (broken) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }

    /** Вызывается из randomTick блоков: самопроизвольные неполадки и притягивание молний в грозу. */
    public static void tickFaults(ServerWorld world, BlockPos pos, Random random, Block self) {
        // реагирует только верхний сегмент, чтобы высота столба не влияла на шанс
        if (world.getBlockState(pos.up()).isOf(self)) return;

        boolean rainHere = world.hasRain(pos.up());
        boolean thunderHere = world.isThundering() && rainHere;
        int chance = thunderHere ? GridConfig.FAULT_THUNDER : rainHere ? GridConfig.FAULT_RAIN : GridConfig.FAULT_DRY;
        if (random.nextInt(chance) == 0) {
            setBroken(world, pos, true);
            return;
        }

        if (thunderHere && world.isSkyVisible(pos.up()) && random.nextInt(GridConfig.LIGHTNING_CHANCE) == 0) {
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt != null) {
                bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
                bolt.setCosmetic(!GridConfig.LIGHTNING_FIRE);
                world.spawnEntity(bolt); // сломает столб через ENTITY_LOAD в CityGrid
            }
        }
    }

    /** Ремонт: нужен набор электрика в руке + 1 медный слиток (провод) из инвентаря. */
    public static ItemActionResult tryRepair(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand) {
        if (!state.get(BROKEN) || !stack.isOf(CityGrid.REPAIR_KIT)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int copperSlot = player.isCreative() ? -2 : findCopper(player);
        if (copperSlot == -1) {
            if (!world.isClient) player.sendMessage(Text.translatable("message.citygrid.need_copper"), true);
            return ItemActionResult.FAIL;
        }
        if (world instanceof ServerWorld sw) {
            if (copperSlot >= 0) player.getInventory().getStack(copperSlot).decrement(1);
            stack.damage(1, player, LivingEntity.getSlotForHand(hand));
            setBroken(world, pos, false);
            sw.spawnParticles(ParticleTypes.WAX_ON,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.0);
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.5f, 1.5f);
            player.sendMessage(Text.translatable("message.citygrid.repaired"), true);
        }
        return ItemActionResult.success(world.isClient);
    }

    /** Подсказка при клике пустой рукой по сломанному блоку. */
    public static net.minecraft.util.ActionResult hint(BlockState state, World world, PlayerEntity player) {
        if (state.get(BROKEN) && player.getMainHandStack().isEmpty()) {
            if (!world.isClient) player.sendMessage(Text.translatable("message.citygrid.need_kit"), true);
            return net.minecraft.util.ActionResult.SUCCESS;
        }
        return net.minecraft.util.ActionResult.PASS;
    }

    private static int findCopper(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.COPPER_INGOT)) return i;
        }
        return -1;
    }
}
