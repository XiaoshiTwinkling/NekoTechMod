package com.nekotech.item.block;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.block.elevator.ElevatorCoreBlock;
import com.nekotech.item.block.elevator.ElevatorPartBlock;
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


    public static final Block basic_storage_enclosure= register("basic_storage_enclosure", new BoxBlock(AbstractBlock.Settings.copy(Blocks.CHEST)
            .strength(1.5F, 6.0F), () -> ModBlockEntities.basic_storage_enclosure));

    public static final Block cushion_block= register("cushion_block", new Cushion(
            FabricBlockSettings.create()
                    .strength(0.3f)  // 软垫子
                    .sounds(BlockSoundGroup.WOOL)
    ));

    public static final Block heater= register("heater", new Heater(AbstractBlock.Settings.create()
            .strength(1.5F, 6.0F)));

    public static final Block bellows = register("bellows",
            new Bellows(AbstractBlock.Settings.create()
                    .strength(1.5f)
            )
    );

    public static final Block alloy_pot = register("alloy_pot",
            new AlloyPot(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block flux_storage = register("flux_storage",
            new FluxStorage(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block wooden_casing = register("wooden_casing",
            new WoodenCasing(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block work_bench = register("work_bench",
            new WorkBench(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block coil_block = register("coil_block",
            new CoilBlock(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );
    public static final Block ELEVATOR_CORE_BLOCK = Registry.register(
            Registries.BLOCK,
            Identifier.of(NekoTechnology.MOD_ID, "elevator_core"),
            new ElevatorCoreBlock(AbstractBlock.Settings.create()
                    .strength(3.0F, 6.0F)
                    .nonOpaque())
    );

    public static final Block ELEVATOR_PART_BLOCK = Registry.register(
            Registries.BLOCK,
            Identifier.of(NekoTechnology.MOD_ID, "elevator_part"),
            new ElevatorPartBlock(AbstractBlock.Settings.create()
                    .strength(3.0F, 6.0F)
                    .nonOpaque())
    );


    public static void registerBlockItems(String id, Block block){
        BlockItem item = Registry.register(Registries.ITEM, Identifier.of(NekoTechnology.MOD_ID, id), new BlockItem(block, new Item.Settings()));
        item.appendBlocks(Item.BLOCK_ITEMS, item);
    }

    public static Block register(String id, Block block){
        registerBlockItems(id, block);
        return Registry.register(Registries.BLOCK, Identifier.of(NekoTechnology.MOD_ID, id), block);
    }
    public static void registerModBlocks(){
        NekoTechnology.LOGGER.info("Registering Mod Blocks");
    }

}
