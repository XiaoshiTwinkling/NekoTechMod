package com.nekotech.item.custom.component;

import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 可以把对面容器物品吸过来的零件喵~
 */
public class ItemInputerItem extends AbstractComponentItem{
    final float inputSpeed; //物品每几秒输入一个

    private long lastExtractTime = 0;

    public ItemInputerItem(float inputSpeed, String tooltipTranslationKey) {
        super(new Settings().maxCount(16), tooltipTranslationKey);
        this.inputSpeed = inputSpeed;
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        BlockPos targetPos = self.getPos().offset(side);
        BlockEntity neighborEntity = world.getBlockEntity(targetPos);

        if (!(neighborEntity instanceof Inventory sourceInventory)) {
            return;
        }

        if (!((self instanceof Inventory targetInventory) && (self instanceof IcanItemIO))) {
            return;
        }

        long currentTime = world.getTime();
        long intervalTicks = (long)(20 / inputSpeed);

        if (currentTime - lastExtractTime < intervalTicks) {
            return;
        }

        lastExtractTime = currentTime;

        ItemStack extractedStack = extractItemFromInventory(sourceInventory, targetInventory, side.getOpposite(), (IcanItemIO) self);

        if (!extractedStack.isEmpty()) {
            boolean inserted = insertItemToInventory(targetInventory, extractedStack);

            if (!inserted) {
                insertItemToInventory(sourceInventory, extractedStack);
            }
        }
    }

    /**
     * 从源容器中抽取一个合适的物品堆栈
     */
    private ItemStack extractItemFromInventory(Inventory source, Inventory target,Direction sourceSide, IcanItemIO targetIo) {
        for (int i = 0; i < source.size(); i++) {
            ItemStack stackInSlot = source.getStack(i);

            if (stackInSlot.isEmpty()) {
                continue;
            }

            if (canInsertItem(target, stackInSlot, targetIo)) {
                ItemStack extracted = stackInSlot.split(1);

                if (stackInSlot.isEmpty()) {
                    source.setStack(i, ItemStack.EMPTY);
                } else {
                    source.setStack(i, stackInSlot);
                }

                if (source instanceof BlockEntity blockEntity) {
                    blockEntity.markDirty();
                }

                return extracted;
            }
        }

        return ItemStack.EMPTY;
    }
    /**
     * 检查目标容器是否能接受指定物品
     */
    private boolean canInsertItem(Inventory inventory, ItemStack stack, IcanItemIO targetIo) {
        if (!targetIo.canInsertwithComponent(stack.copyWithCount(1))) {
            return false;
        }

        ItemStack testStack = stack.copyWithCount(1);

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);


            if (slotStack.isEmpty()) {
                return true;
            } else if (ItemStack.areItemsEqual(slotStack, testStack)) {
                if (slotStack.getCount() < slotStack.getMaxCount()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 将物品插入目标容器
     */
    private boolean insertItemToInventory(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        // 首先尝试合并到已有堆栈
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);

            if (ItemStack.areItemsEqual(slotStack, stack)) {
                // 相同物品，可以合并
                int maxStackSize = slotStack.getMaxCount();
                int spaceAvailable = maxStackSize - slotStack.getCount();

                if (spaceAvailable > 0) {
                    int transferAmount = Math.min(stack.getCount(), spaceAvailable);

                    slotStack.increment(transferAmount);
                    stack.decrement(transferAmount);
                    inventory.setStack(i, slotStack);

                    if (stack.isEmpty()) {
                        if (inventory instanceof BlockEntity blockEntity) {
                            blockEntity.markDirty();
                        }
                        return true;
                    }
                }
            }
        }

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);

            if (slotStack.isEmpty()) {
                inventory.setStack(i, stack.copy());
                stack.setCount(0);

                // 标记目标容器为脏
                if (inventory instanceof BlockEntity blockEntity) {
                    blockEntity.markDirty();
                }
                return true;
            }
        }

        return false; // 无法插入
    }
}
