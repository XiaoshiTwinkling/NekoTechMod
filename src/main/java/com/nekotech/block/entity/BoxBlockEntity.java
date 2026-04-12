package com.nekotech.block.entity;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BoxBlockEntity extends LootableContainerBlockEntity implements IHaveGoogleHUD {
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
    public BoxBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    public BoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(ModBlockEntities.basic_storage_enclosure, blockPos, blockState);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.box");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    @Nullable
    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {

        List<ItemStack> items = new ArrayList<>();

        if (this instanceof Inventory inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                items.add(stack.copy()); // 复制一份，避免修改原物品
            }
        } else if (this instanceof ImplementedInventory implementedInventory) {
            for (int i = 0; i < implementedInventory.size(); i++) {
                ItemStack stack = implementedInventory.getStack(i);
                items.add(stack.copy());
            }
        }

        int columns = 9;
        int rows = 3;
        Text title = Text.translatable("container.box");

        return new ContainerHUDData(items, title, columns, rows);
    }
}
