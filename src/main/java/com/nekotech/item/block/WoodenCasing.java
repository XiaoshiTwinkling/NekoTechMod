package com.nekotech.item.block;

import com.mojang.serialization.MapCodec;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.electrical.ConductorManager;
import com.nekotech.block.entity.machines.HeaterBlockEntity;
import com.nekotech.block.entity.machines.MachineCasingBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WoodenCasing extends DirectionalMachineBlock{
    public static final MapCodec<WoodenCasing> CODEC = createCodec(WoodenCasing::new);

    public WoodenCasing(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCasingBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // 如果方块被替换（包括破坏），而不是被活塞推动
        if (!state.isOf(newState.getBlock())) {
            // 只在服务端处理
            if (!world.isClient()) {
                // 获取导体管理器并通知此位置需要更新
                ConductorManager manager = ConductorManager.get(world);
                if (manager != null) {
                    // 通知管理器此位置的方块发生了变化
                    manager.invalidateAt(pos);
                }
            }
        }

        // 调用父类方法
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
