package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class AbstractComponentItem extends Item {
    public AbstractComponentItem(Settings settings) {
        super(settings);
    }

    /**
     * 当玩家对着方块使用零件时的逻辑
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide(); // 玩家点击的是方块的哪一面
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        // 检查目标是否是可安装零件的机器
        if (!(world.getBlockEntity(pos) instanceof ComponentAdaptation machine)) {
            return ActionResult.PASS; // 不是目标方块，让其他物品处理
        }

        // 检查该机器是否允许在这个面安装这种零件
        if (!machine.canAttachComponent(side, this)) {
            // 如果不允许
            if (!world.isClient() && player != null) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
            }
            return ActionResult.FAIL;
        }

        // 服务器端执行安装逻辑
        if (!world.isClient()) {
            // 调用接口的方法进行安装
            boolean success = machine.attachComponent(side, this);
            if (success) {
                stack.decrement(1); // 消耗一个零件
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }

        return ActionResult.success(world.isClient());
    }
    /**
     * 每个具体零件都必须提供一个渲染器
     * @return 该零件对应的物品渲染器
     */
    protected abstract BuiltinModelItemRenderer getComponentRenderer();
}
