package com.nekotech.item.custom;

import com.nekotech.block.entity.machines.AlloyPotBlockEntity;
import com.nekotech.block.entity.machines.HeaterBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Thermometer extends Item {
    public Thermometer(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand){
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if(user.isSneaking()){
                ActionResult result = measureTemperature(world, user, stack);
                if (result == ActionResult.SUCCESS) {
                    return TypedActionResult.success(stack);
                } else if (result == ActionResult.FAIL) {
                    return TypedActionResult.fail(stack);
                }
            }
        } else {
            user.swingHand(hand);
        }

        return TypedActionResult.pass(stack);
    }

    private ActionResult measureTemperature(World world, PlayerEntity player, ItemStack thermometer) {

        HitResult hit = player.raycast(5.0, 0.0F, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = world.getBlockState(pos);

            // 获取方块实体
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof HeaterBlockEntity heater) {
                // 是加热器方块实体
                float temperature = heater.getTemperature();
                float maxTemperature = heater.getMax_temperature();

                // 发送温度信息给玩家
                sendTemperatureMessage(player, temperature, maxTemperature);


                return ActionResult.SUCCESS;

            } else if (blockEntity instanceof AlloyPotBlockEntity pot){

                float temperature = pot.getHeaterTemperature();
                float maxTemperature = pot.getHeaterMaxTemperature();

                // 发送温度信息给玩家
                sendTemperatureMessage(player, temperature, maxTemperature);

                return ActionResult.SUCCESS;

            } else {
                // 不是加热器
                sendErrorMessage(player, "这不是加热器方块");
                return ActionResult.FAIL;
            }
        } else {
            // 没有指向方块
            sendErrorMessage(player, "请对准一个加热器方块");
            return ActionResult.FAIL;
        }
    }

    private void sendTemperatureMessage(PlayerEntity player, float temperature, float maxTemperature) {
        // 根据温度范围选择颜色
        String color = getTemperatureColor(temperature, maxTemperature);

        // 格式化温度文本
        String message = String.format("§%sCurrent temperature: %.3f°C (%.0f%%)  Highest temperature: %.3f°C",
                color, temperature, (temperature / maxTemperature) * 100, maxTemperature);

        // 发送给玩家
        player.sendMessage(Text.literal(message), false);

        // 在 ActionBar 显示温度
        player.sendMessage(Text.literal(String.format("%.3f°C", temperature)), true);
    }

    private String getTemperatureColor(float temperature, float maxTemperature) {
        float percentage = temperature / maxTemperature;

        if (temperature <= 0) {
            return "7";  // 灰色，无温度
        } else if (percentage < 0.2) {
            return "b";  // 淡蓝色，低温
        } else if (percentage < 0.4) {
            return "a";  // 绿色，中低温
        } else if (percentage < 0.6) {
            return "e";  // 黄色，中等温度
        } else if (percentage < 0.8) {
            return "6";  // 金色，中高温
        } else if (percentage < 1.0) {
            return "c";  // 红色，高温
        } else {
            return "4";  // 深红色，超高温
        }
    }

    private void sendErrorMessage(PlayerEntity player, String message) {
        player.sendMessage(Text.literal("§c" + message), false);
    }
}
