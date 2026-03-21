package com.nekotech.mixin;

import com.nekotech.item.ModItems;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;


@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Shadow private static volatile @Nullable Map<Item, Integer> fuelTimes;

    @Inject(method = "createFuelTimeMap", at= @At("TAIL"))
    private static void addFuelItem(CallbackInfoReturnable<Map<Item, Integer>> cir) {
        fuelTimes.put(ModItems.neko_hair, 25);
        fuelTimes.put(ModItems.neko_silk, 140);
        fuelTimes.put(ModItems.enhanced_neko_hair, 50);
        fuelTimes.put(ModItems.burnt_fish, 50);
    }
}
