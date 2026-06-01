package com.nekotech.mixin;

import com.nekotech.NekoTechnology;
import com.nekotech.goal.MoveToLaserGoal;
import com.nekotech.goal.nekotask.NekoCatTaskData;
import com.nekotech.goal.nekotask.NekoTagInventoryTaskGoal;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoMark.NekoMarkAccess;
import com.nekotech.mixin.Accessor.MobEntityAccessor;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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
    private static final String NEKO_TASK_DATA_KEY = "neko_task_data";

    private NbtCompound nekoTaskData = new NbtCompound();

    @Override
    public NbtCompound neko_technology$getNekoTaskData() {
        return this.nekoTaskData;
    }

    @Unique
    @Nullable
    private DyeColor nekoMarkColor = null;


    @Unique
    private int getRandomInterval() {
        return 7500 + ((CatEntity)(Object)this).getRandom().nextInt(5001);
    }

    @Override
    public void neko_technology$setNekoMarkColor(@Nullable DyeColor color) {
        this.nekoMarkColor = color;
    }

    @Override
    @Nullable
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

        if (goalSelector.getGoals().stream().noneMatch(
                g -> g.getGoal() instanceof NekoTagInventoryTaskGoal)) {

            goalSelector.add(1, new NekoTagInventoryTaskGoal(cat, 1.0));
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
                ItemStack hairStack = new ItemStack(ModItems.NEKO_HAIR);
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

        if (nekoMarkColor != null) {
            nbt.putString(NEKO_MARK_COLOR_KEY, nekoMarkColor.getName());
        }
        nbt.put(NEKO_TASK_DATA_KEY, this.nekoTaskData.copy());
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
            nekoMarkColor = null;
        }

        if (nbt.contains(NEKO_TASK_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            this.nekoTaskData = nbt.getCompound(NEKO_TASK_DATA_KEY).copy();
        } else {
            this.nekoTaskData = new NbtCompound();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void dropCarriedItemsOnDeath(DamageSource damageSource, CallbackInfo ci) {
        CatEntity cat = (CatEntity)(Object)this;

        if (cat.getWorld().isClient()) {
            return;
        }

        List<ItemStack> carriedStacks = NekoCatTaskData.readCarriedStacks(
                this.nekoTaskData,
                cat.getWorld().getRegistryManager()
        );

        if (carriedStacks.isEmpty()) {
            return;
        }

        for (ItemStack stack : carriedStacks) {
            if (!stack.isEmpty()) {
                dropStackWithBurst(cat, stack);
            }
        }

        NekoCatTaskData.writeCarriedStacks(
                this.nekoTaskData,
                cat.getWorld().getRegistryManager(),
                List.of()
        );
    }

    @Unique
    private void dropStackWithBurst(CatEntity cat, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
                cat.getWorld(),
                cat.getX(),
                cat.getY() + 0.25D,
                cat.getZ(),
                stack.copy()
        );

        double velocityX = (cat.getRandom().nextDouble() - 0.5D) * 0.4D;
        double velocityY = 0.2D + cat.getRandom().nextDouble() * 0.2D;
        double velocityZ = (cat.getRandom().nextDouble() - 0.5D) * 0.4D;

        itemEntity.setVelocity(velocityX, velocityY, velocityZ);
        cat.getWorld().spawnEntity(itemEntity);
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteractMob(PlayerEntity player, Hand hand,
                               CallbackInfoReturnable<ActionResult> cir) {

        ItemStack stack = player.getStackInHand(hand);

        if (!stack.isOf(ModItems.NEKO_BOX)) {
            return;
        }

        cir.setReturnValue(ActionResult.PASS);
        cir.cancel();
    }
}
