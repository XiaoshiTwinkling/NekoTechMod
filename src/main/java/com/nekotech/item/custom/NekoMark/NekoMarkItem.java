package com.nekotech.item.custom.NekoMark;

import com.nekotech.item.ModItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;

public class NekoMarkItem extends ModItem {
    private final DyeColor color;

    public NekoMarkItem(Settings settings, String name, DyeColor color) {
        super(settings, name);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof CatEntity cat)) {
            return ActionResult.PASS;
        }

        if (user.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        ((NekoMarkAccess) cat).neko_technology$setNekoMarkColor(this.color);

        stack.decrementUnlessCreative(1, user);

        return ActionResult.SUCCESS;
    }
}
