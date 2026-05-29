package com.nekotech.block.entity;

import com.mojang.datafixers.types.Type;
import com.nekotech.NekoTechnology;
import com.nekotech.block.custom.CatHouseBlock;
import com.nekotech.block.entity.machines.*;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import com.nekotech.block.entity.machines.conductor.CircuitBreakerBlockEntity;
import com.nekotech.block.entity.machines.conductor.FluxStorageBlockEntity;
import com.nekotech.block.entity.machines.conductor.MachineCasingBlockEntity;
import com.nekotech.block.custom.ModBlocks;
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
    public static BlockEntityType<HeaterBlockEntity> HEATER =
            create("heater", BlockEntityType.Builder.create(HeaterBlockEntity::new, ModBlocks.HEATER));

    public static BlockEntityType<BellowsBlockEntity> BELLOWS =
            create("bellows",
                    BlockEntityType.Builder.create(
                            BellowsBlockEntity::new,
                            ModBlocks.BELLOWS
                    ));
    public static BlockEntityType<CushionBlockEntity> CUSHION =
            create("cushion",
                    BlockEntityType.Builder.create(
                            CushionBlockEntity::new,
                            ModBlocks.CUSHION_BLOCK
                    ));

    public static BlockEntityType<AlloyPotBlockEntity> ALLOY_POT =
            create("alloy_pot", BlockEntityType.Builder.create(AlloyPotBlockEntity::new, ModBlocks.ALLOY_POT));

    public static BlockEntityType<FluxStorageBlockEntity> FLUX_STORAGE =
            create("flux_storage", BlockEntityType.Builder.create(FluxStorageBlockEntity::new, ModBlocks.FLUX_STORAGE));

    public static BlockEntityType<MachineCasingBlockEntity> WOODEN_CASING =
            create("wooden_casing", BlockEntityType.Builder.create(MachineCasingBlockEntity::new, ModBlocks.WOODEN_CASING));

    public static BlockEntityType<WorkBenchBlockEntity> WORK_BENCH =
            create("work_bench", BlockEntityType.Builder.create(WorkBenchBlockEntity::new, ModBlocks.WORK_BENCH));

    public static BlockEntityType<CoilBlockEntity> COIL_BLOCK =
            create("coil_block", BlockEntityType.Builder.create(CoilBlockEntity::new, ModBlocks.COIL_BLOCK));

    public static BlockEntityType<CircuitBreakerBlockEntity> CIRCUIT_BREAKER =
            create("circuit_breaker", BlockEntityType.Builder.create(CircuitBreakerBlockEntity::new, ModBlocks.CIRCUIT_BREAKER));

    public static BlockEntityType<CatHouseBlockEntity> CAT_HOUSE =
            create("cat_house", BlockEntityType.Builder.create(CatHouseBlockEntity::new, ModBlocks.CAT_HOUSE));

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder){
        Type<?> type = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(NekoTechnology.MOD_ID, id), builder.build(type));
    }
    public static void registerBlockEntities(){

    }
}
