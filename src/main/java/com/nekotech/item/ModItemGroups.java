package com.nekotech.item;

import com.nekotech.NekoTechnology;
import com.nekotech.item.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

import javax.swing.text.html.parser.Entity;

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
                        .icon(() -> new ItemStack(ModItems.neko_silk))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModItems.neko_silk);
                            entries.add(ModItems.neko_hair);
                            entries.add(ModItems.enhanced_neko_hair);
                            entries.add(ModItems.stone_powder);
                            entries.add(ModItems.neko_feather);
                            entries.add(ModItems.dried_fish);
                            entries.add(ModItems.burnt_fish);
                            entries.add(ModItems.tin_can);
                            entries.add(ModItems.pig_iron_ingot);
                            entries.add(ModItems.pig_iron_plate);
                            entries.add(ModItems.fish_can);
                            entries.add(ModItems.neko_web);
                            entries.add(ModItems.neko_copper_ingot);
                            entries.add(ModItems.slag);
                            entries.add(ModItems.small_handful_of_slag);
                            entries.add(ModItems.neko_copper_plate);
                            entries.add(ModItems.pink_len);
                        })).build());

        Registry.register(Registries.ITEM_GROUP, NekoTech_TOOLS,
                ItemGroup.create(ItemGroup.Row.TOP, 7)
                        .displayName(Text.translatable("itemGroup.nekotech_tools"))
                        .icon(() -> new ItemStack(ModItems.NEKO_COPPER_CHESTPLATE))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModItems.hammer);
                            //entries.add(ModItems.neko_arrow);
                            entries.add(ModItems.neko_ears);
                            entries.add(ModItems.NEKO_COPPER_HELMET);
                            entries.add(ModItems.NEKO_COPPER_CHESTPLATE);
                            entries.add(ModItems.NEKO_COPPER_LEGGINGS);
                            entries.add(ModItems.NEKO_COPPER_BOOTS);
                            entries.add(ModItems.neko_box);
                            entries.add(ModItems.NEKO_TAIL);
                            entries.add(ModItems.thermometer);
                            entries.add(ModItems.neko_goggles);
                            entries.add(ModItems.ray_item);
                        })).build());

        Registry.register(Registries.ITEM_GROUP, NekoTech_BLOCKS,
                ItemGroup.create(ItemGroup.Row.TOP, 7)
                        .displayName(Text.translatable("itemGroup.nekotech_blocks"))
                        .icon(() -> new ItemStack(ModBlocks.heater))
                        .entries(((displayContext, entries) -> {
                            entries.add(ModBlocks.basic_storage_enclosure);
                            entries.add(ModBlocks.cushion_block);
                            entries.add(ModBlocks.bellows);
                            entries.add(ModBlocks.heater);
                            entries.add(ModBlocks.alloy_pot);

                        })).build());



        NekoTechnology.LOGGER.info("Registering ModItemGroups");
    }

}
