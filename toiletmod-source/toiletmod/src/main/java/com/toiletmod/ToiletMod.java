package com.toiletmod;

import net.fabricmc.api.ModInitializer;

public class ToiletMod implements ModInitializer {
	public static final String MOD_ID = "toiletmod";

	@Override
	public void onInitialize() {
		ModBlocks.init();
		NeedsLogic.init();
	}
}
