package com.toiletmod.client;

import com.toiletmod.NeedsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ToiletModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(NeedsPayload.ID, (payload, context) -> {
			ClientNeeds.pee = payload.pee();
			ClientNeeds.poop = payload.poop();
		});
		HudRenderCallback.EVENT.register(NeedsHud::render);
	}
}
