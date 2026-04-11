package com.nekotech.item.custom;

import com.mojang.serialization.Codec;
import com.nekotech.item.ModItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TailItem extends ModItem implements Equipment {

    private final Type type;

    public TailItem(Type type, Settings settings, String tooltipTranslationKey) {
        super(settings, tooltipTranslationKey);
        this.type = type;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 复用原版装备逻辑（自动穿戴 + 替换 + 同步）
        return this.equipAndSwap(this, world, user, hand);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return this.type.getEquipmentSlot();
    }

    // ===== 类型定义（可扩展多个饰品类型）=====
    public enum Type implements StringIdentifiable {

        TAIL(EquipmentSlot.LEGS, "tail");

        public static final Codec<Type> CODEC =
                StringIdentifiable.createBasicCodec(Type::values);

        private final EquipmentSlot slot;
        private final String name;

        Type(EquipmentSlot slot, String name) {
            this.slot = slot;
            this.name = name;
        }

        public EquipmentSlot getEquipmentSlot() {
            return this.slot;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }

    /**
     * 检查玩家是否同时装备了猫尾和猫耳
     * @param player 要检查的玩家
     * @return 如果同时装备了猫尾和猫耳则返回 true
     */
    public static boolean hasTailAndEars(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        // 检查玩家是否装备了猫尾
        ItemStack tailStack = player.getEquippedStack(EquipmentSlot.LEGS);
        boolean hasTail = !tailStack.isEmpty() && tailStack.getItem() instanceof TailItem;

        // 检查玩家是否装备了猫耳
        ItemStack hatStack = player.getEquippedStack(EquipmentSlot.HEAD);
        boolean hasEars = !hatStack.isEmpty() &&
                (hatStack.getItem().getTranslationKey().equals("item.neko-technology.neko_ears") ||
                        hatStack.getItem() instanceof com.nekotech.item.custom.HatItem);

        return hasTail && hasEars;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof PlayerEntity player) {
            if (slot >= 0 && slot < 4 && isEquippedByPlayer(stack, player)) {
                if (hasTailAndEars(player)) {

                    StatusEffectInstance speedEffect = new StatusEffectInstance(StatusEffects.SPEED, 100, 0, false, false, true);

                    // 检查是否已经有迅捷效果，如果没有则添加
                    if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                        player.addStatusEffect(speedEffect);
                    } else {
                        // 如果已有迅捷效果，确保持续时间是足够的
                        StatusEffectInstance existingEffect = player.getStatusEffect(StatusEffects.SPEED);
                        if (existingEffect != null && existingEffect.getDuration() < 50) {
                            // 如果效果持续时间快结束了，刷新它
                            player.addStatusEffect(speedEffect);
                        }
                    }
                } else if (player.hasStatusEffect(StatusEffects.SPEED)) {
                    StatusEffectInstance effect = player.getStatusEffect(StatusEffects.SPEED);
                    if (effect != null && effect.getDuration() <= 100 + 20) {
                    }
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    /**
     * 检查这个物品是否被指定玩家装备
     * @param stack 要检查的物品堆栈
     * @param player 玩家实体
     * @return 如果物品被玩家装备返回 true
     */
    private boolean isEquippedByPlayer(ItemStack stack, PlayerEntity player) {
        // 检查各个装备槽位
        for (ItemStack armorStack : player.getArmorItems()) {
            if (ItemStack.areItemsEqual(armorStack, stack)) {
                return true;
            }
        }
        return false;
    }
}