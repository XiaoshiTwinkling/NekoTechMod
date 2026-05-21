package com.nekotech.block.entity.machines;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.TakeFreelyInventory;
import com.nekotech.block.entity.machines.coil.CoilBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.block.Heater;
import com.nekotech.item.custom.component.IcanItemIO;
import com.nekotech.modTags.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HeaterBlockEntity extends MachineBlockEntity
        implements SidedInventory, ComponentAdaptation , TakeFreelyInventory, IHaveGoogleHUD, IcanItemIO {
    private static final int FUEL_SLOT = 0;
    private static final int MAX_BURN_TIME = 600;
    private static final float MAX_TEMPERATURE = 400.0f;
    private static final float TEMPERATURE_RISING_RATE = 0.14f;
    private static final float BRICK_TEMPERATURE_BONUS = 50.0f;  // 每个砖块增加的温度
    private static final int BRICK_CHECK_RANGE = 1;  // 检查砖块范围
    private static final int COIL_CHECK_RANGE = 2;

    private final DefaultedList<ItemStack> fuelSlots = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float temperature = 0.0f;
    private float temperature_rising_rate = TEMPERATURE_RISING_RATE;
    private float max_temperature = MAX_TEMPERATURE ;

    private static final float BRICK_HEAT_RATE_BONUS = 0.02f;

    private static final float FUEL_TIME_MULTIPLIER = 600f / 1600f;

    private int burnTime = 0;
    private int maxBurnTime = 0;

    private float brickTemperatureBonus = 0;  // 砖块提供的温度加成
    private float brickHeatRateBonus = 0;     // 砖块提供的升温速率加成
    private float coilTemperatureBonus = 0;   // 线圈提供的温度加成
    private float coilHeatRateBonus = 0;      // 线圈提供的升温速率加成

    private boolean isLit = false;

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents ;



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
        super(ModBlockEntities.HEATER, pos, state);
        validComponents= new HashSet<>();
        validComponents.add(ModItems.BRASS_ITEM_INPUTER);
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
    public boolean canInsertwithComponent(ItemStack item){
        return isFuel(item);
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
        float totalHeatInputRate = 0.0f;

        if (isBurning()) {
            totalHeatInputRate += getTemperatureRisingRate();
        }

        float heatFromCoil = calculateHeatInputFromCoil();
        totalHeatInputRate += heatFromCoil;

        if (totalHeatInputRate > 0) {
            float newTemperature = temperature + totalHeatInputRate;
            temperature = Math.min(newTemperature, getMax_temperature());
        } else {
            temperature = Math.max(temperature - 1.0f, 0.0f);
        }
    }

    private float calculateHeatInputFromCoil() {
        if (world == null) return 0.0f;

        BlockPos coilPos = pos.down();
        BlockEntity be = world.getBlockEntity(coilPos);

        if (!(be instanceof CoilBlockEntity coil)) {
            return 0.0f;
        }

        float coilTemperature = coil.getTemperature();
        boolean isCoilActive = coil.isActivelyHeating();

        if (!isCoilActive || coilTemperature <= this.temperature) {
            return 0.0f;
        }

        float temperatureDifference = coilTemperature - this.temperature;
        float heatTransferRate = temperatureDifference * 0.05f;

        float coilMaxHeatOutput = coil.getHeatRate();
        return Math.min(heatTransferRate, coilMaxHeatOutput);
    }

    @Override
    public Map<Direction, Item> getAttachedComponents() {
        return this.attachedComponents;
    }

    @Override
    public Set<Item> getValidComponents() {
        return this.validComponents;
    }

    @Override
    public boolean attachComponent(Direction side, Item component) {
        if (!canAttachComponent(side, component)) {
            return false;
        }
        attachedComponents.put(side, component);

        this.markDirty();

        if (world != null && !world.isClient()) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
        return true;
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

        // 保存安装的零件
        NbtCompound componentsNbt = new NbtCompound();
        for (Map.Entry<Direction, Item> entry : attachedComponents.entrySet()) {
            String sideName = entry.getKey().getName();
            String itemId = Registries.ITEM.getId(entry.getValue()).toString();
            componentsNbt.putString(sideName, itemId);
        }
        nbt.put("AttachedComponents", componentsNbt);

        // 保存允许的零件列表
        NbtList validList = new NbtList();
        for (Item item : validComponents) {
            validList.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        }
        nbt.put("ValidComponents", validList);
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

        // 读取安装的零件
        attachedComponents.clear();
        if (nbt.contains("AttachedComponents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound componentsNbt = nbt.getCompound("AttachedComponents");
            for (String sideName : componentsNbt.getKeys()) {
                Direction side = Direction.byName(sideName);
                String itemId = componentsNbt.getString(sideName);
                Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
                if (side != null) {
                    attachedComponents.put(side, item);
                }
            }
        }

        // 读取允许的零件列表
        validComponents.clear();
        if (nbt.contains("ValidComponents", NbtElement.LIST_TYPE)) {
            NbtList validList = nbt.getList("ValidComponents", NbtElement.STRING_TYPE);
            for (int i = 0; i < validList.size(); i++) {
                String itemId = validList.getString(i);
                Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
                validComponents.add(item);
            }
        }
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

        brickTemperatureBonus = countBricks * BRICK_TEMPERATURE_BONUS;
        brickHeatRateBonus = countBricks * BRICK_HEAT_RATE_BONUS;
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

    public void tickComponents() {
        if (world == null || world.isClient()) return;

        for (Direction side : Direction.values()) {
            Item component = getComponent(side);
            if (component != null) {
                componentTick(world, side);
            }
        }
    }

    //@Environment(EnvType.SERVER)
    public void serverTick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;

        baseTick(world, pos, state); // 启动lazytick

        tickComponents();

        float[] coilBonus = getCoilBonus();
        coilTemperatureBonus = coilBonus[0];
        coilHeatRateBonus = coilBonus[1];

        max_temperature = MAX_TEMPERATURE + brickTemperatureBonus + coilTemperatureBonus;
        temperature_rising_rate = TEMPERATURE_RISING_RATE + brickHeatRateBonus + coilHeatRateBonus;

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
                world.playSound(null, pos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
                        SoundCategory.BLOCKS, 0.5f, 1.0f);
            }
        }

        markDirty(world, pos, state);

        boolean shouldBeLit = isHeating() || (temperature > 50);
        this.isLit = shouldBeLit;

        BlockState currentState = world.getBlockState(pos);
        if (currentState.contains(Heater.LIT) &&
                currentState.get(Heater.LIT) != shouldBeLit) {
            world.setBlockState(pos, currentState.with(Heater.LIT, shouldBeLit), 3);
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

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    /**
     * 计算线圈加成
     */
    public float[] getCoilBonus() {
        if (world == null) return new float[]{0, 0};

        float tempBonus = 0;

        BlockPos checkPos = pos.down();
        BlockEntity be = world.getBlockEntity(checkPos);

        if (be instanceof CoilBlockEntity coil) {
            int[] counts = coil.getCoilCounts();
            tempBonus = counts[1] * counts[0] * 180;
        }

        return new float[]{tempBonus, 0};
    }

    public boolean isHeating() {
        return this.burnTime > 0 || coilHeatRateBonus > 0;
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

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        // 只在服务端返回数据
        if (world.isClient()) {
            return null;
        }

        List<GoogleAbstractHUD> huds = new ArrayList<>();

        List<ItemStack> items = new ArrayList<>();
        if (this instanceof Inventory inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                items.add(stack.copy()); // 复制一份，避免修改原物品
            }
        }
        Text containerTitle = Text.translatable("block.neko-technology.heater");
        ContainerHUDData containerHUD = new ContainerHUDData(pos, items, containerTitle, 1, 1);
        huds.add(containerHUD);

        Text title = Text.translatable("block.neko-technology.heater").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.heater.description",  (int) getTemperature()
                , getMax_temperature()
                , isHeating() ? Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no")
                );
        huds.add(new InfoBoxHUDData(pos, title, content));

        return huds;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        // 此方法创建用于同步方块实体数据的网络包
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        // 这个方法返回的数据会被打包进 toUpdatePacket 发送给客户端
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registries); // 复用你已有的保存逻辑
        return nbt;
    }
}