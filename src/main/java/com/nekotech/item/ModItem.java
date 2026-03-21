// src/main/java/com/yourmod/item/CustomItem.java
package com.nekotech.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ModItem extends Item {

    // 存储翻译键
    private final String tooltipTranslationKey;

    public ModItem(Settings settings, String tooltipTranslationKey) {

        super(settings);

        this.tooltipTranslationKey = "item.neko-technology." + tooltipTranslationKey + ".tooltip";

    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(!Text.translatable(tooltipTranslationKey).getString().equals(tooltipTranslationKey)) {
            tooltip.add(Text.translatable(tooltipTranslationKey).formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            super.appendTooltip(stack, context, tooltip, type);
        }
    }
}