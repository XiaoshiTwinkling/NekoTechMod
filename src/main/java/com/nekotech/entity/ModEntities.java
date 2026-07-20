package com.nekotech.entity;

import com.nekotech.NekoTechnology;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final EntityType<CatCameraBodyEntity> CAT_CAMERA_BODY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NekoTechnology.MOD_ID, "cat_camera_body"),
            EntityType.Builder.create(CatCameraBodyEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F)
                    .maxTrackingRange(10)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .disableSummon()
                    .build("neko-technology:cat_camera_body")
    );

    private ModEntities() {}

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(CAT_CAMERA_BODY, CatCameraBodyEntity.createAttributes());
    }
}
