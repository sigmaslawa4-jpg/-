package com.citygrid.client;

import com.citygrid.CityGrid;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class CityGridClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(CityGrid.WIRE_NODE_BE, WireRenderer::new);
    }
}
