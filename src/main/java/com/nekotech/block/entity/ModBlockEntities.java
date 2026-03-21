package com.nekotech.block.entity;

import com.mojang.datafixers.types.Type;
import com.nekotech.NekoTechnology;
import com.nekotech.item.block.ModBlocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ModBlockEntities {

    public static BlockEntityType<BoxBlockEntity> basic_storage_enclosure=
            create("basic_storage_enclosure", BlockEntityType.Builder.create(BoxBlockEntity::new, ModBlocks.basic_storage_enclosure));

    public static BlockEntityType<AlloyFurnaceBlockEntity> basic_alloy_furnace=
            create("basic_alloy_furnace", BlockEntityType.Builder.create(AlloyFurnaceBlockEntity::new, ModBlocks.basic_alloy_furnace));

    public static BlockEntityType<BellowsBlockEntity> bellows =
            create("bellows",
                    BlockEntityType.Builder.create(
                            BellowsBlockEntity::new,
                            ModBlocks.bellows
                    ));



    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder){
        Type<?> type = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(NekoTechnology.MOD_ID, id), builder.build(type));
    }
    public static void registerBlockEntities(){

    }
}
