package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HeaterBlockEntity extends MachineBlockEntity implements SidedInventory {
    private static final int FUEL_SLOT = 0;
    private static final int MAX_BURN_TIME = 600;
    private static final float MAX_TEMPERATURE = 400.0f;
    private static final float TEMPERATURE_RISING_RATE = 0.14f;
    private static final float BRICK_TEMPERATURE_BONUS = 50.0f;  // 每个砖块增加的温度
    private static final int BRICK_CHECK_RANGE = 1;  // 检查砖块范围

    private final DefaultedList<ItemStack> fuelSlots = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float temperature = 0.0f;
    private float temperature_rising_rate = TEMPERATURE_RISING_RATE;
    private float max_temperature = MAX_TEMPERATURE ;

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

    public float getTemperature() {
        return temperature;
    }

    //数范围内的砖块数量喵
    public int countBricksInRange(World world, BlockPos center) {
        int brickCount = 0;
        for (int xOffset = -BRICK_CHECK_RANGE; xOffset <= BRICK_CHECK_RANGE; xOffset++) {
            for (int yOffset = -BRICK_CHECK_RANGE; yOffset <= BRICK_CHECK_RANGE; yOffset++) {
                for (int zOffset = -BRICK_CHECK_RANGE; zOffset <= BRICK_CHECK_RANGE; zOffset++) {
                    BlockPos checkPos = center.add(xOffset, yOffset, zOffset);
                    BlockState blockState = world.getBlockState(checkPos);
                    if (isBrick(blockState)) {
                        brickCount++;
                    }
                }
            }
        }
        return brickCount;
    }

    //可汗大点兵
    private boolean isBrick(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BRICKS ||              // 砖块
                block == Blocks.NETHER_BRICKS ||      // 下界砖块
                block == Blocks.END_STONE_BRICKS ||   // 末地石砖
                block == Blocks.STONE_BRICKS ||       // 石砖
                block == Blocks.DEEPSLATE_BRICKS ||   // 深板岩砖
                block == Blocks.MUD_BRICKS ||         // 泥砖
                block == Blocks.QUARTZ_BLOCK ||       // 石英块
                block == Blocks.PURPUR_BLOCK ||       // 紫珀块
                block == Blocks.PRISMARINE_BRICKS;    // 海晶石砖
    }

    public boolean addFuel(ItemStack stack) {
        if (isFuel(stack) && !isFull()) {
            ItemStack current = getStack(FUEL_SLOT);
            if (current.isEmpty()) {
                setStack(FUEL_SLOT, stack.copyWithCount(1));
            } else {
                current.increment(1);
            }
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
        return max_temperature;
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



    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {
        final int countBricks = countBricksInRange(world, pos);
        max_temperature = MAX_TEMPERATURE + countBricks * BRICK_TEMPERATURE_BONUS ;
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

        baseTick(world, pos, state); //启动lazytick

        if(getStack(FUEL_SLOT).getCount() > 0){
            if(!isHeating()){
                if(getStack(FUEL_SLOT).getCount()>=1){
                    getStack(FUEL_SLOT).decrement(1);;
                    startBurning();
                    if(getStack(FUEL_SLOT).getCount()==0){
                        getStack(FUEL_SLOT).setCount(0);
                    }
                } else {
                    getStack(FUEL_SLOT).setCount(0);
                }

            } else {
                temperatureRising();
                burnTime --;
            }
        } else {
            temperatureRising();
            if(isHeating())burnTime --;
        }

        if(temperature > 10){
            if(world.random.nextFloat() < 0.05f){
                world.playSound(null, pos,SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS,0.5f,1.0f);
            }
        }
        markDirty(world, pos, state);

        boolean shouldBeLit = isHeating();
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
                    double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
                    double y = pos.getY() + 0.8;
                    double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
                    world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.02, 0.0);
                }

                if (world.getRandom().nextInt(8) == 0) {
                    double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
                    double y = pos.getY() + 1.0;
                    double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.1, 0.0);
                }
            }
        }
    }

    public boolean isHeating() {
        return this.burnTime > 0;
    }
}