package com.nekotech.screen;

import com.nekotech.NekoTechnology;
import com.nekotech.data.AlloyFurnaceData;
import com.nekotech.data.HeaterData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModScreenHandlers {
    public static final ScreenHandlerType<AlloyFurnaceScreenHandler> BASIC_ALLOY_FURNACE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(NekoTechnology.MOD_ID, "basic_alloy_furnace"),
                    new ExtendedScreenHandlerType<>(AlloyFurnaceScreenHandler::new, AlloyFurnaceData.CODEC));



    public static void registerScreenHandlers() {

    }

}
