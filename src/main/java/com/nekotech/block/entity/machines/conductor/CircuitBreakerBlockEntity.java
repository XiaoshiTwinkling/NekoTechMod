package com.nekotech.block.entity.machines.conductor;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.MachineBlockEntity;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.block.CircuitBreakerBlock;
import com.nekotech.util.DelayManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 断路器 顾名思义喵
 */
public class CircuitBreakerBlockEntity extends MachineBlockEntity implements IHaveGoogleHUD, ITransferElectrical {

    private boolean isPoweredByRedstone = false;
    private boolean lastRedstoneState = false;
    private boolean manuallyOpen = false;

    public CircuitBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CIRCUIT_BREAKER, pos, state);
    }


    /**
     * 服务器端tick方法，检查红石信号变化喵~
     */
    public static void tick(World world, BlockPos pos, BlockState state, CircuitBreakerBlockEntity blockEntity) {
        if (!world.isClient()) {
            blockEntity.serverTick(world, pos, state);
        }
    }

    /**
     * 服务器端处理逻辑喵~
     */
    private void serverTick(World world, BlockPos pos, BlockState state) {
        // 检测红石信号
        boolean currentRedstoneState = world.isReceivingRedstonePower(pos);

        // 如果红石信号状态发生变化
        if (currentRedstoneState != lastRedstoneState) {
            lastRedstoneState = currentRedstoneState;
            isPoweredByRedstone = currentRedstoneState;

            this.markDirty();

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CircuitBreakerBlockEntity breaker) {
                BlockState newState = world.getBlockState(pos)
                        .with(CircuitBreakerBlock.POWERED, currentRedstoneState)
                        .with(CircuitBreakerBlock.OPEN, breaker.isManuallyOpen());

                world.setBlockState(pos, newState, Block.NOTIFY_ALL);

                world.updateListeners(pos, newState, newState, 3);

                DelayManager.schedule(1, server -> {
                    breaker.updateConductorSystem();
                });
            }
        }

        lastSwitchAnimationProgress = switchAnimationProgress;
        updateSwitchAnimation();
    }

    /**
     * 设置红石电源状态并同步到方块喵~
     * @param powered 是否被红石激活
     */
    public void setRedstonePower(boolean powered) {
        this.isPoweredByRedstone = powered;
        this.lastRedstoneState = powered;

        this.markDirty();

        if (world != null && !world.isClient()) {
            BlockState newState = getCachedState()
                    .with(CircuitBreakerBlock.POWERED, powered)
                    .with(CircuitBreakerBlock.OPEN, manuallyOpen);

            world.setBlockState(pos, newState, Block.NOTIFY_ALL);

            // 同步到客户端
            world.updateListeners(pos, newState, newState, 3);
        }
    }

    /**
     * 更新断路器的状态喵~
     * 当有红石信号时，断路器断开（不能导电）
     * 当没有红石信号时，断路器闭合（可以导电）
     *
     * @param hasRedstoneSignal 是否有红石信号
     */
    private void updateBreakerState(boolean hasRedstoneSignal) {
        // 如果有红石信号，断路器断开
        // 如果没有红石信号，断路器闭合
        isPoweredByRedstone = hasRedstoneSignal;
    }

    /**
     * 手动切换断路器的开关状态喵~
     * 这会覆盖红石信号的控制
     *
     * @return 切换后的开关状态
     */
    public boolean toggleManualSwitch() {
        manuallyOpen = !manuallyOpen;

        // 标记脏数据
        this.markDirty();

        // 通知客户端更新
        if (world != null && !world.isClient()) {
            // 更新方块状态
            updateBlockState(world, pos, getCachedState());
        }
        // 通知导体管理器状态变化喵~
        updateConductorSystem();

        return manuallyOpen;
    }

    /**
     * 通知导体系统这个方块的状态变化了喵~
     * 这会让导体系统重新扫描网络
     */
    public void updateConductorSystem() {

        if (world != null && !world.isClient() && world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            DelayManager.schedule(1, server -> {});
            ConductorSystem.onBlockEntityStateChange(serverWorld, pos);
        }
    }

    /**
     * 更新方块状态，同步到客户端喵~
     */
    private void updateBlockState(World world, BlockPos pos, BlockState state) {
        if (!world.isClient()) {
            // 调用方块更新，让客户端知道状态变化
            world.updateListeners(pos, state, state, 3);
            // 标记方块需要重新渲染
            world.markDirty(pos);
        }
    }

    /**
     * 实现ITransferElectrical接口的canTransfer方法喵~
     *
     * @return 如果可以导电返回true，否则返回false喵~
     */
    @Override
    public boolean canTransfer() {
        // 如果被手动断开，或者被红石激活，都不能导电
        return !(manuallyOpen || isPoweredByRedstone);
    }

    /**
     * 获取是否被红石激活喵~
     *
     * @return 如果被红石激活返回true
     */
    public boolean isPoweredByRedstone() {
        return isPoweredByRedstone;
    }

    /**
     * 获取是否被手动断开喵~
     *
     * @return 如果被手动断开返回true
     */
    public boolean isManuallyOpen() {
        return manuallyOpen;
    }

    /**
     * 实现IHaveGoogleHUD接口，为护目镜提供HUD信息喵~
     */
    @Nullable
    @Override
    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;  // 只在服务端返回数据喵~
        }

        boolean canTransfer = canTransfer();

        Text title = Text.translatable("block.neko-technology.circuit_breaker").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.circuit_breaker.description",
                Text.translatable("block.neko-technology." + (canTransfer ? "yes" : "no")).formatted(
                        canTransfer ? Formatting.GREEN : Formatting.RED
                ),
                Text.translatable("block.neko-technology.circuit_breaker.status." +
                        (manuallyOpen ? "manual_open" :
                                isPoweredByRedstone ? "redstone_open" : "closed")).formatted(
                        canTransfer ? Formatting.GREEN : Formatting.RED
                ),
                Text.translatable("block.neko-technology.circuit_breaker.redstone." +
                        (isPoweredByRedstone ? "powered" : "unpowered")).formatted(
                        isPoweredByRedstone ? Formatting.RED : Formatting.GREEN
                )
        );

        return new InfoBoxHUDData(pos, title, content);
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {}

    /**
     * 保存NBT数据喵~
     */
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putBoolean("IsPoweredByRedstone", isPoweredByRedstone);
        nbt.putBoolean("LastRedstoneState", lastRedstoneState);
        nbt.putBoolean("ManuallyOpen", manuallyOpen);
    }

    /**
     * 读取NBT数据喵~
     */
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        isPoweredByRedstone = nbt.getBoolean("IsPoweredByRedstone");
        lastRedstoneState = nbt.getBoolean("LastRedstoneState");
        manuallyOpen = nbt.getBoolean("ManuallyOpen");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registries);
        return nbt;
    }

    /**
     * 下面是动画
     */

    // 添加以下字段
    private float switchAnimationProgress = 0.0f;  // 0.0-1.0
    private float lastSwitchAnimationProgress = 0.0f;
    private boolean isSwitchAnimating = false;
    private int animationTicks = 0;
    private static final int ANIMATION_DURATION = 10;  // 动画持续10tick

    // 在tick方法中添加动画更新
    private void updateSwitchAnimation() {
        // 获取目标状态
        float targetProgress = manuallyOpen ? 1.0f : 0.0f;

        // 如果当前进度不等于目标进度，开始动画
        if (Math.abs(switchAnimationProgress - targetProgress) > 0.001f) {
            isSwitchAnimating = true;
            animationTicks++;

            // 计算动画进度（线性插值）
            float animationDelta = (float)animationTicks / ANIMATION_DURATION;

            if (manuallyOpen) {
                // 向打开方向动画
                switchAnimationProgress = Math.min(targetProgress, animationDelta);
            } else {
                // 向关闭方向动画
                switchAnimationProgress = Math.max(targetProgress, 1.0f - animationDelta);
            }

            // 标记需要保存
            this.markDirty();

            // 动画完成
            if (animationTicks >= ANIMATION_DURATION) {
                switchAnimationProgress = targetProgress;
                isSwitchAnimating = false;
                animationTicks = 0;
            }
        } else {
            isSwitchAnimating = false;
            animationTicks = 0;
        }
    }

    // 添加获取动画进度的方法（客户端使用）
    public float getSwitchAnimationProgress(float tickDelta) {
        if (!isSwitchAnimating) {
            return switchAnimationProgress;
        }

        // 线性插值，使动画更平滑
        float progressDelta = switchAnimationProgress - lastSwitchAnimationProgress;
        return lastSwitchAnimationProgress + progressDelta * tickDelta;
    }

}
