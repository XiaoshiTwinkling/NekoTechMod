package com.nekotech.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

public class ModFoodComponents {


    public static final FoodComponent DRIED_FISH = new FoodComponent.Builder().nutrition(6).saturationModifier(0.5f).build();
    public static final FoodComponent BURNT_FISH = new FoodComponent.Builder().nutrition(2).saturationModifier(0.5f)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0), 0.5f).build();
}
