package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.block.Heater;
import com.nekotech.modTags.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class HeaterBlockEntity extends MachineBlockEntity implements SidedInventory, TakeFreelyInventory, IHaveGoogleHUD {
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

    private static final float BRICK_HEAT_RATE_BONUS = 0.02f;

    private static final float FUEL_TIME_MULTIPLIER = 600f / 1600f;

    private int burnTime = 0;
    private int maxBurnTime = 0;

    private boolean isLit = false;

    private static final Map<Item, Integer> FUEL_MAP =
            AbstractFurnaceBlockEntity.createFuelTimeMap();

    @Override
    public int getInventorySize() {
        return fuelSlots.size();
    }

    @Override
    public void markDirty() {
        super.markDirty();
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

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
        return slot == FUEL_SLOT
                && isFuel(stack)
                && !isFull();
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
        return state.isIn(ModTags.Blocks.HEATER_BRICKS);
    }

    private boolean isFuel(ItemStack stack) {
        return getFuelTime(stack) > 0;
    }

    private boolean isFull() {
        return getStack(FUEL_SLOT).getCount() >= getMaxCountPerStack();
    }

    private void startBurning(ItemStack fuelStack) {
        int vanillaFuelTime = getFuelTime(fuelStack);

        if (vanillaFuelTime > 0) {
            burnTime = maxBurnTime = (int)(vanillaFuelTime * FUEL_TIME_MULTIPLIER);
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
            if(temperature > 1){
                temperature -= 1.0F;
            } else {
                temperature = 0F;
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
    public void onItemPut(PlayerEntity player, ItemStack stack, int slot) {
        markDirty();

        if (world != null && !world.isClient) {
            if(isFuel(stack)){
                world.playSound(null, pos,
                        SoundEvents.BLOCK_CAMPFIRE_CRACKLE,
                        SoundCategory.BLOCKS,
                        0.5f, 1.0f);
            }
        }
    }

    @Override
    public void onItemTaken(PlayerEntity player, ItemStack stack, int slot) {
        markDirty();

        if (world != null && !world.isClient) {
            world.playSound(null, pos,
                    SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS,
                    0.5f, 1.0f);
        }
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {
        final int countBricks = countBricksInRange(world, pos);

        // 温度上限
        max_temperature = MAX_TEMPERATURE + countBricks * BRICK_TEMPERATURE_BONUS;

        // 升温速度
        temperature_rising_rate = TEMPERATURE_RISING_RATE + countBricks * BRICK_HEAT_RATE_BONUS;
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

        if (getStack(FUEL_SLOT).getCount() > 0) {
            if (!isHeating()) {
                ItemStack fuelStack = getStack(FUEL_SLOT);
                startBurning(fuelStack);
                fuelStack.decrement(1);
            } else {
                temperatureRising();
                burnTime--;
            }
        } else {
            temperatureRising();
            if (isHeating()) burnTime--;
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
        if (currentState.contains(Heater.LIT)) {
            world.setBlockState(pos, currentState.with(Heater.LIT, shouldBeLit));
        }

        this.markDirty();
    }

    //@Environment(EnvType.CLIENT)
    public void clientTick() {

        if (world != null) {
            BlockState state = world.getBlockState(pos);
            if (state.contains(Heater.LIT) && state.get(Heater.LIT)) {

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


    private int getFuelTime(ItemStack stack) {
        return FUEL_MAP.getOrDefault(stack.getItem(), 0);
    }

    public void onStructureChanged() {
        if (world == null || world.isClient) return;

        int countBricks = countBricksInRange(world, pos);

        max_temperature = MAX_TEMPERATURE + countBricks * BRICK_TEMPERATURE_BONUS;
        temperature_rising_rate = TEMPERATURE_RISING_RATE + countBricks * BRICK_HEAT_RATE_BONUS;

        markDirty();
    }

//    @Override
//    @Nullable
//    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
//
//        List<ItemStack> items = new ArrayList<>();
//
//        if (this instanceof Inventory inventory) {
//            for (int i = 0; i < inventory.size(); i++) {
//                ItemStack stack = inventory.getStack(i);
//                items.add(stack.copy()); // 复制一份，避免修改原物品
//            }
//        } else if (this instanceof ImplementedInventory implementedInventory) {
//            for (int i = 0; i < implementedInventory.size(); i++) {
//                ItemStack stack = implementedInventory.getStack(i);
//                items.add(stack.copy());
//            }
//        }
//
//        int columns = 1;
//        int rows = 1;
//        Text title = Text.translatable("container.heater");
//
//        return new ContainerHUDData(items, title, columns, rows);
//    }

    @Override
    public @Nullable GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
        // 只在服务端返回数据
        if (world.isClient()) {
            return null;
        }

        // 创建标题和内容
        Text title = Text.translatable("block.neko-technology.info_block");
        Text content = Text.translatable("block.neko-technology.info_block.description",
                "这是一个信息框示例，支持多行文本自动换行。\n" +
                        "第二行内容会自动换行显示。\n" +
                        "可以显示很长的文本内容，HUD高度会自动调整。");

        return new InfoBoxHUDData(pos, title, content);
    }
}