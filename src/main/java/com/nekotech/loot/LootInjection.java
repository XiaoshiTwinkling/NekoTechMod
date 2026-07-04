package com.nekotech.loot;

import com.nekotech.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

public class LootInjection {
    private static final Identifier CAT_MORNING_GIFT =
            Identifier.of("minecraft", "gameplay/cat_morning_gift");

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!CAT_MORNING_GIFT.equals(key.getValue())) return;

            LootPool.Builder poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(ItemEntry.builder(ModItems.CATNIP_SEEDS)
                            .weight(3)
                            .apply(SetCountLootFunction.builder(
                                    UniformLootNumberProvider.create(1.0f, 1.0f)))
                    );

            tableBuilder.pool(poolBuilder);
        });
    }
}
