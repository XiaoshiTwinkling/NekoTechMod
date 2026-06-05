package com.nekotech.item.custom.component;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 可以把物品输出到对面容器的零件喵~
 */
public class ItemOutputerItem extends AbstractComponentItem {
    final float outputSpeed; // 物品每几秒输出一个

    private long lastOutputTime = 0;

    public ItemOutputerItem(float outputSpeed, String tooltipTranslationKey) {
        super(new Settings().maxCount(16), tooltipTranslationKey);
        this.outputSpeed = outputSpeed;
    }

    @Override
    public void useComponent(World world, ComponentAdaptation self, Direction side) {
        // 获取目标容器位置
        BlockPos targetPos = self.getPos().offset(side);
        BlockEntity neighborEntity = world.getBlockEntity(targetPos);

        // 检查源容器和目标容器
        if (!(neighborEntity instanceof Inventory targetInventory)) {
            return;
        }

        if (!((self instanceof Inventory sourceInventory) && (self instanceof IcanItemIO))) {
            return;
        }

        // 检查冷却时间
        long currentTime = world.getTime();
        long intervalTicks = (long)(20 / outputSpeed);

        if (currentTime - lastOutputTime < intervalTicks) {
            return;
        }

        lastOutputTime = currentTime;

        // 从源容器中提取一个合适的物品
        ItemStack extractedStack = extractItemFromSource(sourceInventory, targetInventory, side, (IcanItemIO) self);

        if (extractedStack.isEmpty()) {
            return;
        }

        // 尝试插入到目标容器
        boolean inserted = insertItemToTarget(targetInventory, extractedStack, side.getOpposite());

        if (!inserted) {
            // 如果插入失败，将物品放回源容器
            insertItemBackToSource(sourceInventory, extractedStack);
        } else {
            // 如果插入成功，标记源容器为脏
            if (sourceInventory instanceof BlockEntity sourceBlockEntity) {
                sourceBlockEntity.markDirty();
            }
        }
    }

    /**
     * 从源容器中提取一个合适的物品
     */
    private ItemStack extractItemFromSource(Inventory source, Inventory target, Direction outputSide, IcanItemIO sourceIo) {
        for (int i = 0; i < source.size(); i++) {
            ItemStack stackInSlot = source.getStack(i);

            if (stackInSlot.isEmpty()) {
                continue;
            }

            // 检查目标容器是否能接收此物品
            if (canTargetAcceptItem(target, stackInSlot, outputSide.getOpposite())) {
                ItemStack extracted = stackInSlot.split(1);

                if (stackInSlot.isEmpty()) {
                    source.setStack(i, ItemStack.EMPTY);
                } else {
                    source.setStack(i, stackInSlot);
                }

                return extracted;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 检查目标容器是否能接收指定物品
     */
    private boolean canTargetAcceptItem(Inventory target, ItemStack stack, Direction inputSide) {
        // 如果目标容器实现了SidedInventory，遵循面规则
        if (target instanceof SidedInventory sidedInventory) {
            return canSidedInventoryAcceptItem(sidedInventory, stack, inputSide);
        }

        // 普通Inventory的检查
        return canNormalInventoryAcceptItem(target, stack);
    }

    /**
     * 检查SidedInventory是否能接收物品
     */
    private boolean canSidedInventoryAcceptItem(SidedInventory sidedInventory, ItemStack stack, Direction side) {
        // 获取该面允许的槽位
        int[] availableSlots = sidedInventory.getAvailableSlots(side);

        for (int slot : availableSlots) {
            // 检查该槽位是否允许插入
            if (sidedInventory.canInsert(slot, stack, side)) {
                ItemStack slotStack = sidedInventory.getStack(slot);

                if (slotStack.isEmpty()) {
                    return true; // 空槽位，可以插入
                } else if (ItemStack.areItemsEqual(slotStack, stack)) {
                    if (slotStack.getCount() < slotStack.getMaxCount()) {
                        return true; // 相同物品且未满，可以插入
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查普通Inventory是否能接收物品
     */
    private boolean canNormalInventoryAcceptItem(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);

            if (slotStack.isEmpty()) {
                return true; // 空槽位，可以插入
            } else if (ItemStack.areItemsEqual(slotStack, stack)) {
                if (slotStack.getCount() < slotStack.getMaxCount()) {
                    return true; // 相同物品且未满，可以插入
                }
            }
        }

        return false;
    }

    /**
     * 将物品插入到目标容器
     */
    private boolean insertItemToTarget(Inventory target, ItemStack stack, Direction inputSide) {
        if (stack.isEmpty()) {
            return true;
        }

        // 处理SidedInventory
        if (target instanceof SidedInventory sidedInventory) {
            return insertToSidedInventory(sidedInventory, stack, inputSide);
        }

        // 处理普通Inventory
        return insertToNormalInventory(target, stack);
    }

    /**
     * 插入物品到SidedInventory
     */
    private boolean insertToSidedInventory(SidedInventory sidedInventory, ItemStack stack, Direction side) {
        int[] availableSlots = sidedInventory.getAvailableSlots(side);

        // 首先尝试合并到已有堆栈
        for (int slot : availableSlots) {
            if (sidedInventory.canInsert(slot, stack, side)) {
                ItemStack slotStack = sidedInventory.getStack(slot);

                if (ItemStack.areItemsEqual(slotStack, stack)) {
                    int maxStackSize = slotStack.getMaxCount();
                    int spaceAvailable = maxStackSize - slotStack.getCount();

                    if (spaceAvailable > 0) {
                        int transferAmount = Math.min(stack.getCount(), spaceAvailable);

                        slotStack.increment(transferAmount);
                        stack.decrement(transferAmount);
                        sidedInventory.setStack(slot, slotStack);

                        if (stack.isEmpty()) {
                            if (sidedInventory instanceof BlockEntity blockEntity) {
                                blockEntity.markDirty();
                            }
                            return true;
                        }
                    }
                }
            }
        }

        // 然后尝试插入到空槽位
        for (int slot : availableSlots) {
            if (sidedInventory.canInsert(slot, stack, side)) {
                ItemStack slotStack = sidedInventory.getStack(slot);

                if (slotStack.isEmpty()) {
                    sidedInventory.setStack(slot, stack.copy());
                    stack.setCount(0);

                    if (sidedInventory instanceof BlockEntity blockEntity) {
                        blockEntity.markDirty();
                    }
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 插入物品到普通Inventory
     */
    private boolean insertToNormalInventory(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        // 首先尝试合并到已有堆栈
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);

            if (ItemStack.areItemsEqual(slotStack, stack)) {
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

        // 然后尝试插入到空槽位
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);

            if (slotStack.isEmpty()) {
                inventory.setStack(i, stack.copy());
                stack.setCount(0);

                if (inventory instanceof BlockEntity blockEntity) {
                    blockEntity.markDirty();
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 将未成功输出的物品放回源容器
     */
    private void insertItemBackToSource(Inventory source, ItemStack stack) {
        for (int i = 0; i < source.size(); i++) {
            ItemStack slotStack = source.getStack(i);

            if (slotStack.isEmpty()) {
                source.setStack(i, stack.copy());
                stack.setCount(0);
                break;
            } else if (ItemStack.areItemsEqual(slotStack, stack)) {
                int maxStackSize = slotStack.getMaxCount();
                int spaceAvailable = maxStackSize - slotStack.getCount();

                if (spaceAvailable > 0) {
                    int transferAmount = Math.min(stack.getCount(), spaceAvailable);

                    slotStack.increment(transferAmount);
                    stack.decrement(transferAmount);
                    source.setStack(i, slotStack);

                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }
}