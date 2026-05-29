package com.nekotech.item;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.ModBlocks;
import com.nekotech.item.custom.*;
import com.nekotech.item.custom.NekoMark.NekoMarkItem;
import com.nekotech.item.custom.NekoTag.NekoTagItem;
import com.nekotech.item.custom.component.AbstractComponentItem;
import com.nekotech.item.custom.component.FluxInputerItem;
import com.nekotech.item.custom.component.FluxOutputerItem;
import com.nekotech.item.custom.component.ItemInputerItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

import static net.minecraft.item.Items.register;


public class ModItems {


    public static final Item NEKO_SILK = registerItems("neko_silk", new ModItem(new Item.Settings(),"neko_silk"));
    public static final Item NEKO_HAIR = registerItems("neko_hair", new ModItem(new Item.Settings(),"neko_hair"));
    public static final Item ENHANCED_NEKO_HAIR = registerItems("enhanced_neko_hair", new ModItem(new Item.Settings(),"enhanced_neko_hair"));
    public static final Item STONE_POWDER = registerItems("stone_powder", new Item(new Item.Settings()));
    public static final Item NEKO_FEATHER = registerItems("neko_feather", new ModItem(new Item.Settings(),"neko_feather"));
    public static final Item DRIED_FISH = registerItems("dried_fish", new ModItem(new Item.Settings().food(ModFoodComponents.DRIED_FISH),"dried_fish"));
    public static final Item BURNT_FISH = registerItems("burnt_fish", new ModItem(new Item.Settings().food(ModFoodComponents.BURNT_FISH),"burnt_fish"));
    public static final Item TIN_CAN = registerItems("tin_can", new Item(new Item.Settings()));
    public static final Item HAMMER = registerItems("hammer", new Hammer(new Item.Settings()));
    public static final Item PIG_IRON_INGOT = registerItems("pig_iron_ingot", new Item(new Item.Settings()));
    public static final Item PIG_IRON_PLATE = registerItems("pig_iron_plate", new Item(new Item.Settings()));
    public static final Item FISH_CAN = registerItems("fish_can", new ModItem(new Item.Settings().food(new FoodComponent.Builder().nutrition(12).saturationModifier(0.8F).usingConvertsTo(TIN_CAN).build()), "fish_can"));
    public static final Item NEKO_WEB = registerItems("neko_web", new Item(new Item.Settings()));
    public static final Item PINK_LEN = registerItems("pink_len", new Item(new Item.Settings()));
    public static final Item NEKO_COPPER_INGOT = registerItems("neko_copper_ingot", new ModItem(new Item.Settings(),"neko_copper_ingot"));
    public static final Item SLAG = registerItems("slag", new Item(new Item.Settings()));
    public static final Item SMALL_HANDFUL_OF_SLAG = registerItems("small_handful_of_slag", new Item(new Item.Settings()));
    public static final Item NEKO_COPPER_PLATE = registerItems("neko_copper_plate", new Item(new Item.Settings()));
    public static final Item PETGRASS = registerItems("petgrass", new Item(new Item.Settings()));
    public static final Item PETGRASS_SEEDS = registerItems("petgrass_seeds", new AliasedBlockItem(ModBlocks.PETGRASS_CROP, new Item.Settings()));public static final Item CATNIP = registerItems("catnip", new CatnipItem(new Item.Settings().food(new FoodComponent.Builder().saturationModifier(0.1f).build()), "catnip"));
    public static final Item CATNIP_SEEDS = registerItems("catnip_seeds", new AliasedBlockItem(ModBlocks.CATNIP_CROP, new Item.Settings()));


    public static final Item NEKO_BOX = registerItems("neko_box", new CatBoxItem(new Item.Settings()));
    public static final Item THERMOMETER = registerItems("thermometer", new Thermometer(new Item.Settings(),"thermometer"));
    public static final Item RAY_ITEM = registerItems("ray_item", new RayItem(new Item.Settings().maxCount(1)));
    public static final Item NEKO_TAG = registerItems("neko_tag", new NekoTagItem(new Item.Settings().maxCount(1), "neko_tag"));
    public static final Item GLASS_COVER = registerItems("glass_cover", new GlassCover(new Item.Settings(),"glass_cover"));
    public static final Item ELEVATOR_ITEM = registerItems("elevator", new ElevatorItem(new Item.Settings().maxCount(16)));
    public static final Item PIG_IRON_FRAMEWORK = registerItems("pig_iron_framework", new ModItem(new Item.Settings(), "pig_iron_framework"));

    public static final Item NEKO_EARS = registerItems("neko_ears", new HatItem(HatItem.Type.HAT, new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5)), "neko_ears"));
    public static final Item NEKO_TAIL = registerItems("neko_tail", new TailItem(TailItem.Type.TAIL, new Item.Settings().maxCount(1), "neko_tail"));
    public static final Item NEKO_COPPER_HELMET = registerItems("neko_copper_helmet", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_helmet"));
    public static final Item NEKO_COPPER_CHESTPLATE = registerItems("neko_copper_chestplate", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_chestplate"));
    public static final Item NEKO_COPPER_LEGGINGS = registerItems("neko_copper_leggings", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.LEGGINGS, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_leggings"));
    public static final Item NEKO_COPPER_BOOTS = registerItems("neko_copper_boots", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.BOOTS, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_boots"));

    public static final Item PINK_NEKO_GOGGLES = registerItems("pink_neko_goggles", new GogglesItem(HatItem.Type.HAT, new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5)), "neko_goggles"));
    public static final Item BLUE_NEKO_GOGGLES = registerItems("blue_neko_goggles", new GogglesItem(HatItem.Type.HAT, new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5)), "neko_goggles"));
    public static final Item YELLOW_NEKO_GOGGLES = registerItems("yellow_neko_goggles", new GogglesItem(HatItem.Type.HAT, new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5)), "neko_goggles"));
    public static final Item GREEN_NEKO_GOGGLES = registerItems("green_neko_goggles", new GogglesItem(HatItem.Type.HAT, new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5)), "neko_goggles"));

    private static final Set<Item> allComponents = Collections.synchronizedSet(new LinkedHashSet<>()); //所有零件
    public static final Item COMPONENT_CASING = registerComponent("component_casing", new AbstractComponentItem(new Item.Settings().maxCount(16), "component_casing") {
        @Override public void useComponent(World world, ComponentAdaptation self, Direction side) {}
    });
    public static final Item BRASS_ITEM_INPUTER = registerComponent("brass_item_inputer", new ItemInputerItem(1f, "brass_item_inputer"));
    public static final Item BRASS_FLUX_OUTPUTER = registerComponent("brass_flux_outputer", new FluxOutputerItem(0.1f, "brass_flux_outputer"));
    public static final Item BRASS_FLUX_INPUTER = registerComponent("brass_flux_inputer", new FluxInputerItem(0.1f, "brass_flux_inputer"));
    public static final Item NEKO_COPPER_FLUX_OUTPUTER = registerComponent("neko_copper_flux_outputer", new FluxOutputerItem(0.4f, "neko_copper_flux_outputer"));
    public static final Item NEKO_COPPER_FLUX_INPUTER = registerComponent("neko_copper_flux_inputer", new FluxInputerItem(0.4f, "neko_copper_flux_inputer"));

    public static final Item COPPER_COIL = registerItems("copper_coil", new ModItem(new Item.Settings(), "copper_coil"));
    public static final Item PIG_IRON_COIL = registerItems("pig_iron_coil", new ModItem(new Item.Settings(), "pig_iron_coil"));
    public static final Item NEKO_COPPER_COIL = registerItems("neko_copper_coil", new ModItem(new Item.Settings(), "neko_copper_coil"));

    public static final Item EMPTY_NEKO_MARK = registerItems("empty_neko_mark", new Item(new Item.Settings()));
    public static final Item WHITE_NEKO_MARK = registerItems("white_neko_mark", new NekoMarkItem(new Item.Settings(), "white_neko_mark", DyeColor.WHITE));
    public static final Item ORANGE_NEKO_MARK = registerItems("orange_neko_mark", new NekoMarkItem(new Item.Settings(), "orange_neko_mark", DyeColor.ORANGE));
    public static final Item MAGENTA_NEKO_MARK = registerItems("magenta_neko_mark", new NekoMarkItem(new Item.Settings(), "magenta_neko_mark", DyeColor.MAGENTA));
    public static final Item LIGHT_BLUE_NEKO_MARK = registerItems("light_blue_neko_mark", new NekoMarkItem(new Item.Settings(), "light_blue_neko_mark", DyeColor.LIGHT_BLUE));
    public static final Item YELLOW_NEKO_MARK = registerItems("yellow_neko_mark", new NekoMarkItem(new Item.Settings(), "yellow_neko_mark", DyeColor.YELLOW));
    public static final Item LIME_NEKO_MARK = registerItems("lime_neko_mark", new NekoMarkItem(new Item.Settings(), "lime_neko_mark", DyeColor.LIME));
    public static final Item PINK_NEKO_MARK = registerItems("pink_neko_mark", new NekoMarkItem(new Item.Settings(), "pink_neko_mark", DyeColor.PINK));
    public static final Item GRAY_NEKO_MARK = registerItems("gray_neko_mark", new NekoMarkItem(new Item.Settings(), "gray_neko_mark", DyeColor.GRAY));
    public static final Item LIGHT_GRAY_NEKO_MARK = registerItems("light_gray_neko_mark", new NekoMarkItem(new Item.Settings(), "light_gray_neko_mark", DyeColor.LIGHT_GRAY));
    public static final Item CYAN_NEKO_MARK = registerItems("cyan_neko_mark", new NekoMarkItem(new Item.Settings(), "cyan_neko_mark", DyeColor.CYAN));
    public static final Item PURPLE_NEKO_MARK = registerItems("purple_neko_mark", new NekoMarkItem(new Item.Settings(), "purple_neko_mark", DyeColor.PURPLE));
    public static final Item BLUE_NEKO_MARK = registerItems("blue_neko_mark", new NekoMarkItem(new Item.Settings(), "blue_neko_mark", DyeColor.BLUE));
    public static final Item BROWN_NEKO_MARK = registerItems("brown_neko_mark", new NekoMarkItem(new Item.Settings(), "brown_neko_mark", DyeColor.BROWN));
    public static final Item GREEN_NEKO_MARK = registerItems("green_neko_mark", new NekoMarkItem(new Item.Settings(), "green_neko_mark", DyeColor.GREEN));
    public static final Item RED_NEKO_MARK = registerItems("red_neko_mark", new NekoMarkItem(new Item.Settings(), "red_neko_mark", DyeColor.RED));
    public static final Item BLACK_NEKO_MARK = registerItems("black_neko_mark", new NekoMarkItem(new Item.Settings(), "black_neko_mark", DyeColor.BLACK));



    private static Item registerItems(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(NekoTechnology.MOD_ID ,id), item);
    }

    private static void addItemToIG(FabricItemGroupEntries fabricItemGroupEntries){

    }

    public static void registerModItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemToIG);
        NekoTechnology.LOGGER.info("Registering Mod Items");
    }

    public static <T extends AbstractComponentItem> T registerComponent(String name, T component) {
        Identifier id = Identifier.of(NekoTechnology.MOD_ID, name);
        allComponents.add(component);
        return Registry.register(Registries.ITEM, id, component);
    }

    public static Set<Item> getAllComponents() {
        return allComponents;
    }

}
