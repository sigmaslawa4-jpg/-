package com.toiletmod;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.toiletmod.state.NeedsState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class NeedsLogic {
	/** За сколько секунд полоска заполняется до 100%. */
	public static final float PEE_FULL_SECONDS = 600f;   // 10 минут
	public static final float POOP_FULL_SECONDS = 1200f; // 20 минут

	public static void init() {
		PayloadTypeRegistry.playS2C().register(NeedsPayload.ID, NeedsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenBrowserPayload.ID, OpenBrowserPayload.CODEC);

		// Раз в секунду увеличиваем потребности и шлём клиенту.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) return;
			NeedsState state = NeedsState.get(server);
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				float[] n = state.of(p.getUuid());
				if (!p.isCreative() && !p.isSpectator() && p.isAlive()) {
					n[0] += 100f / PEE_FULL_SECONDS;
					n[1] += 100f / POOP_FULL_SECONDS;
					if (n[0] >= 100f) {
						n[0] = 0f;
						p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
						p.sendMessage(Text.translatable("message.toiletmod.accident_pee"), true);
					}
					if (n[1] >= 100f) {
						n[1] = 0f;
						p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
						p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
						p.sendMessage(Text.translatable("message.toiletmod.accident_poop"), true);
					}
				}
				sync(p, n);
			}
		});

		// Сразу показываем полоски при входе в мир.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			float[] n = NeedsState.get(server).of(handler.getPlayer().getUuid());
			sync(handler.getPlayer(), n);
		});

		// Команда для теста: /needs <пописать 0-100> <покакать 0-100>
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("needs")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("pee", FloatArgumentType.floatArg(0, 100))
								.then(CommandManager.argument("poop", FloatArgumentType.floatArg(0, 100))
										.executes(ctx -> {
											ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
											MinecraftServer server = ctx.getSource().getServer();
											float[] n = NeedsState.get(server).of(p.getUuid());
											n[0] = FloatArgumentType.getFloat(ctx, "pee");
											n[1] = FloatArgumentType.getFloat(ctx, "poop");
											sync(p, n);
											ctx.getSource().sendFeedback(() -> Text.literal("OK"), false);
											return 1;
										})))));
	}

	public static void sync(ServerPlayerEntity player, float[] n) {
		ServerPlayNetworking.send(player, new NeedsPayload(n[0], n[1]));
	}
}
