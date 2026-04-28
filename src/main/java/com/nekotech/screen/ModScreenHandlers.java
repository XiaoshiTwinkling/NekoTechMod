package com.nekotech.screen;

import com.nekotech.screen.NekoTag.NekoTagScreenHandler;
import com.nekotech.NekoTechnology;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModScreenHandlers {
//    public static final ScreenHandlerType<AlloyFurnaceScreenHandler> BASIC_ALLOY_FURNACE_SCREEN_HANDLER =
//            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(NekoTechnology.MOD_ID, "basic_alloy_furnace"),
//                    new ExtendedScreenHandlerType<>(AlloyFurnaceScreenHandler::new, AlloyFurnaceData.CODEC));


    public static final ScreenHandlerType<NekoTagScreenHandler> NEKO_TAG =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(NekoTechnology.MOD_ID, "neko_tag"),
                    new ScreenHandlerType<>(NekoTagScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );


    public static void registerScreenHandlers() {

    }

}
