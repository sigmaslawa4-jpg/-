package com.citygrid;

import com.citygrid.block.CableBlock;
import com.citygrid.block.GeneratorBlock;
import com.citygrid.block.GeneratorBlockEntity;
import com.citygrid.block.HouseLampBlock;
import com.citygrid.block.LampPoleBlock;
import com.citygrid.block.MeterBoxBlock;
import com.citygrid.block.PowerPoleBlock;
import com.citygrid.block.StreetLampBlock;
import com.citygrid.block.SubstationBlock;
import com.citygrid.block.TvBlock;
import com.citygrid.block.WireNodeBlockEntity;
import com.citygrid.item.WireSpoolItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CityGrid implements ModInitializer {
    public static final String MOD_ID = "citygrid";

    public static Block POWER_POLE;
    public static Block LAMP_POLE;
    public static Block STREET_LAMP;
    public static Block CABLE;
    public static Block SUBSTATION;
    public static Block METER_BOX;
    public static Block HOUSE_LAMP;
    public static Block TV;
    public static Block GENERATOR;
    public static Item REPAIR_KIT;
    public static Item WIRE_SPOOL;
    public static BlockEntityType<WireNodeBlockEntity> WIRE_NODE_BE;
    public static BlockEntityType<GeneratorBlockEntity> GENERATOR_BE;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, id(name), block);
        Registry.register(Registries.ITEM, id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    @Override
    public void onInitialize() {
        POWER_POLE = registerBlock("power_pole", new PowerPoleBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.BROWN)
                        .instrument(NoteBlockInstrument.BASS)
                        .strength(2.0f)
                        .sounds(BlockSoundGroup.WOOD)
                        .nonOpaque()));

        LAMP_POLE = registerBlock("lamp_pole", new LampPoleBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(3.0f, 6.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()));

        STREET_LAMP = registerBlock("street_lamp", new StreetLampBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(3.0f, 6.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .luminance(state -> state.get(Properties.LIT) ? 15 : 0)
                        .nonOpaque()));

        CABLE = registerBlock("cable", new CableBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.ORANGE)
                        .strength(1.5f, 6.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()));

        SUBSTATION = registerBlock("substation", new SubstationBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(4.0f, 8.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()));

        METER_BOX = registerBlock("meter_box", new MeterBoxBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.LIGHT_GRAY)
                        .strength(2.0f, 4.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()));

        HOUSE_LAMP = registerBlock("house_lamp", new HouseLampBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.YELLOW)
                        .strength(0.5f)
                        .sounds(BlockSoundGroup.GLASS)
                        .luminance(state -> state.get(Properties.LIT) ? 15 : 0)
                        .nonOpaque()));

        TV = registerBlock("tv", new TvBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.BLACK)
                        .strength(1.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .luminance(state -> state.get(Properties.LIT) ? 7 : 0)
                        .nonOpaque()));

        GENERATOR = registerBlock("generator", new GeneratorBlock(
                AbstractBlock.Settings.create()
                        .mapColor(MapColor.ORANGE)
                        .strength(3.0f, 6.0f)
                        .sounds(BlockSoundGroup.METAL)
                        .luminance(state -> state.get(Properties.LIT) ? 8 : 0)
                        .nonOpaque()));

        WIRE_NODE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("wire_node"),
                FabricBlockEntityTypeBuilder.create(WireNodeBlockEntity::new, POWER_POLE, METER_BOX, SUBSTATION).build());
        GENERATOR_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("generator"),
                FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, GENERATOR).build());

        REPAIR_KIT = Registry.register(Registries.ITEM, id("repair_kit"),
                new Item(new Item.Settings().maxDamage(40)));
        WIRE_SPOOL = Registry.register(Registries.ITEM, id("wire_spool"),
                new WireSpoolItem(new Item.Settings().maxDamage(32)));

        // реестр подстанций / щитков / генераторов (в т.ч. при загрузке чанков)
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, world) -> Grid.register(world, be.getPos(), be.getCachedState()));
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, world) -> Grid.unregister(world, be.getPos()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Grid.clear());

        Registry.register(Registries.ITEM_GROUP, id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(STREET_LAMP))
                .displayName(Text.translatable("itemGroup.citygrid"))
                .entries((ctx, entries) -> {
                    entries.add(POWER_POLE);
                    entries.add(LAMP_POLE);
                    entries.add(STREET_LAMP);
                    entries.add(CABLE);
                    entries.add(SUBSTATION);
                    entries.add(METER_BOX);
                    entries.add(HOUSE_LAMP);
                    entries.add(TV);
                    entries.add(GENERATOR);
                    entries.add(WIRE_SPOOL);
                    entries.add(REPAIR_KIT);
                })
                .build());

        // Любая молния (и естественная, и вызванная нашим столбом) ломает всё рядом.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof LightningEntity bolt)) return;
            BlockPos c = bolt.getBlockPos();
            int r = GridConfig.STRIKE_RADIUS;
            BlockPos.Mutable p = new BlockPos.Mutable();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > r * r) continue;
                    if (!world.getChunkManager().isChunkLoaded((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4)) continue;
                    for (int dy = -14; dy <= 2; dy++) {
                        p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                        if (!Faults.isFaultable(world.getBlockState(p))) continue;
                        // близко - ломается всегда, подальше - 50% (электрическая дуга)
                        if (distSq <= 4 || world.random.nextBoolean()) {
                            Faults.setBroken(world, p.toImmutable(), true);
                        }
                    }
                }
            }
        });
    }
}
