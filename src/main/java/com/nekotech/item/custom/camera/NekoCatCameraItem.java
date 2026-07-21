package com.nekotech.item.custom.camera;

import com.nekotech.item.api.chargeable_item.AbstractChargeableItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class NekoCatCameraItem extends AbstractChargeableItem {
    private static final float MAX_ENERGY = 10000.0f;

    public NekoCatCameraItem(Settings settings) {
        super(settings, MAX_ENERGY);
    }

    @Override
    public boolean performAction(ItemStack stack, World world, PlayerEntity player, Hand hand) {
        return false;
    }

    @Override
    protected float getEnergyCostPerUse(ItemStack stack) {
        return 0;
    }
}
