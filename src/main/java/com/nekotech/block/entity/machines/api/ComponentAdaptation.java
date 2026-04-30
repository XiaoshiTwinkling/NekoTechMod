package com.nekotech.block.entity.machines.api;

import com.nekotech.item.custom.component.AbstractComponentItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/*
 * 实现这个接口表示该方块实体可以在其六面放置零件
 * 零件是一个可以摆放在某些方块六面的小道具 （比如黄铜电流输出接口 可以用来输出猫猫能量（NekoFlux））
 * 这些小道具不是方块 必须附着在实现接口方块的六面 并且在方块被破坏后掉落 且都有独特的模型
 * 这些零件附着在六面时候 不会影响相邻方块的放置
 */
public interface ComponentAdaptation {
    /* ================= 数据 ================= */

    /**
     * 六个面安装的零件
     * Key: 方块朝向
     * Value: 零件 Item
     */
    Map<Direction, Item> getAttachedComponents();

    /**
     * 该机器允许安装的零件类型
     */
    Set<Item> getValidComponents();

    default boolean canAttachComponent(Direction side, Item component) {
        return getValidComponents().contains(component);
    }

    default boolean attachComponent(Direction side, Item component) {
        if (!canAttachComponent(side, component)) return false;
        getAttachedComponents().put(side, component);
        return true;
    }

    default void removeComponent(Direction side) {
        getAttachedComponents().remove(side);
    }

    @Nullable
    default Item getComponent(Direction side) {
        return getAttachedComponents().get(side);
    }

    /**
     * 在 BlockEntity 的 serverTick 中调用
     */
    default void componentTick(World world, Direction side) {
        Item component = getComponent(side);
        if (component instanceof AbstractComponentItem compItem) {
            compItem.useComponent(world, this, side);
        }
    }

    /**
     * 破坏掉落
     */
    default Map<Direction, Item> getAllComponentsForDrop() {
        return Collections.unmodifiableMap(getAttachedComponents());
    }

    /**
     * @return 方块实体位置
     */
    default BlockPos getPos() {
        if (this instanceof BlockEntity be) {
            return be.getPos();
        }
        throw new IllegalStateException(
                "ComponentAdaptation must be implemented by BlockEntity"
        );
    }
}
