package com.nekotech.genetics;

import net.minecraft.entity.passive.CatEntity;

/**
 * 表示性状喵
 */
public interface Trait {
    void applyTo(CatEntity cat);
}
