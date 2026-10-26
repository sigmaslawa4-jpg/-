package com.citygrid.block;

import com.citygrid.CityGrid;
import com.citygrid.Grid;
import com.citygrid.GridConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

/** Бак генератора. Топливо тратится только пока генератор реально работает (в сети нет тока). */
public class GeneratorBlockEntity extends BlockEntity {
    private int fuel;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(CityGrid.GENERATOR_BE, pos, state);
    }

    public int getFuel() {
        return fuel;
    }

    public void addFuel(int ticks) {
        fuel = Math.min(GridConfig.MAX_FUEL, fuel + ticks);
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("fuel", fuel);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        fuel = nbt.getInt("fuel");
    }

    public void serverTick(ServerWorld world, BlockPos pos, BlockState state) {
        if (world.getTime() % 20 != 0) return;
        boolean run = fuel > 0 && Grid.needsBackup(world, pos);
        if (run) {
            fuel = Math.max(0, fuel - 20);
            markDirty();
        }
        if (run != state.get(Properties.LIT)) {
            world.setBlockState(pos, state.with(Properties.LIT, run), 3);
        }
    }
}
