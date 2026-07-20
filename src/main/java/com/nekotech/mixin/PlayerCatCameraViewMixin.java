package com.nekotech.mixin;

import com.nekotech.catcamera.CatCameraViewManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerCatCameraViewMixin {
    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    private void keepCatCameraAttached(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && CatCameraViewManager.isViewing(player)) {
            cir.setReturnValue(false);
        }
    }
}
