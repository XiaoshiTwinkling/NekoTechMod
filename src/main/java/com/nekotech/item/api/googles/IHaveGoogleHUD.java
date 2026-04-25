package com.nekotech.item.api.googles;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 实现这个接口表示这个方块可以被google看到一个HUD喵~
 */
public interface IHaveGoogleHUD {

    /**
     * 获取这个方块对应的HUD喵~
     */
    @Nullable
    default GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state){
        return null;
    }

    /**
     * 获取这个方块对应的一堆HUD喵~
     */
    default List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        GoogleAbstractHUD singleHud = getGoogleHUD(world, pos, state);
        if (singleHud != null) {
            // 把单个HUD包装成一个只包含一个元素的列表喵~
            return Collections.singletonList(singleHud);
        }
        // 如果没有HUD，就返回一个空列表喵~
        return Collections.emptyList();
    }

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
