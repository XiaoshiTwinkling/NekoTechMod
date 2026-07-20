package com.nekotech.renderer;

import com.nekotech.NekoTechnology;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.Identifier;

public final class CatCameraModels {
    public static final Identifier CAMERA_MODEL_ID =
            Identifier.of(NekoTechnology.MOD_ID, "item/neko_cat_camera");

    private CatCameraModels() {}

    public static void initialize() {
        ModelLoadingPlugin.register(context -> context.addModels(CAMERA_MODEL_ID));
    }

    public static BakedModel getCameraModel() {
        return ((FabricBakedModelManager) MinecraftClient.getInstance().getBakedModelManager())
                .getModel(CAMERA_MODEL_ID);
    }
}
