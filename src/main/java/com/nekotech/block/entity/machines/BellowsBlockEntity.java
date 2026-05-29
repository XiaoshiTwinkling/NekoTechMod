package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
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

public class BellowsBlockEntity extends BlockEntity implements ICatNeedMachine {

    public float progress = 0.0f;
    public float lastProgress = 0.0f;
    public boolean compressing = true;

    private boolean working = false;

    private BlockPos boundControllerPos = null;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELLOWS, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BellowsBlockEntity be) {
        // 服务器tick
        if (!world.isClient) {
            if (world.getTime() % 20 == 0) {
                boolean newWorking = be.canMachineRun();

                if (newWorking != be.working) {
                    be.working = newWorking;
                    be.markDirty();
                    // 同步到客户端
                    world.updateListeners(pos, state, state, 3);
                }
            }

            if (world.getTime() % 200 == 0) {
                be.cleanupInvalidBindings();
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

    public float getRenderProgress(float tickDelta) {
        return this.lastProgress + (this.progress - this.lastProgress) * tickDelta;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("working", working);

        if (boundControllerPos != null) {
            nbt.putInt("ControllerX", boundControllerPos.getX());
            nbt.putInt("ControllerY", boundControllerPos.getY());
            nbt.putInt("ControllerZ", boundControllerPos.getZ());
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        working = nbt.getBoolean("working");

        if (nbt.contains("ControllerX")) {
            boundControllerPos = new BlockPos(
                    nbt.getInt("ControllerX"),
                    nbt.getInt("ControllerY"),
                    nbt.getInt("ControllerZ")
            );
        } else {
            boundControllerPos = null;
        }
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

    @Override
    public @Nullable BlockPos getBoundControllerPos() {
        return boundControllerPos;
    }

    @Override
    public void setBoundControllerPos(@Nullable BlockPos pos) {
        this.boundControllerPos = pos;
        this.markDirty();
    }

    // 可选：保留一个便捷方法用于调试
    public boolean hasBoundController() {
        return boundControllerPos != null;
    }
}