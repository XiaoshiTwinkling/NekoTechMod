package com.nekotech.item.api.chargeable_item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/**
 * 可充能物品
 */
public abstract class AbstractChargeableItem extends Item implements IChargeableItem {

    private static final String NEKO_FLUX_KEY = "NekoFlux";

    private final float maxNekoFlux;

    public AbstractChargeableItem(Settings settings, float maxNekoFlux) {
        // 可充能物品不可堆叠，每个物品有自己的能量
        super(settings.maxCount(1));
        this.maxNekoFlux = maxNekoFlux;
    }

    @Override
    public float getMaxNekoFlux(ItemStack stack) {
        return maxNekoFlux;
    }

    @Override
    public float getNekoFlux(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) {
            return 0;
        }
        NbtCompound nbt = component.copyNbt();
        if (!nbt.contains(NEKO_FLUX_KEY)) {
            return 0;
        }
        return nbt.getFloat(NEKO_FLUX_KEY);
    }

    @Override
    public void setNekoFlux(ItemStack stack, float flux) {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat(NEKO_FLUX_KEY, MathHelper.clamp(flux, 0, maxNekoFlux));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return maxNekoFlux > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        float ratio = getNekoFlux(stack) / maxNekoFlux;
        return Math.round(ratio * 13);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getNekoFlux(stack) / maxNekoFlux;
        if (ratio > 0.75f) {
            return 0x00FF00;
        } else if (ratio > 0.25f) {
            return 0xFFFF00;
        } else {
            return 0xFF0000;
        }
    }

    /**
     * 尝试消耗能量并执行功能。子类应重写此方法实现具体行为。
     * @return true 表示消耗成功并执行了功能，false 表示能量不足或无法使用
     */
    public abstract boolean performAction(ItemStack stack, World world, PlayerEntity player, Hand hand);

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!canUse(stack)) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("message.neko-technology.energy_depleted"), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (performAction(stack, world, user, hand)) {
            consumeNekoFlux(stack, getEnergyCostPerUse(stack));
            return TypedActionResult.success(stack);
        }
        return TypedActionResult.pass(stack);
    }

    /**
     * 每次使用消耗的能量
     */
    protected float getEnergyCostPerUse(ItemStack stack) {
        return 1.0f;
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        float current = getNekoFlux(stack);
        float max = getMaxNekoFlux(stack);
        tooltip.add(Text.translatable("tooltip.neko-technology.energy",
                        String.format("%.0f", current), String.format("%.0f", max))
                .formatted(Formatting.GRAY));
    }
}