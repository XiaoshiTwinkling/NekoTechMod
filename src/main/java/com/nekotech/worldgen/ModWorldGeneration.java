package com.nekotech.worldgen;

import com.nekotech.NekoTechnology;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModWorldGeneration {

    public static final RegistryKey<PlacedFeature> PETGRASS_RIVER_SIDE_PLACED_KEY =
            RegistryKey.of(
                    RegistryKeys.PLACED_FEATURE,
                    Identifier.of(NekoTechnology.MOD_ID, "petgrass_river_side")
            );

    public static final RegistryKey<PlacedFeature> TIN_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                    Identifier.of("neko-technology","tin_ore")
            );

    public static final RegistryKey<PlacedFeature> ALUMINUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                    Identifier.of("neko-technology","aluminum_ore")
            );


    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.VEGETAL_DECORATION,
                PETGRASS_RIVER_SIDE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                TIN_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                ALUMINUM_ORE_PLACED_KEY
        );
    }
}