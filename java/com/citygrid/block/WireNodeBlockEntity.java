package com.citygrid.block;

import com.citygrid.CityGrid;
import com.citygrid.Grid;
import com.citygrid.GridConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Хранит провода, подключённые к столбу / щитку / подстанции. Связи хранятся на обоих концах. */
public class WireNodeBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();

    public WireNodeBlockEntity(BlockPos pos, BlockState state) {
        super(CityGrid.WIRE_NODE_BE, pos, state);
    }

    public List<BlockPos> getLinks() {
        return links;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        long[] arr = new long[links.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = links.get(i).asLong();
        nbt.putLongArray("links", arr);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        links.clear();
        for (long l : nbt.getLongArray("links")) links.add(BlockPos.fromLong(l));
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }

    private void sync() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    /** Соединить два узла проводом. */
    public static boolean link(World world, BlockPos a, BlockPos b) {
        if (!(world.getBlockEntity(a) instanceof WireNodeBlockEntity ea)
                || !(world.getBlockEntity(b) instanceof WireNodeBlockEntity eb)) {
            return false;
        }
        if (ea.links.contains(b)) return false;
        if (ea.links.size() >= GridConfig.WIRE_LINKS_MAX || eb.links.size() >= GridConfig.WIRE_LINKS_MAX) return false;
        ea.links.add(b.toImmutable());
        eb.links.add(a.toImmutable());
        ea.sync();
        eb.sync();
        Grid.markDirty(world);
        return true;
    }

    /** Убрать все провода узла (и обратные связи у соседей). */
    public static void removeAllLinks(World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof WireNodeBlockEntity be)) return;
        for (BlockPos t : new ArrayList<>(be.links)) {
            if (world.getBlockEntity(t) instanceof WireNodeBlockEntity other) {
                other.links.remove(pos);
                other.sync();
            }
        }
        be.links.clear();
        be.sync();
        Grid.markDirty(world);
    }
}
