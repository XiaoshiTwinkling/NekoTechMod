package com.nekotech.mixin;

import com.nekotech.NekoTechnology;
import com.nekotech.item.ModItems;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CatEntity.class)
public class CatEntityMixin {
    @Unique
    private int nekoDropTimer = 0;

    @Unique
    private static final String DROP_TIMER_KEY = "neko_tech_drop_timer";

    @Unique
    private static final int DROP_INTERVAL = 10000;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        CatEntity cat = (CatEntity) (Object) this;

        if (!cat.getWorld().isClient() && cat.isAlive()) {
            nekoDropTimer++;

            if (nekoDropTimer >= DROP_INTERVAL) {
                ItemStack hairStack = new ItemStack(ModItems.neko_hair);
                cat.dropStack(hairStack);
                nekoDropTimer = 0;

                NekoTechnology.LOGGER.info("Cat dropped neko hair! Timer: {}", nekoDropTimer);
            }
        }
    }

    // 使用更简单的方法 - 直接修改实体的 NBT
    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void onWriteCustomData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt(DROP_TIMER_KEY, nekoDropTimer);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void onReadCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(DROP_TIMER_KEY)) {
            nekoDropTimer = nbt.getInt(DROP_TIMER_KEY);
        } else {
            nekoDropTimer = 0;
        }
    }
}