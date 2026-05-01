package com.nekotech.block.entity.api.electrical;

/**
 * 这个接口用来表示 这个方块实体可以传输电力
 * 连在一起的一些实现该接口的方块实体可以视作一整块导体 导体可以拥有多个输入输出口
 * 当一个方块含有flux_input零件或者被flux_output零件对准时 这个方块会被标记为输入口
 * 当一个方块含有flux_output零件或者被flux_input零件对准时 这个方块会被标记为输出口
 */
public interface ITransferElectrical {}
