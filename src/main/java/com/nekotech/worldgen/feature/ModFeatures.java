package com.nekotech.worldgen.feature;

import com.nekotech.NekoTechnology;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

public class ModFeatures {

    public static final Feature<DefaultFeatureConfig> PETGRASS_RIVER_SIDE =
            Registry.register(
                    Registries.FEATURE,
                    Identifier.of(NekoTechnology.MOD_ID, "petgrass_river_side"),
                    new PetgrassRiverSideFeature(DefaultFeatureConfig.CODEC)
            );

    public static void register() {
    }
}