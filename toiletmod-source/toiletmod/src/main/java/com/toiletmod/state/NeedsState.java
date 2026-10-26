package com.toiletmod.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.UUID;

/** Хранит потребности всех игроков (сохраняется вместе с миром). */
public class NeedsState extends PersistentState {
	/** UUID -> {пописать, покакать} */
	public final HashMap<UUID, float[]> players = new HashMap<>();

	private static final PersistentState.Type<NeedsState> TYPE =
			new PersistentState.Type<>(NeedsState::new, NeedsState::fromNbt, null);

	public static NeedsState get(MinecraftServer server) {
		PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
		NeedsState state = manager.getOrCreate(TYPE, "toiletmod_needs");
		state.markDirty();
		return state;
	}

	public float[] of(UUID uuid) {
		return players.computeIfAbsent(uuid, k -> new float[]{0f, 0f});
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		NbtCompound all = new NbtCompound();
		for (var e : players.entrySet()) {
			NbtCompound c = new NbtCompound();
			c.putFloat("pee", e.getValue()[0]);
			c.putFloat("poop", e.getValue()[1]);
			all.put(e.getKey().toString(), c);
		}
		nbt.put("players", all);
		return nbt;
	}

	public static NeedsState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		NeedsState state = new NeedsState();
		NbtCompound all = nbt.getCompound("players");
		for (String key : all.getKeys()) {
			NbtCompound c = all.getCompound(key);
			state.players.put(UUID.fromString(key), new float[]{c.getFloat("pee"), c.getFloat("poop")});
		}
		return state;
	}
}
