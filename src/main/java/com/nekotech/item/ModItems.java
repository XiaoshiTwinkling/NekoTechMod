package com.nekotech.item;

import com.nekotech.NekoTechnology;
import com.nekotech.item.custom.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.component.type.FoodComponent;


public class ModItems {


    public static final Item neko_silk= registerItems("neko_silk", new ModItem(new Item.Settings(),"neko_silk"));
    public static final Item neko_hair= registerItems("neko_hair", new ModItem(new Item.Settings(),"neko_hair"));
    public static final Item enhanced_neko_hair= registerItems("enhanced_neko_hair", new ModItem(new Item.Settings(),"enhanced_neko_hair"));
    public static final Item stone_powder= registerItems("stone_powder", new Item(new Item.Settings()));
    public static final Item neko_feather= registerItems("neko_feather", new ModItem(new Item.Settings(),"neko_feather"));
//    public static final Item neko_arrow= registerItems("neko_arrow", new Item(new Item.Settings()));
    public static final Item dried_fish= registerItems("dried_fish", new ModItem(new Item.Settings().food(ModFoodComponents.DRIED_FISH),"dried_fish"));
    public static final Item burnt_fish= registerItems("burnt_fish", new ModItem(new Item.Settings().food(ModFoodComponents.BURNT_FISH),"burnt_fish"));
    public static final Item tin_can= registerItems("tin_can", new Item(new Item.Settings()));
    public static final Item hammer = registerItems("hammer", new Hammer(new Item.Settings()));
    public static final Item pig_iron_ingot= registerItems("pig_iron_ingot", new Item(new Item.Settings()));
    public static final Item pig_iron_plate= registerItems("pig_iron_plate", new Item(new Item.Settings()));
    public static final Item fish_can= registerItems("fish_can", new ModItem(new Item.Settings().food(new FoodComponent.Builder()
            .nutrition(12)
            .saturationModifier(0.8F)
            .usingConvertsTo(tin_can)
            .build()
    ), "fish_can"));
    public static final Item neko_web= registerItems("neko_web", new Item(new Item.Settings()));
    public static final Item neko_copper_ingot= registerItems("neko_copper_ingot", new ModItem(new Item.Settings(),"neko_copper_ingot"));
    public static final Item slag= registerItems("slag", new Item(new Item.Settings()));
    public static final Item small_handful_of_slag= registerItems("small_handful_of_slag", new Item(new Item.Settings()));
    public static final Item neko_copper_plate= registerItems("neko_copper_plate", new Item(new Item.Settings()));
    public static final Item neko_ears = registerItems("neko_ears", new HatItem(HatItem.Type.HAT,
            new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5))));
    public static final Item neko_goggles = registerItems("neko_goggles", new GogglesItem(HatItem.Type.HAT,
            new Item.Settings().maxDamage(HatItem.Type.HAT.getMaxDamage(5))));

    public static final Item NEKO_COPPER_HELMET = registerItems("neko_copper_helmet", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.HELMET,
            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_helmet"));
    public static final Item NEKO_COPPER_CHESTPLATE = registerItems("neko_copper_chestplate", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.CHESTPLATE,
            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_chestplate"));
    public static final Item NEKO_COPPER_LEGGINGS = registerItems("neko_copper_leggings", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.LEGGINGS,
            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_leggings"));
    public static final Item NEKO_COPPER_BOOTS = registerItems("neko_copper_boots", new ModArmorItem(ModArmorMaterials.NEKO_COPPER, ArmorItem.Type.BOOTS,
            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)),"neko_copper_boots"));
    public static final Item neko_box = registerItems("neko_box", new CatBoxItem(new Item.Settings()));
    public static final Item thermometer = registerItems("thermometer", new Thermometer(new Item.Settings(),"thermometer"));



    public static final Item NEKO_TAIL = registerItems(
            "neko_tail",
            new TailItem(
                    TailItem.Type.TAIL,
                    new Item.Settings().maxCount(1)
            )
    );


    private static Item registerItems(String id, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(NekoTechnology.MOD_ID ,id), item);
    }

    private static void addItemToIG(FabricItemGroupEntries fabricItemGroupEntries){

    }

    public static void registerModItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemToIG);
        NekoTechnology.LOGGER.info("Registering Mod Items");
    }


}
