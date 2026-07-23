package com.nekotech.item;

import com.nekotech.NekoTechnology;
import com.nekotech.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModItemGroups {

    public static final RegistryKey<ItemGroup> NekoTech_INGREDIENTS = register("nekotech_ingredients");  //材料
    public static final RegistryKey<ItemGroup> NekoTech_TOOLS = register("nekotech_tools");  //材料
    public static final RegistryKey<ItemGroup> NekoTech_BLOCKS = register("nekotech_blocks"); //方块

    private static RegistryKey<ItemGroup> register(String id) {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(NekoTechnology.MOD_ID,id));
    }

    public static void registerModItemGroups() {
        Registry.register(Registries.ITEM_GROUP, NekoTech_INGREDIENTS,
                ItemGroup.create(ItemGroup.Row.TOP, 7)
                        .displayName(Text.translatable("itemGroup.nekotech_ingredients"))
                        .icon(() -> new ItemStack(ModItems.NEKO_SILK))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModBlocks.ALUMINUM_BLOCK);
                            entries.add(ModItems.ALUMINUM_INGOT);
                            entries.add(ModItems.ALUMINUM_NUGGET);
                            entries.add(ModBlocks.ALUMINUM_ORE);
                            entries.add(ModBlocks.DEEPSLATE_ALUMINUM_ORE);
                            entries.add(ModItems.RAW_ALUMINUM);
                            entries.add(ModBlocks.RAW_ALUMINUM_BLOCK);
                            entries.add(ModBlocks.TIN_BLOCK);
                            entries.add(ModItems.TIN_INGOT);
                            entries.add(ModItems.TIN_NUGGET);
                            entries.add(ModBlocks.TIN_ORE);
                            entries.add(ModBlocks.DEEPSLATE_TIN_ORE);
                            entries.add(ModItems.RAW_TIN);
                            entries.add(ModBlocks.RAW_TIN_BLOCK);
                            entries.add(ModBlocks.PIG_IRON_BLOCK);
                            entries.add(ModItems.PIG_IRON_INGOT);
                            entries.add(ModItems.PIG_IRON_NUGGET);
                            entries.add(ModItems.PIG_IRON_PLATE);
                            entries.add(ModBlocks.NEKO_COPPER_BLOCK);
                            entries.add(ModItems.NEKO_COPPER_INGOT);
                            entries.add(ModItems.NEKO_COPPER_NUGGET);
                            entries.add(ModItems.NEKO_COPPER_PLATE);
                            entries.add(ModBlocks.BRASS_BLOCK);
                            entries.add(ModItems.BRASS_INGOT);
                            entries.add(ModItems.BRASS_NUGGET);

                            entries.add(ModItems.NEKO_SILK);
                            entries.add(ModItems.NEKO_HAIR);
                            entries.add(ModItems.ENHANCED_NEKO_HAIR);
                            entries.add(ModItems.STONE_POWDER);
                            entries.add(ModItems.NEKO_FEATHER);
                            entries.add(ModItems.DRIED_FISH);
                            entries.add(ModItems.BURNT_FISH);
                            entries.add(ModItems.TIN_CAN);
                            entries.add(ModItems.FISH_CAN);
                            entries.add(ModItems.NEKO_WEB);
                            entries.add(ModItems.SLAG);
                            entries.add(ModItems.SMALL_HANDFUL_OF_SLAG);
                            entries.add(ModItems.PINK_LEN);
                            entries.add(ModItems.CATNIP);
                            entries.add(ModItems.CATNIP_SEEDS);
                            entries.add(ModItems.PETGRASS);
                            entries.add(ModItems.PETGRASS_SEEDS);
                        })).build());

        Registry.register(Registries.ITEM_GROUP, NekoTech_TOOLS,
                ItemGroup.create(ItemGroup.Row.TOP, 7)
                        .displayName(Text.translatable("itemGroup.nekotech_tools"))
                        .icon(() -> new ItemStack(ModItems.PINK_NEKO_GOGGLES))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModItems.HAMMER);
                            entries.add(ModItems.NEKO_EARS);
                            entries.add(ModItems.NEKO_COPPER_HELMET);
                            entries.add(ModItems.NEKO_COPPER_CHESTPLATE);
                            entries.add(ModItems.NEKO_COPPER_LEGGINGS);
                            entries.add(ModItems.NEKO_COPPER_BOOTS);
                            entries.add(ModItems.NEKO_BOX);
                            entries.add(ModItems.NEKO_TAIL);
                            entries.add(ModItems.THERMOMETER);
                            entries.add(ModItems.PINK_NEKO_GOGGLES);
                            entries.add(ModItems.BLUE_NEKO_GOGGLES);
                            entries.add(ModItems.YELLOW_NEKO_GOGGLES);
                            entries.add(ModItems.GREEN_NEKO_GOGGLES);
                            entries.add(ModItems.RAY_ITEM);
                            entries.add(ModItems.NEKO_TAG);
                            entries.add(ModItems.COMPONENT_CASING);
                            entries.add(ModItems.BRASS_ITEM_INPUTER);
                            entries.add(ModItems.BRASS_ITEM_OUTPUTER);
                            entries.add(ModItems.BRASS_FLUX_OUTPUTER);
                            entries.add(ModItems.BRASS_FLUX_INPUTER);
                            entries.add(ModItems.NEKO_COPPER_FLUX_OUTPUTER);
                            entries.add(ModItems.NEKO_COPPER_FLUX_INPUTER);
                            entries.add(ModItems.WIRE_POLE);
                            entries.add(ModItems.GLASS_COVER);
                            entries.add(ModItems.COPPER_WIRE_BUNDLE);
                            entries.add(ModItems.BRASS_WIRE_BUNDLE);
                            entries.add(ModItems.NEKO_COPPER_WIRE_BUNDLE);
                            entries.add(ModItems.COPPER_COIL);
                            entries.add(ModItems.PIG_IRON_COIL);
                            entries.add(ModItems.NEKO_COPPER_COIL);
                            entries.add(ModItems.PIG_IRON_FRAMEWORK);
                            entries.add(ModItems.EMPTY_NEKO_MARK);
                            entries.add(ModItems.WHITE_NEKO_MARK);
                            entries.add(ModItems.ORANGE_NEKO_MARK);
                            entries.add(ModItems.MAGENTA_NEKO_MARK);
                            entries.add(ModItems.LIGHT_BLUE_NEKO_MARK);
                            entries.add(ModItems.YELLOW_NEKO_MARK);
                            entries.add(ModItems.LIME_NEKO_MARK);
                            entries.add(ModItems.PINK_NEKO_MARK);
                            entries.add(ModItems.GRAY_NEKO_MARK);
                            entries.add(ModItems.LIGHT_GRAY_NEKO_MARK);
                            entries.add(ModItems.CYAN_NEKO_MARK);
                            entries.add(ModItems.PURPLE_NEKO_MARK);
                            entries.add(ModItems.BLUE_NEKO_MARK);
                            entries.add(ModItems.BROWN_NEKO_MARK);
                            entries.add(ModItems.GREEN_NEKO_MARK);
                            entries.add(ModItems.RED_NEKO_MARK);
                            entries.add(ModItems.BLACK_NEKO_MARK);
                            entries.add(ModItems.NEKO_TAG_READER);
                            entries.add(ModItems.CAT_CAMERA_TERMINAL);
                            entries.add(ModItems.CAT_CAMERA);

                            entries.add(ModItems.COPPER_BATTERY);
                            entries.add(ModItems.ALUMINUM_BATTERY);
                            entries.add(ModItems.PIG_IRON_BATTERY);
                            entries.add(ModItems.BRASS_BATTERY);
                            entries.add(ModItems.NEKO_COPPER_BATTERY);
                        })).build());

        Registry.register(Registries.ITEM_GROUP, NekoTech_BLOCKS,
                ItemGroup.create(ItemGroup.Row.TOP, 7)
                        .displayName(Text.translatable("itemGroup.nekotech_blocks"))
                        .icon(() -> new ItemStack(ModBlocks.HEATER))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModBlocks.CUSHION_BLOCK);
                            entries.add(ModBlocks.CAT_HOUSE);
                            entries.add(ModBlocks.BELLOWS);
                            entries.add(ModBlocks.HEATER);
                            entries.add(ModBlocks.ALLOY_POT);
                            entries.add(ModBlocks.WOODEN_CASING);
                            entries.add(ModBlocks.FLUX_STORAGE);
                            entries.add(ModBlocks.CAT_GENERATOR);
                            entries.add(ModBlocks.WORK_BENCH);
                            entries.add(ModBlocks.ADVANCED_WORK_BENCH);
                            entries.add(ModBlocks.COIL_BLOCK);
                            entries.add(ModItems.ELEVATOR_ITEM);
                            entries.add(ModBlocks.CIRCUIT_BREAKER);
                            entries.add(ModBlocks.BEACON_DIFFUSER);
                            entries.add(ModBlocks.CHARGE_STATION);
                       })).build());



        NekoTechnology.LOGGER.info("Registering ModItemGroups");
    }

}
