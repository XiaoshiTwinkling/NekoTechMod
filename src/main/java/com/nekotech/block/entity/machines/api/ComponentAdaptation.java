package com.nekotech.block.entity.machines.api;

import net.minecraft.item.Item;
import net.minecraft.util.math.Direction;
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
    /**
     * 用于存储六个面零件的容器
     * Key: 方块的朝向 (Direction)
     * Value: 零件的物品实例 (Item)
     */
    Map<Direction, Item> getAttachedComponents();

    /**
     * @return 该方块允许安装的零件类型集合
     */
    Set<Item> getValidComponents();

    /**
     * 检查某个方向的零件是否可以被安装
     */
    default boolean canAttachComponent(Direction side, Item component) {
        return getValidComponents().contains(component);
    }

    /**
     * 尝试在指定面安装零件
     * @return 是否安装成功
     */
    default boolean attachComponent(Direction side, Item component) {
        if (canAttachComponent(side, component)) {
            getAttachedComponents().put(side, component);
            return true;
        }
        return false;
    }

    /**
     * 移除指定面的零件
     */
    default void removeComponent(Direction side) {
        getAttachedComponents().remove(side);
    }

    /**
     * 获取指定面的零件
     */
    @Nullable
    default Item getComponent(Direction side) {
        return getAttachedComponents().get(side);
    }

    /**
     * 当方块被破坏时，获取所有应该掉落的零件
     */
    default Map<Direction, Item> getAllComponentsForDrop() {
        return Collections.unmodifiableMap(getAttachedComponents());
    }
}
