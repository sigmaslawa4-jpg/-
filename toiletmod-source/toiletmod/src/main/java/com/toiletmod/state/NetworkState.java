package com.toiletmod.state;

import com.toiletmod.block.NetworkBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/** Список всех роутеров и станций в мире + проверка «есть ли тут интернет». */
public class NetworkState extends PersistentState {
	public record Source(String dim, BlockPos pos, int range) {}

	private final Set<Source> sources = new LinkedHashSet<>();

	private static final PersistentState.Type<NetworkState> TYPE =
			new PersistentState.Type<>(NetworkState::new, NetworkState::fromNbt, null);

	public static NetworkState get(MinecraftServer server) {
		PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
		return manager.getOrCreate(TYPE, "toiletmod_network");
	}

	public static String dimOf(ServerWorld world) {
		return world.getRegistryKey().getValue().toString();
	}

	public void add(ServerWorld world, BlockPos pos, int range) {
		sources.add(new Source(dimOf(world), pos.toImmutable(), range));
		markDirty();
	}

	/** true, если рядом (в радиусе) есть работающий роутер или станция. */
	public boolean hasInternet(ServerWorld world, BlockPos pos) {
		String dim = dimOf(world);
		boolean found = false;
		Iterator<Source> it = sources.iterator();
		while (it.hasNext()) {
			Source s = it.next();
			if (!s.dim().equals(dim)) continue;
			boolean loaded = world.getChunkManager().isChunkLoaded(s.pos().getX() >> 4, s.pos().getZ() >> 4);
			if (loaded && !(world.getBlockState(s.pos()).getBlock() instanceof NetworkBlock)) {
				it.remove(); // блок сломан
				markDirty();
				continue;
			}
			if (s.pos().getSquaredDistance(pos) <= (double) s.range() * s.range()) {
				found = true;
			}
		}
		return found;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		NbtList list = new NbtList();
		for (Source s : sources) {
			NbtCompound c = new NbtCompound();
			c.putString("dim", s.dim());
			c.putInt("x", s.pos().getX());
			c.putInt("y", s.pos().getY());
			c.putInt("z", s.pos().getZ());
			c.putInt("range", s.range());
			list.add(c);
		}
		nbt.put("sources", list);
		return nbt;
	}

	public static NetworkState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		NetworkState state = new NetworkState();
		NbtList list = nbt.getList("sources", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound c = list.getCompound(i);
			state.sources.add(new Source(c.getString("dim"),
					new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z")), c.getInt("range")));
		}
		return state;
	}
}
