package com.nekotech.item.api.googles;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 实现这个接口表示这个方块可以被google看到一个HUD喵~
 */
public interface IHaveGoogleHUD {

    /**
     * 获取这个方块对应的HUD喵~
     */
    @Nullable
    GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state);

    /**
     * 获取HUD的显示优先级
     */
    default int getHudPriority() {
        return 0;
    }

    /**
     * HUD是否应该显示
     */
    default boolean shouldShowHUD(World world, BlockPos pos, BlockState state) {
        return true;
    }
}
