package com.nekotech.mixin;

import com.nekotech.NekoTechnology;
import com.nekotech.goal.MoveToLaserGoal;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoMark.NekoMarkAccess;
import com.nekotech.mixin.Accessor.MobEntityAccessor;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatEntity.class)
public class CatEntityMixin implements NekoMarkAccess {
    @Unique
    private int nekoDropTimer = 0;

    @Unique
    private int nextDropInterval = 0;

    @Unique
    private static final String DROP_TIMER_KEY = "neko_tech_drop_timer";

    @Unique
    private static final String DROP_INTERVAL_KEY = "neko_tech_next_interval";

    @Unique
    private static final String NEKO_MARK_COLOR_KEY = "NekoMarkColor";

    @Unique
    private DyeColor nekoMarkColor = DyeColor.WHITE;


    @Unique
    private int getRandomInterval() {
        return 7500 + ((CatEntity)(Object)this).getRandom().nextInt(5001);
    }

    @Override
    public void neko_technology$setNekoMarkColor(DyeColor color) {
        this.nekoMarkColor = color;
    }

    @Override
    public DyeColor neko_technology$getNekoMarkColor() {
        return this.nekoMarkColor;
    }

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

            // 初始化一次
            if (nextDropInterval == 0) {
                nextDropInterval = getRandomInterval();
            }

            nekoDropTimer++;

            if (nekoDropTimer >= nextDropInterval) {
                ItemStack hairStack = new ItemStack(ModItems.neko_hair);
                cat.dropStack(hairStack);

                nekoDropTimer = 0;
                nextDropInterval = getRandomInterval();

                NekoTechnology.LOGGER.info(
                        "Cat dropped neko hair! Next interval: {}", nextDropInterval
                );
            }
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void onWriteCustomData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt(DROP_TIMER_KEY, nekoDropTimer);
        nbt.putInt(DROP_INTERVAL_KEY, nextDropInterval);
        nbt.putString(NEKO_MARK_COLOR_KEY, nekoMarkColor.getName());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void onReadCustomData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(DROP_TIMER_KEY)) {
            nekoDropTimer = nbt.getInt(DROP_TIMER_KEY);
        } else {
            nekoDropTimer = 0;
        }

        if (nbt.contains(DROP_INTERVAL_KEY)) {
            nextDropInterval = nbt.getInt(DROP_INTERVAL_KEY);
        } else {
            nextDropInterval = 0;
        }

        if (nbt.contains(NEKO_MARK_COLOR_KEY, NbtElement.STRING_TYPE)) {
            nekoMarkColor = DyeColor.byName(
                    nbt.getString(NEKO_MARK_COLOR_KEY),
                    DyeColor.WHITE
            );
        } else {
            nekoMarkColor = DyeColor.WHITE;
        }
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteractMob(PlayerEntity player, Hand hand,
                               CallbackInfoReturnable<ActionResult> cir) {

        ItemStack stack = player.getStackInHand(hand);

        if (!stack.isOf(ModItems.neko_box)) {
            return;
        }

        cir.setReturnValue(ActionResult.PASS);
        cir.cancel();
    }
}