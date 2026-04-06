package com.nekotech.block.entity;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
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
        // 只在服务端返回数据，客户端通过其他方式获取
        if (world.isClient()) {
            return null;
        }

        // 准备数据
        Text title = Text.translatable("block.nekotech.example_chest");
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            items.add(inventory.get(i).copy());
        }

        return new ContainerHUDData(items, title, 9, 3);
    }
}
