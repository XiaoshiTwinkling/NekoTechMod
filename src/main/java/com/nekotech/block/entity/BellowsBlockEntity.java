package com.nekotech.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BellowsBlockEntity extends BlockEntity {

    // ===== 动画（客户端）=====
    public float progress = 0.0f;
    public float lastProgress = 0.0f;
    public boolean compressing = true;

    // ===== 状态（服务器同步）=====
    private boolean working = false;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.bellows, pos, state);
    }

    // ===== Tick =====
    public static void tick(World world, BlockPos pos, BlockState state, BellowsBlockEntity be) {

        // 服务器tick
        if (!world.isClient) {

            if (world.getTime() % 20 == 0) {

                boolean newWorking = hasCatNearby(world, pos);

                if (newWorking != be.working) {
                    be.working = newWorking;
                    be.markDirty();
                    // 同步到客户端
                    world.updateListeners(pos, state, state, 3);
                }
            }
        }

        // 客户端tick
        if (world.isClient) {

            be.lastProgress = be.progress;

            float downSpeedBase = 0.08f;
            float upSpeed = 0.14f;

            if (!be.working) {
                be.progress = Math.max(0.0f, be.progress - upSpeed);
                be.compressing = true;
                return;
            }

            if (be.compressing) {
                float downSpeed = downSpeedBase * (1.0f - be.progress) + 0.01f;
                be.progress += downSpeed;

                if (be.progress >= 0.999f) {
                    be.progress = 1.0f;
                    be.compressing = false;
                }
            } else {
                be.progress -= upSpeed;

                if (be.progress <= 0.001f) {
                    be.progress = 0.0f;
                    be.compressing = true;
                }
            }
        }
    }

    //检测猫
    private static boolean hasCatNearby(World world, BlockPos pos) {
        return !world.getEntitiesByClass(
                net.minecraft.entity.passive.CatEntity.class,
                new net.minecraft.util.math.Box(pos).expand(5),
                cat -> true
        ).isEmpty();
    }

    public float getRenderProgress(float tickDelta) {
        return this.lastProgress + (this.progress - this.lastProgress) * tickDelta;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("working", working);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        working = nbt.getBoolean("working");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    @Nullable
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // ===== 给 BER 用 =====
    public boolean isWorking() {
        return working;
    }
}