package com.nekotech.mixin;

import com.nekotech.NekoTechnology;
import com.nekotech.goal.MoveToLaserGoal;
import com.nekotech.item.ModItems;
import com.nekotech.mixin.Accessor.MobEntityAccessor;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatEntity.class)
public class CatEntityMixin {
    @Unique
    private int nekoDropTimer = 0;

    @Unique
    private static final String DROP_TIMER_KEY = "neko_tech_drop_timer";

    @Unique
    private static final int DROP_INTERVAL = 10000;

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addLaserGoal(CallbackInfo ci) {
        CatEntity cat = (CatEntity)(Object)this;

        GoalSelector goalSelector =
                ((MobEntityAccessor)this).getGoalSelector();

        if (goalSelector.getGoals().stream().noneMatch(
                g -> g.getGoal() instanceof MoveToLaserGoal)) {

            goalSelector.add(4, new MoveToLaserGoal(cat, 1.5));
        }
    }

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

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {

        ItemStack stack = player.getStackInHand(hand);

        if (!stack.isOf(ModItems.neko_box)) {
            return;
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}