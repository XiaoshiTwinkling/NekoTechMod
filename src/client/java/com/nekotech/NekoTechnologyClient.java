package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.renderer.BellowsBlockEntityRenderer;
import com.nekotech.screen.AlloyFurnaceScreen;
import com.nekotech.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class NekoTechnologyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NekoTechnology.LOGGER.info("NekoTechnologyClient initialized");
		HandledScreens.register(ModScreenHandlers.BASIC_ALLOY_FURNACE_SCREEN_HANDLER, AlloyFurnaceScreen::new);

        BlockEntityRendererFactories.register(
                ModBlockEntities.bellows,
                ctx -> new BellowsBlockEntityRenderer(ctx)
        );
	}
}