package com.nekotech.util;

import net.minecraft.item.ItemStack;

public class SmithingRemainderHandle {

    public static final ThreadLocal<ItemStack> REMAINDER_STACK= new ThreadLocal<>();
}
