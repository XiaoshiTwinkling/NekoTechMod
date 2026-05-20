package com.nekotech.block.entity;

import com.mojang.datafixers.types.Type;
import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.machines.*;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import com.nekotech.item.block.ModBlocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ModBlockEntities {

    public static final BlockEntityType<ElevatorCoreBlockEntity> ELEVATOR_CORE_BLOCK_ENTITY =
            create("elevator_core",
                    BlockEntityType.Builder.create(
                            ElevatorCoreBlockEntity::new,
                            ModBlocks.ELEVATOR_CORE_BLOCK
                    ));

    public static BlockEntityType<BoxBlockEntity> basic_storage_enclosure=
            create("basic_storage_enclosure", BlockEntityType.Builder.create(BoxBlockEntity::new, ModBlocks.basic_storage_enclosure));

    public static BlockEntityType<HeaterBlockEntity> heater=
            create("heater", BlockEntityType.Builder.create(HeaterBlockEntity::new, ModBlocks.heater));

    public static BlockEntityType<BellowsBlockEntity> bellows =
            create("bellows",
                    BlockEntityType.Builder.create(
                            BellowsBlockEntity::new,
                            ModBlocks.bellows
                    ));

    public static BlockEntityType<CushionBlockEntity> cushion =
            create("cushion",
                    BlockEntityType.Builder.create(
                            CushionBlockEntity::new,
                            ModBlocks.cushion_block
                    ));

    public static BlockEntityType<AlloyPotBlockEntity> alloy_pot =
            create("alloy_pot", BlockEntityType.Builder.create(AlloyPotBlockEntity::new, ModBlocks.alloy_pot));

    public static BlockEntityType<FluxStorageBlockEntity> flux_storage =
            create("flux_storage", BlockEntityType.Builder.create(FluxStorageBlockEntity::new, ModBlocks.flux_storage));

    public static BlockEntityType<MachineCasingBlockEntity> wooden_casing =
            create("wooden_casing", BlockEntityType.Builder.create(MachineCasingBlockEntity::new, ModBlocks.wooden_casing));

    public static BlockEntityType<WorkBenchBlockEntity> work_bench =
            create("work_bench", BlockEntityType.Builder.create(WorkBenchBlockEntity::new, ModBlocks.work_bench));

    public static BlockEntityType<CoilBlockEntity> coil_block =
            create("coil_block", BlockEntityType.Builder.create(CoilBlockEntity::new, ModBlocks.coil_block));



    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder){
        Type<?> type = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(NekoTechnology.MOD_ID, id), builder.build(type));
    }
    public static void registerBlockEntities(){

    }
}
