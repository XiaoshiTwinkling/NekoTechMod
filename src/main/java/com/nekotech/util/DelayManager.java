package com.nekotech.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 延迟任务管理器喵
 * 超级好用的延时工具喵~~~~~~
 */
public class DelayManager {
    private static final Map<String, DelayedTask> delayedTasks = new ConcurrentHashMap<>();
    private static int currentTick = 0;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        // 注册服务器 tick 事件监听器
        ServerTickEvents.END_SERVER_TICK.register(DelayManager::onServerTick);
        initialized = true;
    }

    private static void onServerTick(@NotNull MinecraftServer server) {
        currentTick++;

        Iterator<Map.Entry<String, DelayedTask>> iterator = delayedTasks.entrySet().iterator();
        List<DelayedTask> tasksToExecute = new ArrayList<>();
        List<String> tasksToRemove = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<String, DelayedTask> entry = iterator.next();
            DelayedTask task = entry.getValue();

            if (task.scheduledTick <= currentTick) {
                tasksToExecute.add(task);
                tasksToRemove.add(entry.getKey());
            }
        }

        for (String taskId : tasksToRemove) {
            delayedTasks.remove(taskId);
        }

        for (DelayedTask task : tasksToExecute) {
            try {
                task.function.apply(server);
            } catch (Exception e) {
                System.err.println("执行延迟任务时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 延迟执行函数喵~
     * @param delayTicks 延迟的 tick 数
     * @param function 要执行的函数
     * @return 任务ID，可用于取消任务
     */
    public static String schedule(int delayTicks, @NotNull Function<MinecraftServer, Void> function) {
        if (!initialized) {
            throw new IllegalStateException("DelayManager 未初始化，请在模组初始化时调用 DelayManager.initialize()");
        }

        if (delayTicks < 0) {
            throw new IllegalArgumentException("延迟 tick 数不能为负数");
        }

        String taskId = generateTaskId();
        int scheduledTick = currentTick + delayTicks;

        DelayedTask task = new DelayedTask(taskId, scheduledTick, function);
        delayedTasks.put(taskId, task);

        return taskId;
    }

    /**
     * 延迟执行 Consumer 喵~
     *
     * @param delayTicks 延迟的 tick 数
     * @param consumer   要执行的 consumer
     */
    public static void schedule(int delayTicks, @NotNull Consumer<MinecraftServer> consumer) {
        schedule(delayTicks, server -> {
            consumer.accept(server);
            return null;
        });
    }

    /**
     * 延迟执行 Runnable 喵~
     * @param delayTicks 延迟的 tick 数
     * @param runnable 要执行的 runnable
     * @return 任务ID，可用于取消任务
     */
    public static String schedule(int delayTicks, @NotNull Runnable runnable) {
        return schedule(delayTicks, server -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 取消延迟任务喵~
     * @param taskId 要取消的任务ID
     * @return 是否成功取消
     */
    public static boolean cancel(String taskId) {
        return delayedTasks.remove(taskId) != null;
    }

    /**
     * 检查任务是否存在喵~
     * @param taskId 要检查的任务ID
     * @return 任务是否存在
     */
    public static boolean hasTask(String taskId) {
        return delayedTasks.containsKey(taskId);
    }

    /**
     * 获取当前 tick
     * @return 当前服务器 tick
     */
    public static int getCurrentTick() {
        return currentTick;
    }

    /**
     * 清理所有延迟任务
     */
    public static void clearAll() {
        delayedTasks.clear();
    }

    /**
     * 生成唯一的任务ID
     */
    private static String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 延迟任务
     */
    private static class DelayedTask {
        private final String taskId;
        private final int scheduledTick;
        private final Function<MinecraftServer, Void> function;

        public DelayedTask(String taskId, int scheduledTick, Function<MinecraftServer, Void> function) {
            this.taskId = taskId;
            this.scheduledTick = scheduledTick;
            this.function = function;
        }

        public String getTaskId() { return taskId; }
        public int getScheduledTick() { return scheduledTick; }
        public Function<MinecraftServer, Void> getFunction() { return function; }
    }
}
