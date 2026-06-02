package com.nekotech.mixin.Accessor;

import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CatEntity.class)
public interface CatEntityAccessor {
    @Invoker("setCollarColor")
    void neko_technology$setCollarColor(DyeColor color);
}
