package com.nekotech.recipe;

import com.google.gson.JsonObject;
import com.nekotech.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合金炉配方（双输入 + 燃料消耗）
 */
public class AlloyRecipe {

    // 配方定义
    public static class RecipeDefinition {
        public final ItemStack input1;        // 合金槽1输入
        public final ItemStack input2;        // 合金槽2输入
        public final ItemStack alloyOutput;   // 合金输出
        public final ItemStack slagOutput;    // 炉渣输出
        public final int cookTime;            // 烹饪时间（tick）
        public final float experience;        // 获得的经验

        public RecipeDefinition(ItemStack input1, ItemStack input2,
                                ItemStack alloyOutput, ItemStack slagOutput,
                                int cookTime, float experience) {
            this.input1 = input1;
            this.input2 = input2;
            this.alloyOutput = alloyOutput;
            this.slagOutput = slagOutput;
            this.cookTime = cookTime;
            this.experience = experience;
        }

        // 检查配方是否匹配（忽略顺序）
        public boolean matches(ItemStack slot1, ItemStack slot2) {
            // 检查数量是否足够
            boolean quantityOk = slot1.getCount() >= input1.getCount() &&
                    slot2.getCount() >= input2.getCount();

            if (!quantityOk) return false;

            // 检查物品是否匹配（支持两种顺序）
            return (ItemStack.areItemsEqual(slot1, input1) &&
                    ItemStack.areItemsEqual(slot2, input2)) ||
                    (ItemStack.areItemsEqual(slot1, input2) &&
                            ItemStack.areItemsEqual(slot2, input1));
        }

        // 消耗输入物品
        public Pair<ItemStack, ItemStack> consumeInputs(ItemStack slot1, ItemStack slot2) {
            // 确定哪个槽位对应哪个输入
            if (ItemStack.areItemsEqual(slot1, input1) &&
                    ItemStack.areItemsEqual(slot2, input2)) {
                // 顺序匹配
                slot1.decrement(input1.getCount());
                slot2.decrement(input2.getCount());
            } else {
                // 交换匹配
                slot1.decrement(input2.getCount());
                slot2.decrement(input1.getCount());
            }

            return new Pair<>(slot1, slot2);
        }
    }

    // 配方管理器
    public static class RecipeManager {
        private static final Map<Identifier, RecipeDefinition> RECIPES = new HashMap<>();
        private static final List<RecipeDefinition> RECIPE_LIST = new ArrayList<>();

        // 注册配方
        public static void register(Identifier id, RecipeDefinition recipe) {
            RECIPES.put(id, recipe);
            RECIPE_LIST.add(recipe);
        }

        // 查找匹配的配方
        @Nullable
        public static RecipeDefinition findMatching(ItemStack slot1, ItemStack slot2) {
            if (slot1.isEmpty() || slot2.isEmpty()) {
                return null;
            }

            for (RecipeDefinition recipe : RECIPE_LIST) {
                if (recipe.matches(slot1, slot2)) {
                    return recipe;
                }
            }
            return null;
        }

        // 从 JSON 加载配方
        public static void loadFromJson(Identifier id, JsonObject json) {
            // 解析输入1
            JsonObject input1Json = JsonHelper.getObject(json, "input1");
            Item input1Item = Registries.ITEM.get(Identifier.of(JsonHelper.getString(input1Json, "item")));
            int input1Count = JsonHelper.getInt(input1Json, "count", 1);
            ItemStack input1 = new ItemStack(input1Item, input1Count);

            // 解析输入2
            JsonObject input2Json = JsonHelper.getObject(json, "input2");
            Item input2Item = Registries.ITEM.get(Identifier.of(JsonHelper.getString(input2Json, "item")));
            int input2Count = JsonHelper.getInt(input2Json, "count", 1);
            ItemStack input2 = new ItemStack(input2Item, input2Count);

            // 解析合金输出
            JsonObject alloyOutputJson = JsonHelper.getObject(json, "alloy_output");
            Item alloyOutputItem = Registries.ITEM.get(Identifier.of(JsonHelper.getString(alloyOutputJson, "item")));
            int alloyOutputCount = JsonHelper.getInt(alloyOutputJson, "count", 1);
            ItemStack alloyOutput = new ItemStack(alloyOutputItem, alloyOutputCount);

            // 解析炉渣输出
            ItemStack slagOutput = ItemStack.EMPTY;
            if (json.has("slag_output")) {
                JsonObject slagOutputJson = JsonHelper.getObject(json, "slag_output");
                Item slagOutputItem = Registries.ITEM.get(Identifier.of(JsonHelper.getString(slagOutputJson, "item")));
                int slagOutputCount = JsonHelper.getInt(slagOutputJson, "count", 1);
                slagOutput = new ItemStack(slagOutputItem, slagOutputCount);
            }

            // 解析其他参数
            int cookTime = JsonHelper.getInt(json, "cook_time", 200);
            float experience = JsonHelper.getFloat(json, "experience", 0.0f);

            RecipeDefinition recipe = new RecipeDefinition(
                    input1, input2, alloyOutput, slagOutput, cookTime, experience
            );

            register(id, recipe);
        }

        // 初始化默认配方
        public static void initDefaultRecipes() {
            // 示例配方1：猫毛 + 煤炭 = 强化猫毛
            register(
                    Identifier.of("nekotech", "enhanced_neko_hair"),
                    new RecipeDefinition(
                            new ItemStack(ModItems.neko_hair, 1),      // 输入1
                            new ItemStack(Items.COAL, 1),              // 输入2
                            new ItemStack(ModItems.enhanced_neko_hair, 1), // 合金输出
                            new ItemStack(ModItems.small_handful_of_slag, 1),       // 炉渣
                            400,  // 烹饪时间
                            1.0f  // 经验
                    )
            );
            register(
                    Identifier.of("nekotech", "neko_copper_ingot"),
                    new RecipeDefinition(
                            new ItemStack(Items.COPPER_INGOT, 1),      // 输入1
                            new ItemStack(ModItems.neko_hair, 1),              // 输入2
                            new ItemStack(ModItems.neko_copper_ingot, 1), // 合金输出
                            new ItemStack(ModItems.small_handful_of_slag, 1),       // 炉渣
                            600,  // 烹饪时间
                            1.0f  // 经验
                    )
            );
            register(
                    Identifier.of("nekotech", "pig_iron_ingot"),
                    new RecipeDefinition(
                            new ItemStack(Items.IRON_INGOT, 4),      // 输入1
                            new ItemStack(Items.COAL, 1),              // 输入2
                            new ItemStack(ModItems.pig_iron_ingot, 4), // 合金输出
                            new ItemStack(ModItems.small_handful_of_slag, 2),       // 炉渣
                            600,  // 烹饪时间
                            1.0f  // 经验
                    )
            );
        }

        // 获取所有配方
        public static List<RecipeDefinition> getAllRecipes() {
            return new ArrayList<>(RECIPE_LIST);
        }
    }
}