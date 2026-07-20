package com.nekotech.item.custom.battery;

import com.nekotech.item.api.chargeable_item.AbstractChargeableItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * 电池物品基类，继承自 AbstractChargeableItem，增加输出效率（放电速率）属性
 * 子类可通过构造函数设定最大容量和每 tick 最大输出能量
 * 提供 discharge() 方法用于向外部设备或自身功能供电
 */
public abstract class BatteryItem extends AbstractChargeableItem {

    /** 每 tick 最大输出能量（NF/tick） */
    private final float maxDischargeRate;

    /**
     * @param settings          物品设置
     * @param maxCapacity       最大能量容量（NF）
     * @param maxDischargeRate  每 tick 最大输出能量（NF/tick） 0 表示不可放电
     */
    public BatteryItem(Settings settings, float maxCapacity, float maxDischargeRate) {
        super(settings, maxCapacity);
        this.maxDischargeRate = maxDischargeRate;
    }

    /**
     * 获取当前电池的输出效率（NF/tick）
     */
    public float getMaxDischargeRate() {
        return maxDischargeRate;
    }

    /**
     * 尝试从电池中放出指定数量的能量。
     * 实际放出的能量受限于当前电量、输出效率和请求量。
     *
     * @param stack   电池物品栈
     * @param request 请求放出的能量（NF）
     * @return 实际放出的能量（NF），可能小于 request
     */
    public float discharge(ItemStack stack, float request) {
        if (request <= 0 || maxDischargeRate <= 0) return 0;

        float available = getNekoFlux(stack);
        float actual = Math.min(request, Math.min(maxDischargeRate, available));
        if (actual > 0) {
            setNekoFlux(stack, available - actual);
        }
        return actual;
    }

    @Override
    public boolean performAction(ItemStack stack, World world, PlayerEntity player, Hand hand) {
        return true;
    }

    @Override
    protected float getEnergyCostPerUse(ItemStack stack) {
        return 0;
    }
}