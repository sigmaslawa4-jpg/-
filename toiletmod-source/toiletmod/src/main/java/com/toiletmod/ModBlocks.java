package com.toiletmod;

import com.toiletmod.block.NetworkBlock;
import com.toiletmod.block.ToiletBlock;
import com.toiletmod.block.ScreenBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModBlocks {
	public static final Block TOILET = register("toilet",
			new ToiletBlock(AbstractBlock.Settings.create().strength(1.5f).sounds(BlockSoundGroup.STONE).nonOpaque()));

	public static final Block ROUTER = register("router",
			new NetworkBlock(AbstractBlock.Settings.create().strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque(),
					NetworkBlock.ROUTER_RANGE, NetworkBlock.ROUTER_SHAPE));

	public static final Block INTERNET_STATION = register("internet_station",
			new NetworkBlock(AbstractBlock.Settings.create().strength(2.0f).sounds(BlockSoundGroup.METAL).nonOpaque(),
					NetworkBlock.STATION_RANGE, NetworkBlock.STATION_SHAPE));

	public static final Block TV = register("tv",
			new ScreenBlock(AbstractBlock.Settings.create().strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque()
					.luminance(state -> state.get(ScreenBlock.ON) ? 8 : 0), ScreenBlock.KIND_TV));

	public static final Block COMPUTER = register("computer",
			new ScreenBlock(AbstractBlock.Settings.create().strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque()
					.luminance(state -> state.get(ScreenBlock.ON) ? 8 : 0), ScreenBlock.KIND_COMPUTER));

	private static Block register(String name, Block block) {
		Identifier id = Identifier.of(ToiletMod.MOD_ID, name);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
		return block;
	}

	public static void init() {
		Registry.register(Registries.ITEM_GROUP, Identifier.of(ToiletMod.MOD_ID, "main"),
				FabricItemGroup.builder()
						.displayName(Text.translatable("itemGroup.toiletmod.main"))
						.icon(() -> new ItemStack(TOILET))
						.entries((ctx, entries) -> {
							entries.add(TOILET);
							entries.add(ROUTER);
							entries.add(INTERNET_STATION);
							entries.add(TV);
							entries.add(COMPUTER);
						})
						.build());
	}
}
