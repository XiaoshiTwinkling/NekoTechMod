package com.nekotech;

import com.nekotech.screen.AlloyFurnaceScreen;
import com.nekotech.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class NekoTechnologyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NekoTechnology.LOGGER.info("NekoTechnologyClient initialized");
		HandledScreens.register(ModScreenHandlers.BASIC_ALLOY_FURNACE_SCREEN_HANDLER, AlloyFurnaceScreen::new);

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}