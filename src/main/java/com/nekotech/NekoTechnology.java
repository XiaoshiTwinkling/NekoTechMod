package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.handler.DriedFishTameHandler;
import com.nekotech.item.ModItemGroups;
import com.nekotech.item.ModItems;
import com.nekotech.item.block.ModBlocks;
import com.nekotech.recipe.AlloyRecipe;
import com.nekotech.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;


public class NekoTechnology implements ModInitializer {
	public static final String MOD_ID = "neko-technology";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution



		ModItems.registerModItems();
		ModItemGroups.registerModItemGroups();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
		AlloyRecipe.RecipeManager.initDefaultRecipes();
		AlloyRecipe.RecipeManager.getAllRecipes();


		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			// 调用自定义处理逻辑
			return DriedFishTameHandler.handleTameAttempt(player, hand, entity);
		});

		LOGGER.info("Hello Fabric world!");

	}
}