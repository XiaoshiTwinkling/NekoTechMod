package com.nekotech.block.entity.api.electrical;

/**
 * 这个接口用来表示 这个方块实体可以传输电力
 * 连在一起的一些实现该接口的方块实体可以视作一整块导体 导体可以拥有多个输入输出口
 * 当一个方块含有flux_input零件或者被flux_output零件对准时 这个方块会被标记为输入口
 * 当一个方块含有flux_output零件或者被flux_input零件对准时 这个方块会被标记为输出口
 */
public interface ITransferElectrical {
    /**
     * 判断这个方块实体当前是否可以传输电力喵~
     * 有些方块实体（比如断路器）可能在某些条件下不能导电
     *
     * @return 如果可以传输电力返回true，否则返回false喵~
     */
    default boolean canTransfer() {
        return true;
    }
}
