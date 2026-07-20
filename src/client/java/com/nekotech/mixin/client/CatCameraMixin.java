package com.nekotech.mixin.client;

import com.nekotech.catcamera.CatCameraClientState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CatCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void updateCatCamera(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                   boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!CatCameraClientState.isActive() || !(focusedEntity instanceof CatEntity cat)
                || !cat.getUuid().equals(CatCameraClientState.getCatUuid())) return;

        float yaw = MathHelper.lerp(tickDelta, cat.prevHeadYaw, cat.headYaw);
        float pitch = MathHelper.lerp(tickDelta, cat.prevPitch, cat.getPitch());
        double x = MathHelper.lerp(tickDelta, cat.lastRenderX, cat.getX());
        double y = MathHelper.lerp(tickDelta, cat.lastRenderY, cat.getY()) + cat.getEyeHeight(cat.getPose()) + 0.46D;
        double z = MathHelper.lerp(tickDelta, cat.lastRenderZ, cat.getZ()) + cat.getEyeHeight(cat.getPose()) - 0.63D;
        double radians = Math.toRadians(yaw);
        x += -Math.sin(radians) * 0.10D;
        z += Math.cos(radians) * 0.10D;
        // 允许渲染作为摄像实体的猫咪
        this.thirdPerson = true;
        setRotation(yaw, pitch);
        setPos(x, y, z);
    }
}
