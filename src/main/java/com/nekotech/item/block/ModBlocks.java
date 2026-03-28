package com.nekotech.item.block;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.machines.HeaterBlockEntity;
import com.nekotech.item.custom.*;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block basic_alloy_furnace= register("basic_alloy_furnace", new AlloyFurnace(AbstractBlock.Settings.create()
            .strength(1.5F, 6.0F)));

    public static final Block basic_storage_enclosure= register("basic_storage_enclosure", new BoxBlock(AbstractBlock.Settings.copy(Blocks.CHEST)
            .strength(1.5F, 6.0F), () -> ModBlockEntities.basic_storage_enclosure));

    public static final Block cushion_block= register("cushion_block", new Cushion(
            FabricBlockSettings.create()
                    .strength(0.3f)  // 软垫子
                    .sounds(BlockSoundGroup.WOOL)
                    .nonOpaque()
    ));

    public static final Block heater= register("heater", new Heater(AbstractBlock.Settings.create()
            .strength(1.5F, 6.0F)));

    public static final Block bellows = register("bellows",
            new Bellows(AbstractBlock.Settings.create()
                    .strength(1.5f)
            )
    );

    public static void registerBlockItems(String id, Block block){
        Item item = Registry.register(Registries.ITEM, Identifier.of(NekoTechnology.MOD_ID, id), new BlockItem(block, new Item.Settings()));
        if(item instanceof BlockItem){
            ((BlockItem)item).appendBlocks(Item.BLOCK_ITEMS, item);
        }
    }

    public static Block register(String id, Block block){
        registerBlockItems(id, block);
        return Registry.register(Registries.BLOCK, Identifier.of(NekoTechnology.MOD_ID, id), block);
    }
    public static void registerModBlocks(){
        NekoTechnology.LOGGER.info("Registering Mod Blocks");
    }
}
