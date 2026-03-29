package com.nekotech.item.custom;

import com.mojang.serialization.Codec;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TailItem extends Item implements Equipment {

    private final Type type;

    public TailItem(Type type, Settings settings) {
        super(settings);
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
}