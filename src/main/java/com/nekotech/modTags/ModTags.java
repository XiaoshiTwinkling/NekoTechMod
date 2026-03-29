package com.nekotech.modTags;


import com.nekotech.NekoTechnology;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block> HEATER_BRICKS =
                create("heater_bricks");

        private static TagKey<Block> create(String name) {
            return TagKey.of(RegistryKeys.BLOCK,
                    Identifier.of(NekoTechnology.MOD_ID, name));
        }
    }

    public static class Items {


        private static TagKey<Item> create(String name) {
            return TagKey.of(RegistryKeys.ITEM,
                    Identifier.of(NekoTechnology.MOD_ID, name));
        }
    }
}