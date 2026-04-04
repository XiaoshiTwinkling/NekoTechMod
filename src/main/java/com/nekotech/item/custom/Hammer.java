package com.nekotech.item.custom;

import com.nekotech.item.AbstractDurabilityItem;
import net.minecraft.item.Item;

public class Hammer extends Item implements AbstractDurabilityItem {
    public Hammer(Item.Settings settings) {
        super(settings.maxDamage(128));
    }
}
