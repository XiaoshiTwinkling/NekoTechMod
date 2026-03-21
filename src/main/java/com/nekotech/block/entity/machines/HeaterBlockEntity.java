package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ImplementedInventory;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.data.AlloyFurnaceData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HeaterBlockEntity extends MachineBlockEntity implements ExtendedScreenHandlerFactory<AlloyFurnaceData> , ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state
    , float maximumFurnaceTemperature, float basicHeatingRate, PropertyDelegate propertyDelegate) {
        super(type, pos, state);
        this.propertyDelegate = propertyDelegate;
    }

    private float maximumFurnaceTemperature;  //基础最大炉温，加热器旁边每添加一块砖，就可以加50炉温
    private float basicHeatingRate;           //基础每tick升温速度，加热器旁边每添加一块砖，就可以加1.0速度
    private final float coolingRate = 0.4F;

    private float HeatingRate;

    private float temperature = 0.0F; //温度
    private int fuelTime = 0;  //这个燃料的剩余燃烧时间
    private int maxFuelTime = 0;

    private static final int INPUT_SLOT_1 = 0;   //输入槽1

    protected final PropertyDelegate propertyDelegate;

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.basic_alloy_furnace, pos, state);

        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return (int) switch (index) {
                    case 0 -> temperature;   // 温度
                    case 1 -> fuelTime;      // 这个燃料的剩余燃烧时间
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> temperature = value;
                    case 1 -> fuelTime = value;
                }
            }

            @Override
            public int size() {
                return 1;  // 1个属性
            }
        };
    }

    private boolean isFuel(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COAL || item == Items.CHARCOAL;
    }

    @Override
    public int getMaxCountPerStack(){
        return 64;
    }

    // 获取燃料燃烧时间 木炭和煤炭
    private int getFuelTime(ItemStack fuel) {
        if (fuel.getItem() == Items.COAL) {
            return 160;
        } else if (fuel.getItem() == Items.CHARCOAL) {
            return 160;
        }
        return 0;
    }

    private boolean canInsertIntoSlot(int slot, ItemStack stack) {
        ItemStack current = getStack(slot);

        if (current.isEmpty()) {
            return true;
        }

        if (!ItemStack.areItemsEqual(current, stack)) {
            return false;
        }

        int newCount = current.getCount() + stack.getCount();
        return newCount <= getMaxCountPerStack();
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    // 重置进度
    private void resetProgress() {
        if (fuelTime != 0) {
            fuelTime = 0;
        }
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return;
        }

        boolean shouldMarkDirty = false;

        //检查这个燃料有没有烧完
        if (fuelTime <= 0) {
            ItemStack fuelStack = getStack(INPUT_SLOT_1);
            if (fuelStack.getCount() > 0 && getFuelTime(fuelStack)!=0) {

                fuelTime = getFuelTime(fuelStack);
                maxFuelTime = fuelTime;
                fuelStack.decrement(1);// 消耗1个燃料
                shouldMarkDirty = true;

                if (fuelStack.isEmpty()) {
                    setStack(INPUT_SLOT_1, ItemStack.EMPTY);
                }
            }
        } else {
            // 消耗燃料
            fuelTime--;
            shouldMarkDirty = true;
        }

        //处理炉温机制
        if(fuelTime > 1){
            if(temperature + basicHeatingRate >= maximumFurnaceTemperature){
                temperature = maximumFurnaceTemperature;
                shouldMarkDirty = true;
            } else {
                temperature += maximumFurnaceTemperature;
                shouldMarkDirty = true;
            }
        } else {
            if(temperature - coolingRate <= 0F){
                temperature = coolingRate;
                shouldMarkDirty = true;
            } else {
                temperature -= coolingRate;
                shouldMarkDirty = true;
            }
        }



        if (shouldMarkDirty) {
            markDirty(world, pos, state);
        }
    }


    @Override
    public AlloyFurnaceData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return new AlloyFurnaceData(pos);
    }

    @Override
    public Text getDisplayName() {
        return null;
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, false, registryLookup);
        nbt.putInt("temperature", (int)temperature);
        nbt.putInt("fuelTime", fuelTime);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        temperature = nbt.getInt("temperature");
        fuelTime = nbt.getInt("fuelTime");
    }
}
