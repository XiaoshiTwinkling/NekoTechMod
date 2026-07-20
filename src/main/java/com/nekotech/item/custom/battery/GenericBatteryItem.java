package com.nekotech.item.custom.battery;

/**
 * 通用电池物品 可以通过构造函数指定容量和输出效率喵
 */
public class GenericBatteryItem extends BatteryItem {

    private final String materialName;

    public GenericBatteryItem(Settings settings, float maxCapacity, float maxDischargeRate, String materialName) {
        super(settings, maxCapacity, maxDischargeRate);
        this.materialName = materialName;
    }

    public String getMaterialName() {
        return materialName;
    }
}