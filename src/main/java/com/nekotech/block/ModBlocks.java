package com.nekotech.block;

import com.nekotech.NekoTechnology;
import com.nekotech.block.custom.*;
import com.nekotech.block.custom.crops.CatnipCropBlock;
import com.nekotech.block.custom.crops.PetgrassCropBlock;
import com.nekotech.block.custom.elevator.ElevatorCoreBlock;
import com.nekotech.block.custom.elevator.ElevatorPartBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;

public class ModBlocks {

    public static final Block CUSHION_BLOCK = register("cushion_block", new Cushion(
            FabricBlockSettings.create()
                    .strength(0.3f)
                    .sounds(BlockSoundGroup.WOOL)
    ));

    public static final Block CAT_HOUSE = register("cat_house", new CatHouseBlock(
            FabricBlockSettings.create()
                    .strength(0.3f)
                    .sounds(BlockSoundGroup.WOOD)
    ));

    public static final Block HEATER = register("heater", new Heater(AbstractBlock.Settings.create()
            .strength(1.5F, 6.0F)));

    public static final Block BELLOWS = register("bellows",
            new Bellows(AbstractBlock.Settings.create()
                    .strength(1.5f)
            )
    );

    public static final Block ALLOY_POT = register("alloy_pot",
            new AlloyPot(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block FLUX_STORAGE = register("flux_storage",
            new FluxStorage(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block CAT_GENERATOR = register("cat_generator",
            new CatGeneratorBlock(
                    Block.Settings.create()
                            .strength(3.5f)
                            .nonOpaque()
            )
    );

    public static final Block WOODEN_CASING = register("wooden_casing",
            new WoodenCasing(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block WORK_BENCH = register("work_bench",
            new WorkBench(
                    Block.Settings.create()
                            .strength(3.5f)
            )
    );

    public static final Block COIL_BLOCK = register("coil_block",
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

    public static final Block ELEVATOR_CORE_BOX = Registry.register(
            Registries.BLOCK,
            Identifier.of(NekoTechnology.MOD_ID, "elevator_core_box"),
            new Block(AbstractBlock.Settings.create()
                    .strength(3.0F, 6.0F)
                    .nonOpaque())
    );

    public static final Block ALUMINUM_ORE = register(
            "aluminum_ore",
            new ExperienceDroppingBlock(
                    ConstantIntProvider.create(0),
                    AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(3.0F, 3.0F)
            )
    );

    public static final Block DEEPSLATE_ALUMINUM_ORE = register(
            "deepslate_aluminum_ore",
            new ExperienceDroppingBlock(
                    ConstantIntProvider.create(0),
                    AbstractBlock.Settings.copyShallow(ALUMINUM_ORE).mapColor(MapColor.DEEPSLATE_GRAY).strength(4.5F, 3.0F).sounds(BlockSoundGroup.DEEPSLATE)
            )
    );

    public static final Block TIN_ORE = register(
            "tin_ore",
            new ExperienceDroppingBlock(
                    ConstantIntProvider.create(0),
                    AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(3.0F, 3.0F)
            )
    );

    public static final Block DEEPSLATE_TIN_ORE = register(
            "deepslate_tin_ore",
            new ExperienceDroppingBlock(
                    ConstantIntProvider.create(0),
                    AbstractBlock.Settings.copyShallow(TIN_ORE).mapColor(MapColor.DEEPSLATE_GRAY).strength(4.5F, 3.0F).sounds(BlockSoundGroup.DEEPSLATE)
            )
    );

    public static final Block RAW_ALUMINUM_BLOCK = register(
            "raw_aluminum_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block RAW_TIN_BLOCK = register(
            "raw_tin_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block PIG_IRON_BLOCK = register(
            "pig_iron_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block NEKO_COPPER_BLOCK = register(
            "neko_copper_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block BRASS_BLOCK = register(
            "brass_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block ALUMINUM_BLOCK = register(
            "aluminum_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block TIN_BLOCK = register(
            "tin_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).requiresTool().strength(5.0F, 6.0F))
    );

    public static final Block CIRCUIT_BREAKER = register("circuit_breaker",
            new CircuitBreakerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque()));

    public static final Block CATNIP_CROP = Registry.register(Registries.BLOCK, Identifier.of(NekoTechnology.MOD_ID, "catnip_crop"),
            new CatnipCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    public static final Block PETGRASS_CROP = Registry.register(Registries.BLOCK, Identifier.of(NekoTechnology.MOD_ID, "petgrass_crop"),
            new PetgrassCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));


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
