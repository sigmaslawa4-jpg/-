package com.citygrid;

import com.citygrid.block.CableBlock;
import com.citygrid.block.GeneratorBlock;
import com.citygrid.block.MeterBoxBlock;
import com.citygrid.block.PowerPoleBlock;
import com.citygrid.block.SubstationBlock;
import com.citygrid.block.WireNodeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Электросеть. Ток течёт от подстанций по исправным столбам, проводам и подземным кабелям.
 * Сломанный столб ток не пропускает - всё, что за ним, остаётся без света.
 */
public final class Grid {
    private Grid() {}

    public static final class State {
        final Set<BlockPos> substations = new HashSet<>();
        final Set<BlockPos> meters = new HashSet<>();
        final Set<BlockPos> generators = new HashSet<>();
        final Set<Long> energized = new HashSet<>();
        long lastTick = Long.MIN_VALUE / 2;
        boolean dirty = true;
    }

    private static final Map<RegistryKey<World>, State> STATES = new HashMap<>();

    public static void clear() {
        STATES.clear();
    }

    private static State of(ServerWorld world) {
        return STATES.computeIfAbsent(world.getRegistryKey(), k -> new State());
    }

    /** Сообщить сети, что что-то поменялось (поломка, ремонт, новый провод...). */
    public static void markDirty(World world) {
        if (world instanceof ServerWorld sw) of(sw).dirty = true;
    }

    // ---------- реестр узлов ----------

    public static void register(ServerWorld world, BlockPos pos, BlockState state) {
        State s = of(world);
        BlockPos p = pos.toImmutable();
        if (state.getBlock() instanceof SubstationBlock) s.substations.add(p);
        else if (state.getBlock() instanceof MeterBoxBlock) s.meters.add(p);
        else if (state.getBlock() instanceof GeneratorBlock) s.generators.add(p);
        s.dirty = true;
    }

    public static void unregister(ServerWorld world, BlockPos pos) {
        State s = of(world);
        s.substations.remove(pos);
        s.meters.remove(pos);
        s.generators.remove(pos);
        s.dirty = true;
    }

    // ---------- расчёт сети ----------

    private static boolean loaded(ServerWorld w, BlockPos p) {
        return w.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4);
    }

    private static boolean isConductive(BlockState st) {
        if (st.getBlock() instanceof PowerPoleBlock) return !st.get(Faults.BROKEN);
        return st.getBlock() instanceof CableBlock
                || st.getBlock() instanceof MeterBoxBlock
                || st.getBlock() instanceof SubstationBlock;
    }

    private static void visit(ServerWorld w, State s, BlockPos pos, ArrayDeque<BlockPos> queue) {
        long key = pos.asLong();
        if (s.energized.contains(key)) return;
        if (!loaded(w, pos)) return;
        if (!isConductive(w.getBlockState(pos))) return;
        s.energized.add(key);
        queue.add(pos.toImmutable());
    }

    private static void fresh(ServerWorld w, State s) {
        long now = w.getTime();
        if (!s.dirty && now >= s.lastTick && now - s.lastTick < 20) return;

        s.energized.clear();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        Iterator<BlockPos> it = s.substations.iterator();
        while (it.hasNext()) {
            BlockPos p = it.next();
            if (!(w.getBlockState(p).getBlock() instanceof SubstationBlock)) {
                it.remove();
                continue;
            }
            if (s.energized.add(p.asLong())) queue.add(p);
        }

        int budget = 30000;
        while (!queue.isEmpty() && budget-- > 0) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.values()) {
                visit(w, s, cur.offset(d), queue);
            }
            BlockEntity be = w.getBlockEntity(cur);
            if (be instanceof WireNodeBlockEntity node) {
                for (BlockPos target : node.getLinks()) {
                    visit(w, s, target, queue);
                }
            }
        }
        s.dirty = false;
        s.lastTick = now;
    }

    // ---------- запросы потребителей ----------

    /** Находится ли блок (столб, кабель, щиток) под напряжением от подстанции. */
    public static boolean isEnergized(ServerWorld world, BlockPos pos) {
        State s = of(world);
        fresh(world, s);
        return s.energized.contains(pos.asLong());
    }

    /** Уличный фонарь: рядом есть столб под напряжением. */
    public static boolean streetPowered(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld sw)) return false;
        State s = of(sw);
        fresh(sw, s);
        if (s.energized.isEmpty()) return false;
        int r = GridConfig.STREET_RANGE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    long key = BlockPos.asLong(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (s.energized.contains(key)) {
                        BlockState st = sw.getBlockState(BlockPos.fromLong(key));
                        if (st.getBlock() instanceof PowerPoleBlock) return true;
                    }
                }
            }
        }
        return false;
    }

    /** Есть ли ток на щитке: либо от сети, либо от работающего генератора рядом. */
    public static boolean isMeterPowered(World world, BlockPos meter) {
        if (!(world instanceof ServerWorld sw)) return false;
        State s = of(sw);
        fresh(sw, s);
        return meterPowered(sw, s, meter);
    }

    private static boolean meterPowered(ServerWorld sw, State s, BlockPos meter) {
        if (s.energized.contains(meter.asLong())) return true;
        double r2 = (double) GridConfig.GENERATOR_RADIUS * GridConfig.GENERATOR_RADIUS;
        for (BlockPos g : s.generators) {
            if (g.getSquaredDistance(meter) > r2) continue;
            BlockState st = sw.getBlockState(g);
            if (st.getBlock() instanceof GeneratorBlock && st.get(Properties.LIT)) return true;
        }
        return false;
    }

    /** Домашний потребитель (лампа, ТВ): рядом есть щиток с током. */
    public static boolean houseHasPower(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld sw)) return false;
        State s = of(sw);
        fresh(sw, s);
        double r2 = (double) GridConfig.HOUSE_RADIUS * GridConfig.HOUSE_RADIUS;
        Iterator<BlockPos> it = s.meters.iterator();
        while (it.hasNext()) {
            BlockPos m = it.next();
            if (!(sw.getBlockState(m).getBlock() instanceof MeterBoxBlock)) {
                it.remove();
                continue;
            }
            if (m.getSquaredDistance(pos) > r2) continue;
            if (meterPowered(sw, s, m)) return true;
        }
        return false;
    }

    /** Нужно ли генератору работать: рядом есть щиток, на который сеть ток не даёт. */
    public static boolean needsBackup(ServerWorld sw, BlockPos genPos) {
        State s = of(sw);
        fresh(sw, s);
        double r2 = (double) GridConfig.GENERATOR_RADIUS * GridConfig.GENERATOR_RADIUS;
        for (BlockPos m : s.meters) {
            if (m.getSquaredDistance(genPos) > r2) continue;
            if (!s.energized.contains(m.asLong())) return true;
        }
        return false;
    }
}
