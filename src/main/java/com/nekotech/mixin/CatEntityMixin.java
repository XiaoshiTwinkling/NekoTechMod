package com.nekotech.mixin;

import com.nekotech.NekoTechnology;
import com.nekotech.data.worlddata.NekoTagWorldState;
import com.nekotech.catcamera.CatCameraChannelAccess;
import com.nekotech.catcamera.CatCameraChannelData;
import com.nekotech.catcamera.CatCameraChannelService;
import com.nekotech.goal.MoveToLaserGoal;
import com.nekotech.goal.nekotask.NekoCatTaskData;
import com.nekotech.goal.nekotask.NekoTagInventoryTaskGoal;
import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoMark.NekoMarkAccess;
import com.nekotech.item.custom.NekoMark.NekoMarkItem;
import com.nekotech.item.custom.CatCameraTerminalItem;
import com.nekotech.mixin.Accessor.CatEntityAccessor;
import com.nekotech.mixin.Accessor.MobEntityAccessor;
import com.nekotech.block.entity.machines.TreadmillCat;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CatEntity.class)
public class CatEntityMixin implements NekoMarkAccess, TreadmillCat, CatCameraChannelAccess {
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

    @Unique
    private static final int NO_NEKO_MARK_COLOR = -1;

    @Unique
    private static final TrackedData<Integer> NEKO_MARK_COLOR =
            DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.INTEGER);

    @Unique
    private static final TrackedData<Boolean> CAT_CAMERA_CHANNEL_ACTIVE =
            DataTracker.registerData(CatEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    @Nullable
    private CatCameraChannelData catCameraChannelData;

    @Unique
    private boolean catCameraChannelReconciled;

    @Unique
    private NbtCompound nekoTaskData = new NbtCompound();

    @Unique
    @Nullable
    private NekoTagWorldState.TaskCandidate currentNekoTask = null;

    @Override
    public NbtCompound neko_technology$getNekoTaskData() {
        return this.nekoTaskData;
    }

    @Override
    @Nullable
    public CatCameraChannelData neko_technology$getCatCameraChannel() {
        return catCameraChannelData;
    }

    @Override
    public void neko_technology$setCatCameraChannel(@Nullable CatCameraChannelData data) {
        this.catCameraChannelData = data;
        ((CatEntity)(Object)this).getDataTracker().set(
                CAT_CAMERA_CHANNEL_ACTIVE,
                data != null && data.active()
        );
    }

    @Override
    public boolean neko_technology$isCatCameraChannelActive() {
        return ((CatEntity)(Object)this).getDataTracker().get(CAT_CAMERA_CHANNEL_ACTIVE);
    }

    @Override
    public boolean neko_technology$isCatCameraChannelReconciled() {
        return catCameraChannelReconciled;
    }

    @Override
    public void neko_technology$setCatCameraChannelReconciled(boolean reconciled) {
        this.catCameraChannelReconciled = reconciled;
    }

    @Override
    @Nullable
    public NekoTagWorldState.TaskCandidate neko_technology$getCurrentNekoTask() {
        return this.currentNekoTask;
    }

    @Override
    public void neko_technology$setCurrentNekoTask(
            @Nullable NekoTagWorldState.TaskCandidate currentTask
    ) {
        this.currentNekoTask = currentTask;
    }

    @Unique
    @Nullable
    private DyeColor nekoMarkColor = null;

    @Unique
    private boolean neko_runningOnTreadmill = false;

    @Unique
    private Vec3d neko_treadmillCenter = Vec3d.ZERO.add(0,-0.1,0);

    @Unique
    private float neko_treadmillFacingYaw = 0f;

    @Unique private int neko_treadmillAlignCd = 0;

    @Unique
    private int getRandomInterval() {
        return 7500 + ((CatEntity)(Object)this).getRandom().nextInt(5001);
    }

    @Override
    public void neko_technology$setNekoMarkColor(@Nullable DyeColor color) {
        this.nekoMarkColor = color;
        ((CatEntity)(Object)this).getDataTracker().set(
                NEKO_MARK_COLOR,
                color == null ? NO_NEKO_MARK_COLOR : color.getId()
        );
    }

    @Override
    @Nullable
    public DyeColor neko_technology$getNekoMarkColor() {
        int trackedColor = ((CatEntity)(Object)this).getDataTracker().get(NEKO_MARK_COLOR);

        if (trackedColor >= 0) {
            DyeColor color = DyeColor.byId(trackedColor);
            this.nekoMarkColor = color;
            return color;
        }

        if (((CatEntity)(Object)this).getWorld().isClient()) {
            this.nekoMarkColor = null;
        }

        return this.nekoMarkColor;
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initNekoMarkDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(NEKO_MARK_COLOR, NO_NEKO_MARK_COLOR);
        builder.add(CAT_CAMERA_CHANNEL_ACTIVE, false);
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

            if (!catCameraChannelReconciled) {
                CatCameraChannelService.reconcile(cat);
            } else if (cat.age % 20 == 0) {
                CatCameraChannelService.updateLocation(cat);
            }

            if (nextDropInterval == 0) {
                nextDropInterval = getRandomInterval();
            }

            nekoDropTimer++;

            if (nekoDropTimer >= nextDropInterval) {
                ItemStack hairStack = new ItemStack(ModItems.NEKO_HAIR);
                cat.dropStack(hairStack);

                nekoDropTimer = 0;
                nextDropInterval = getRandomInterval();
            }
        }

        if (neko_runningOnTreadmill) {
            boolean client = cat.getWorld().isClient;

            //允许微小移动 这样更好看喵
            final double SPEED = 0.065;
            final double CORRECT_DIST = 0.05;

            float wantYaw = neko_treadmillFacingYaw;

            if (!client) {
                neko_treadmillAlignCd--;
                if (neko_treadmillAlignCd <= 0) {
                    neko_treadmillAlignCd = 5;
                    double dx = neko_treadmillCenter.x - cat.getX();
                    double dz = neko_treadmillCenter.z - cat.getZ();
                    if (dx*dx + dz*dz > CORRECT_DIST * CORRECT_DIST) {
                        cat.refreshPositionAndAngles(
                                neko_treadmillCenter.x, neko_treadmillCenter.y, neko_treadmillCenter.z,
                                wantYaw, cat.getPitch()
                        );
                    }
                }

                cat.setYaw(wantYaw);
                cat.bodyYaw = wantYaw;
                cat.headYaw = wantYaw;

                double rad = Math.toRadians(wantYaw);
                double vx = -Math.sin(rad) * SPEED;
                double vz =  Math.cos(rad) * SPEED;
                cat.setVelocity(vx, 0.0, vz);

                cat.setOnGround(true);
                cat.fallDistance = 0f;
                cat.getNavigation().stop();
                cat.setSprinting(true);
            } else {
                float cur = cat.getYaw();
                float lerped = cur + MathHelper.wrapDegrees(wantYaw - cur) * 0.3f;
                cat.setYaw(lerped);
                cat.bodyYaw = lerped;
                cat.headYaw = lerped;
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
        if (catCameraChannelData != null) {
            nbt.put(CatCameraChannelData.NBT_KEY, catCameraChannelData.toNbt());
        }
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

        ((CatEntity)(Object)this).getDataTracker().set(
                NEKO_MARK_COLOR,
                nekoMarkColor == null ? NO_NEKO_MARK_COLOR : nekoMarkColor.getId()
        );

        if (nbt.contains(NEKO_TASK_DATA_KEY, NbtElement.COMPOUND_TYPE)) {
            this.nekoTaskData = nbt.getCompound(NEKO_TASK_DATA_KEY).copy();
        } else {
            this.nekoTaskData = new NbtCompound();
        }

        if (nbt.contains(CatCameraChannelData.NBT_KEY, NbtElement.COMPOUND_TYPE)) {
            this.catCameraChannelData = CatCameraChannelData.fromNbt(nbt.getCompound(CatCameraChannelData.NBT_KEY));
        } else {
            this.catCameraChannelData = null;
        }
        this.catCameraChannelReconciled = false;
        ((CatEntity)(Object)this).getDataTracker().set(
                CAT_CAMERA_CHANNEL_ACTIVE,
                catCameraChannelData != null && catCameraChannelData.active()
        );
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"))
    private void dropCarriedItemsOnDeath(CallbackInfoReturnable<SoundEvent> cir) {
        CatEntity cat = (CatEntity)(Object)this;

        if (cat.getWorld().isClient()) {
            return;
        }

        if (neko_technology$isCatCameraChannelActive()) {
            CatCameraChannelService.delete(cat);
        }

        dropCurrentNekoMark(cat);

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
        CatEntity cat = (CatEntity)(Object)this;

        ItemStack stack = player.getStackInHand(hand);

        if (stack.getItem() instanceof CatCameraTerminalItem terminal) {
            cir.setReturnValue(terminal.useOnEntity(stack, player, cat, hand));
            cir.cancel();
            return;
        }

        if (stack.getItem() instanceof NekoMarkItem nekoMarkItem) {
            cir.setReturnValue(handleNekoMarkInteraction(cat, player, stack, nekoMarkItem));
            cir.cancel();
            return;
        }

        if (stack.getItem() instanceof DyeItem dyeItem && cat.isTamed() && cat.isOwner(player)) {
            DyeColor dyeColor = dyeItem.getColor();
            DyeColor markColor = neko_technology$getNekoMarkColor();

            if (cat.getCollarColor() != dyeColor || markColor != null && markColor != dyeColor) {
                if (!cat.getWorld().isClient()) {
                    applyDyeColor(cat, dyeColor);
                    stack.decrementUnlessCreative(1, player);
                    cat.setPersistent();
                }

                cir.setReturnValue(ActionResult.success(cat.getWorld().isClient()));
                cir.cancel();
                return;
            }
        }

        if (!stack.isOf(ModItems.NEKO_BOX)) {
            return;
        }

        cir.setReturnValue(ActionResult.PASS);
        cir.cancel();
    }

    @Unique
    private ActionResult handleNekoMarkInteraction(
            CatEntity cat,
            PlayerEntity player,
            ItemStack stack,
            NekoMarkItem nekoMarkItem
    ) {
        if (!cat.isTamed() || !cat.isOwner(player)) {
            return ActionResult.FAIL;
        }

        if (!cat.getWorld().isClient()) {
            applyNekoMarkColor(cat, nekoMarkItem.getColor());
            stack.decrementUnlessCreative(1, player);
            cat.setPersistent();
        }

        return ActionResult.success(cat.getWorld().isClient());
    }

    @Unique
    private void applyNekoMarkColor(CatEntity cat, DyeColor color) {
        DyeColor oldColor = neko_technology$getNekoMarkColor();

        if (oldColor != null) {
            dropStackWithBurst(cat, new ItemStack(getNekoMarkItem(oldColor)));
        }

        neko_technology$setNekoMarkColor(color);
        ((CatEntityAccessor)cat).neko_technology$setCollarColor(color);
    }

    @Unique
    private void applyDyeColor(CatEntity cat, DyeColor color) {
        if (neko_technology$getNekoMarkColor() != null) {
            neko_technology$setNekoMarkColor(color);
        }

        ((CatEntityAccessor)cat).neko_technology$setCollarColor(color);
    }

    @Unique
    private void dropCurrentNekoMark(CatEntity cat) {
        DyeColor color = neko_technology$getNekoMarkColor();

        if (color == null) {
            return;
        }

        dropStackWithBurst(cat, new ItemStack(getNekoMarkItem(color)));
        neko_technology$setNekoMarkColor(null);
    }

    @Unique
    private static Item getNekoMarkItem(DyeColor color) {
        return switch (color) {
            case WHITE -> ModItems.WHITE_NEKO_MARK;
            case ORANGE -> ModItems.ORANGE_NEKO_MARK;
            case MAGENTA -> ModItems.MAGENTA_NEKO_MARK;
            case LIGHT_BLUE -> ModItems.LIGHT_BLUE_NEKO_MARK;
            case YELLOW -> ModItems.YELLOW_NEKO_MARK;
            case LIME -> ModItems.LIME_NEKO_MARK;
            case PINK -> ModItems.PINK_NEKO_MARK;
            case GRAY -> ModItems.GRAY_NEKO_MARK;
            case LIGHT_GRAY -> ModItems.LIGHT_GRAY_NEKO_MARK;
            case CYAN -> ModItems.CYAN_NEKO_MARK;
            case PURPLE -> ModItems.PURPLE_NEKO_MARK;
            case BLUE -> ModItems.BLUE_NEKO_MARK;
            case BROWN -> ModItems.BROWN_NEKO_MARK;
            case GREEN -> ModItems.GREEN_NEKO_MARK;
            case RED -> ModItems.RED_NEKO_MARK;
            case BLACK -> ModItems.BLACK_NEKO_MARK;
        };
    }

    /**
     * 让猫在猫跑机上跑步
     * @param center 猫跑机中心坐标
     * @param facingYaw 猫应该面向的 yaw 角度
     */
    @Override
    public void neko_technology$startRunningOnTreadmill(Vec3d center, float facingYaw) {
        this.neko_runningOnTreadmill = true;
        this.neko_treadmillCenter = center;
        this.neko_treadmillFacingYaw = facingYaw;
    }

    /** 停止跑步 */
    @Override
    public void neko_technology$stopRunningOnTreadmill() {
        this.neko_runningOnTreadmill = false;
    }

}
