package com.nekotech;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.conductor.ConductorManager;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.events.BlockBreakEvents;
import com.nekotech.catcamera.CatCameraViewManager;
import com.nekotech.entity.ModEntities;
import com.nekotech.events.ServerTick;
import com.nekotech.handler.DriedFishTameHandler;
import com.nekotech.item.ModItemGroups;
import com.nekotech.item.ModItems;
import com.nekotech.block.ModBlocks;
import com.nekotech.loot.LootInjection;
import com.nekotech.network.NetworkHandler;
import com.nekotech.recipe.ModRecipes;
import com.nekotech.screen.ModScreenHandlers;
import com.nekotech.util.DelayManager;
import com.nekotech.worldgen.ModWorldGeneration;
import com.nekotech.worldgen.feature.ModFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

/*
* Welcome to Nekotech
 */

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
		ModEntities.initialize();
		ModItemGroups.registerModItemGroups();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
        ModRecipes.init();
		NetworkHandler.initialize();
        ServerTick.init();
		DelayManager.initialize();
        BlockBreakEvents.register();
		new ConductorSystem().onInitialize();
        ModWorldGeneration.register();
        ModFeatures.register();
		LootInjection.register();

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			// 调用自定义处理逻辑
			return DriedFishTameHandler.handleTameAttempt(player, hand, entity);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			CatCameraViewManager.tick(server);
			for (ServerWorld world : server.getWorlds()) {
				ConductorManager manager = ConductorManager.get(world);
				manager.tick(world);
			}
		});

		LOGGER.info("Hello Fabric world!");

	}
}
