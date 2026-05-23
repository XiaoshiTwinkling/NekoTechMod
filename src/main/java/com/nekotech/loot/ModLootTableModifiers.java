package com.nekotech.loot;

import com.nekotech.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.InvertedLootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;

public class ModLootTableModifiers {
    private static final Identifier SHORT_GRASS_LOOT_TABLE_ID = Identifier.of("minecraft", "blocks/short_grass");
    private static final Identifier TALL_GRASS_LOOT_TABLE_ID = Identifier.of("minecraft", "blocks/tall_grass");
    private static final Identifier FERN_LOOT_TABLE_ID = Identifier.of("minecraft", "blocks/fern");

    public static void registerModifications() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Identifier currentId = key.getValue();

            //为猫草和猫薄荷添加战利品
            if (SHORT_GRASS_LOOT_TABLE_ID.equals(currentId) ||
                    TALL_GRASS_LOOT_TABLE_ID.equals(currentId) ||
                    FERN_LOOT_TABLE_ID.equals(currentId)) {

                LootPool.Builder additionalDropsPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(InvertedLootCondition.builder(
                                MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS))));

                additionalDropsPool.with(
                        ItemEntry.builder(Items.WHEAT_SEEDS)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1)))
                                .conditionally(RandomChanceLootCondition.builder(0.125f))
                );

                additionalDropsPool.with(
                        ItemEntry.builder(ModItems.PETGRASS_SEEDS)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1)))
                                .conditionally(RandomChanceLootCondition.builder(0.08f))
                );

                additionalDropsPool.with(
                        ItemEntry.builder(ModItems.CATNIP_SEEDS)
                                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1)))
                                .conditionally(RandomChanceLootCondition.builder(0.08f))
                );

                tableBuilder.pool(additionalDropsPool.build());
            }
        });

    }

}
