package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.ModBlockEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HeaterBlockEntity extends BlockEntity implements SidedInventory {
    private static final int FUEL_SLOT = 0;
    private static final int MAX_BURN_TIME = 100; // 示例值
    private static final float MAX_TEMPERATURE = 400.0f;
    private static final float TEMPERATURE_RISING_RATE = 0.5f;

    private final DefaultedList<ItemStack> fuelSlots = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float temperature = 0.0f;
    private float temperature_rising_rate = 0.5f;

    private int burnTime = 0;
    private int maxBurnTime = 0;

    private boolean isLit = false;

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.heater, pos, state);
    }


    @Override
    public int size() {
        return fuelSlots.size();
    }

    @Override
    public boolean isEmpty() {
        return fuelSlots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return fuelSlots.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(fuelSlots, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(fuelSlots, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        fuelSlots.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == FUEL_SLOT && isFuel(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false; // 不允许提取燃料
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{FUEL_SLOT};
    }

    @Override
    public void clear() {
        fuelSlots.clear();
        markDirty();
    }


    public boolean addFuel(ItemStack stack) {
        if (isFuel(stack) && !isFull()) {
            ItemStack current = getStack(FUEL_SLOT);
            if (current.isEmpty()) {
                setStack(FUEL_SLOT, stack.copyWithCount(1));
            } else {
                current.increment(1);
            }
            startBurning();
            return true;
        }
        return false;
    }

    private boolean isFuel(ItemStack stack) {
        // 只允许煤炭
        return stack.getItem() == Items.COAL;
    }

    private boolean isFull() {
        return getStack(FUEL_SLOT).getCount() >= getMaxCountPerStack();
    }

    private void startBurning() {
        if (!isHeating()) {
            burnTime = maxBurnTime = MAX_BURN_TIME;
            markDirty();
        }
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    public float getTemperatureRisingRate(){
        return temperature_rising_rate;
    }

    public float getMax_temperature(){
        return MAX_TEMPERATURE;
    }

    public void temperatureRising(){
        if(isHeating()){
            if(temperature + getTemperatureRisingRate() >= getMax_temperature()){
                temperature = getMax_temperature();
            } else {
                temperature += getTemperatureRisingRate();
            }
        } else {
            if(temperature >= 1){
                temperature -= 1.0F;
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        Inventories.writeNbt(nbt, fuelSlots, false, registryLookup);

        nbt.putFloat("Temperature", temperature);
        nbt.putFloat("TemperatureRisingRate", temperature_rising_rate);
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("MaxBurnTime", maxBurnTime);
        nbt.putBoolean("IsLit", isLit);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        Inventories.readNbt(nbt, fuelSlots, registryLookup);

        temperature = nbt.getFloat("Temperature");
        temperature_rising_rate = nbt.getFloat("TemperatureRisingRate");
        burnTime = nbt.getInt("BurnTime");
        maxBurnTime = nbt.getInt("MaxBurnTime");
        isLit = nbt.getBoolean("IsLit");
    }


    public DefaultedList<ItemStack> getItems() {
        return fuelSlots;
    }

    public boolean checkType(ItemStack stack) {
        return isFuel(stack);
    }

    public boolean isClient() {
        return world != null && world.isClient();
    }

    public static void tick(World world, BlockPos pos, BlockState state, HeaterBlockEntity blockEntity) {
        if (world.isClient()) {
            // 客户端逻辑
            blockEntity.clientTick();
        } else {
            // 服务端逻辑
            blockEntity.serverTick(world, pos, state);
        }
    }

    //@Environment(EnvType.SERVER)
    public void serverTick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;
        boolean shouldBeLit = isHeating();

        if(getStack(FUEL_SLOT).getCount() > 0){
            if(!isHeating()){
                getStack(FUEL_SLOT).decrement(1);
                startBurning();
            } else {
                temperatureRising();
                burnTime --;
            }
        } else {
            temperatureRising();
            if(isHeating())burnTime --;

        }
        markDirty(world, pos, state);

        NekoTechnology.LOGGER.info(String.valueOf(burnTime));
        NekoTechnology.LOGGER.info(String.valueOf(temperature));


        this.isLit = shouldBeLit;
        // 更新方块状态
        BlockState currentState = world.getBlockState(pos);
        if (currentState.contains(com.nekotech.item.custom.Heater.LIT)) {
            world.setBlockState(pos, currentState.with(com.nekotech.item.custom.Heater.LIT, shouldBeLit));
        }

        this.markDirty();
    }

    //@Environment(EnvType.CLIENT)
    public void clientTick() {

        if (world != null) {
            BlockState state = world.getBlockState(pos);
            if (state.contains(com.nekotech.item.custom.Heater.LIT) && state.get(com.nekotech.item.custom.Heater.LIT)) {

                if (world.getRandom().nextInt(3) == 0) {
                    double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.2;
                    double y = pos.getY() + 0.8;
                    double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.2;
                    world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
                }

                if (world.getRandom().nextInt(8) == 0) {
                    double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
                    double y = pos.getY() + 1.0;
                    double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.01, 0.0);
                }
            }
        }
    }

    public boolean isHeating() {
        return this.burnTime > 1;
    }
}