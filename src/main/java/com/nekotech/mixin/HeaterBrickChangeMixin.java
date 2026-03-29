package com.nekotech.mixin;

import com.nekotech.block.entity.machines.HeaterBlockEntity;
import com.nekotech.modTags.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class HeaterBrickChangeMixin {

    @Inject(method = "onBlockChanged", at = @At("TAIL"))
    private void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {

        World world = (World)(Object)this;

        if (world.isClient) return;

        // 只在 heater brick 变化时触发
        if (oldState.isIn(ModTags.Blocks.HEATER_BRICKS)
                || newState.isIn(ModTags.Blocks.HEATER_BRICKS)) {

            notifyNearbyHeaters(world, pos);
        }
    }

    private static void notifyNearbyHeaters(World world, BlockPos pos) {
        int range = 3;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {

                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);

                    if (world.getBlockEntity(mutable) instanceof HeaterBlockEntity heater) {
                        heater.onStructureChanged(); // 仅触发一次刷新
                    }
                }
            }
        }
    }
}