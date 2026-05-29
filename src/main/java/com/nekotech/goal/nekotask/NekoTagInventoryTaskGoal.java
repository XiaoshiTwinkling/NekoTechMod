package com.nekotech.goal.nekotask;

import com.nekotech.data.worlddata.NekoTagWorldState;
import com.nekotech.block.elevator.ElevatorPartBlock;
import com.nekotech.item.custom.NekoMark.NekoMarkAccess;
import com.nekotech.item.custom.NekoTag.NekoPlacedTag;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NekoTagInventoryTaskGoal extends Goal {

    private static final int SEARCH_RADIUS = 20;
    private static final double ARRIVE_DISTANCE_SQ = 2.0D * 2.0D;
    private static final int MAX_TRAVEL_TICKS = 20 * 15;

    private static final String TASK_INPUT = "input";
    private static final String TASK_OUTPUT = "output";

    private final CatEntity cat;
    private final double speed;

    private NekoTagWorldState.TaskCandidate target;
    private int travelTicks;
    private boolean finished;

    public NekoTagInventoryTaskGoal(CatEntity cat, double speed) {
        this.cat = cat;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (!(this.cat.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        if (!(this.cat instanceof NekoMarkAccess access)) {
            return false;
        }

        DyeColor color = access.neko_technology$getNekoMarkColor();

        if (color == null) {
            return false;
        }

        NbtCompound taskData = access.neko_technology$getNekoTaskData();

        this.target = selectTarget(
                world,
                taskData,
                color.getName()
        );

        return this.target != null;
    }

    @Override
    public void start() {
        this.finished = false;
        this.travelTicks = 0;

        if (!(cat.getWorld() instanceof ServerWorld world)) {
            this.finished = true;
            return;
        }

        if (this.target == null) {
            this.finished = true;
            return;
        }

        BlockPos pos = this.target.pos();

        boolean pathStarted = this.cat.getNavigation().startMovingTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                this.speed
        );

        if (!pathStarted) {
            // 寻路失败也记为任务成功。
            completeCurrentTask(world);
        }
    }

    @Override
    public boolean shouldContinue() {
        return !this.finished && this.target != null;
    }

    @Override
    public void tick() {
        if (!(cat.getWorld() instanceof ServerWorld world)) {
            this.finished = true;
            return;
        }

        if (this.target == null) {
            this.finished = true;
            return;
        }

        this.travelTicks++;

        if (isNearTarget()) {
            executeTarget(world);
            return;
        }

        if (this.cat.getNavigation().isIdle()) {
            // 寻路提前结束但没有到达，按寻路失败处理。
            completeCurrentTask(world);
            return;
        }

        if (this.travelTicks > MAX_TRAVEL_TICKS) {
            // 防止极端情况下长时间卡住。
            completeCurrentTask(world);
        }
    }

    @Override
    public void stop() {
        this.cat.getNavigation().stop();
        this.target = null;
        this.travelTicks = 0;
        this.finished = false;
    }

    private NekoTagWorldState.TaskCandidate selectTarget(
            ServerWorld world,
            NbtCompound taskData,
            String color
    ) {
        NekoTagWorldState state = NekoTagWorldState.get(world.getServer());

        List<NekoTagWorldState.TaskCandidate> candidates = state.findTasksNear(
                world,
                this.cat.getBlockPos(),
                color,
                SEARCH_RADIUS
        );

        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<NekoTaskMemoryKey> memory = NekoCatTaskData.readMemory(taskData);

        /*
         * 第一阶段：
         * 在所有“未记忆”的任务中，选择 priority 最高的一组。
         */
        short highestUnrememberedPriority = Short.MIN_VALUE;
        List<NekoTagWorldState.TaskCandidate> bestUnremembered = new ArrayList<>();

        for (NekoTagWorldState.TaskCandidate candidate : candidates) {
            NekoTaskMemoryKey key = NekoTaskMemoryKey.of(candidate);

            if (memory.contains(key)) {
                continue;
            }

            short priority = candidate.tag().priority();

            if (bestUnremembered.isEmpty() || priority > highestUnrememberedPriority) {
                highestUnrememberedPriority = priority;
                bestUnremembered.clear();
                bestUnremembered.add(candidate);
                continue;
            }

            if (priority == highestUnrememberedPriority) {
                bestUnremembered.add(candidate);
            }
        }

        if (!bestUnremembered.isEmpty()) {
            return bestUnremembered.get(this.cat.getRandom().nextInt(bestUnremembered.size()));
        }

        /*
         * 第二阶段：
         * 所有候选任务都已经在 memory 里。
         */
        List<NekoTagWorldState.TaskCandidate> topPriorityTasks = getHighestPriorityTasks(candidates);

        /*
         * memory 顺序按时间排序提取
         */
        for (NekoTaskMemoryKey rememberedKey : memory) {
            for (NekoTagWorldState.TaskCandidate candidate : topPriorityTasks) {
                if (rememberedKey.equals(NekoTaskMemoryKey.of(candidate))) {
                    return candidate;
                }
            }
        }

        return topPriorityTasks.get(this.cat.getRandom().nextInt(topPriorityTasks.size()));
    }

    private static @NotNull List<NekoTagWorldState.TaskCandidate> getHighestPriorityTasks(
            List<NekoTagWorldState.TaskCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        short highestPriority = candidates.getFirst().tag().priority();

        for (NekoTagWorldState.TaskCandidate candidate : candidates) {
            if (candidate.tag().priority() > highestPriority) {
                highestPriority = candidate.tag().priority();
            }
        }

        List<NekoTagWorldState.TaskCandidate> result = new ArrayList<>();

        for (NekoTagWorldState.TaskCandidate candidate : candidates) {
            if (candidate.tag().priority() == highestPriority) {
                result.add(candidate);
            }
        }

        return result;
    }

    private boolean isNearTarget() {
        if (this.target == null) {
            return false;
        }

        Vec3d center = Vec3d.ofCenter(this.target.pos());
        return this.cat.squaredDistanceTo(center) <= ARRIVE_DISTANCE_SQ;
    }

    private void executeTarget(ServerWorld world) {
        if (this.target == null) {
            this.finished = true;
            return;
        }

        BlockPos pos = this.target.pos();
        BlockState state = world.getBlockState(pos);

        Inventory inventory;

        if (state.getBlock() instanceof ElevatorPartBlock) {
            inventory = NekoInventoryOps.getElevatorInventory(world, pos);
        } else {
            inventory = NekoInventoryOps.getInventoryAt(world, pos);
        }

        NekoPlacedTag tag = this.target.tag();
        String task = tag.task();

        if (TASK_INPUT.equals(task)) {
            executeInput(world, inventory, tag);
            completeCurrentTask(world);
            return;
        }

        if (TASK_OUTPUT.equals(task)) {
            executeOutput(world, inventory, tag);
            completeCurrentTask(world);
            return;
        }

        // 未知任务类型也不阻塞。
        completeCurrentTask(world);
    }

    /**
     * input 是对 inventory 的 input：
     * 猫把自己体内 display_stack_id 对应物品输入 inventory。
     * 如果放不进去，就在猫当前位置掉落该物品。
     */
    private void executeInput(
            ServerWorld world,
            Inventory inventory,
            NekoPlacedTag tag
    ) {
        if (!(this.cat instanceof NekoMarkAccess access)) {
            return;
        }

        Item item = resolveDisplayItem(tag);

        if (item == null) {
            return;
        }

        NbtCompound taskData = access.neko_technology$getNekoTaskData();

        ItemStack carried = NekoCatTaskData.removeOneCarriedItem(
                taskData,
                world.getRegistryManager(),
                item
        );

        if (carried.isEmpty()) {
            return;
        }

        ItemStack remaining = NekoInventoryOps.insertStack(inventory, carried);

        if (!remaining.isEmpty()) {
            dropAtCat(world, remaining);
        }
    }

    /**
     * output 是对 inventory 的 output：
     * 猫从 inventory 取出 display_stack_id 对应物品，每次一个。
     * 如果 inventory 没有对应物品，不做更改，仍然算任务成功。
     */
    private void executeOutput(
            ServerWorld world,
            Inventory inventory,
            NekoPlacedTag tag
    ) {
        if (!(this.cat instanceof NekoMarkAccess access)) {
            return;
        }

        Item item = resolveDisplayItem(tag);

        if (item == null) {
            return;
        }

        ItemStack taken = NekoInventoryOps.removeOneMatching(inventory, item);

        if (taken.isEmpty()) {
            return;
        }

        NbtCompound taskData = access.neko_technology$getNekoTaskData();

        NekoCatTaskData.addCarriedStack(
                taskData,
                world.getRegistryManager(),
                taken
        );
    }

    private Item resolveDisplayItem(NekoPlacedTag tag) {
        Identifier identifier = tag.displayIdentifierOrNull();

        if (identifier == null) {
            return null;
        }

        Item item = Registries.ITEM.get(identifier);

        if (item == Items.AIR) {
            return null;
        }

        return item;
    }

    private void dropAtCat(ServerWorld world, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(
                world,
                this.cat.getX(),
                this.cat.getY(),
                this.cat.getZ(),
                stack.copy()
        );

        world.spawnEntity(itemEntity);
    }

    private void completeCurrentTask(ServerWorld world) {
        if (this.target != null && this.cat instanceof NekoMarkAccess access) {
            NbtCompound taskData = access.neko_technology$getNekoTaskData();

            NekoTaskMemoryKey key = NekoTaskMemoryKey.of(this.target);
            NekoCatTaskData.remember(taskData, key);
        }

        this.cat.getNavigation().stop();
        this.finished = true;
    }
}
