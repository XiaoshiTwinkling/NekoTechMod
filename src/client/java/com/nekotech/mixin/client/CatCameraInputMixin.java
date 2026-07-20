package com.nekotech.mixin.client;

import com.nekotech.catcamera.CatCameraClientState;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class CatCameraInputMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
    private void lockCatCameraViewActions(CallbackInfo ci) {
        if (CatCameraClientState.isActive()) ci.cancel();
    }
}
