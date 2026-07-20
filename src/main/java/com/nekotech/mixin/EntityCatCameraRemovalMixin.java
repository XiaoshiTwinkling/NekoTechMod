package com.nekotech.mixin;

import com.nekotech.catcamera.CatCameraChannelAccess;
import com.nekotech.catcamera.CatCameraChannelService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityCatCameraRemovalMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void removeCatCameraChannelOnPermanentRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
                && self instanceof CatEntity cat
                && cat instanceof CatCameraChannelAccess access
                && access.neko_technology$isCatCameraChannelActive()) {
            CatCameraChannelService.delete(cat);
        }
    }
}
