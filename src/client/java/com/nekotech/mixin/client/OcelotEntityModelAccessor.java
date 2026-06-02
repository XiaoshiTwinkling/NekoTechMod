package com.nekotech.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OcelotEntityModel.class)
public interface OcelotEntityModelAccessor {
    @Accessor("head")
    ModelPart neko_technology$getHead();

    @Accessor("body")
    ModelPart neko_technology$getBody();
}
