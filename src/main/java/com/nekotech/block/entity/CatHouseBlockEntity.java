package com.nekotech.block.entity;

import com.nekotech.block.entity.api.ICatControlBlock;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.custom.CatBoxItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CatHouseBlockEntity extends BlockEntity implements ICatControlBlock, IHaveGoogleHUD {
    private static final int MAX_MACHINES = 4;
    private static final String CONTROLLER_TYPE = "cat_house";

    // 存储猫的NBT数据
    private NbtCompound storedCatData = null;
    private final String controllerId;

    private final List<BlockPos> controlledMachines = new ArrayList<>();

    public CatHouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_HOUSE, pos, state);
        this.controllerId = "cat_house_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String getControllerId() {
        return controllerId;
    }

    @Override
    public boolean isControllerActive() {
        return hasCatStored();
    }

    @Override
    public int getMaxControlledMachines() {
        return MAX_MACHINES;
    }

    @Override
    public boolean canControlMachine(World world, BlockPos machinePos, BlockState machineState) {
        BlockEntity be = world.getBlockEntity(machinePos);
        return be instanceof ICatNeedMachine;
    }

    @Override
    public void onControllerStateChanged(World world, BlockPos pos, BlockState state, boolean newState) {
        for (BlockPos machinePos : controlledMachines) {
            BlockEntity be = world.getBlockEntity(machinePos);
            if (be instanceof ICatNeedMachine machine) {
                // 可以在这里添加状态变化通知
            }
        }
    }

    @Override
    public List<BlockPos> getControlledMachines() {
        return new ArrayList<>(controlledMachines);
    }

    @Override
    public boolean registerMachine(BlockPos machinePos) {
        if (controlledMachines.contains(machinePos)) {
            return true;
        }

        if (controlledMachines.size() >= MAX_MACHINES) {
            return false;
        }

        controlledMachines.add(machinePos);
        markDirty();
        return true;
    }

    @Override
    public boolean unregisterMachine(BlockPos machinePos) {
        boolean removed = controlledMachines.remove(machinePos);
        if (removed) {
            markDirty();
        }
        return removed;
    }

    /**
     * 立即清理指定位置的机器注册喵~
     * 当机器被破坏时调用喵~
     */
    public void unregisterMachineImmediately(BlockPos machinePos) {
        boolean removed = controlledMachines.remove(machinePos);
        if (removed) {
            markDirty();

            // 播放音效或粒子效果
            if (world != null && !world.isClient) {
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP,
                        net.minecraft.sound.SoundCategory.BLOCKS, 0.3f, 1.5f);
            }
        }
    }

    @Override
    public boolean isMachineRegistered(BlockPos machinePos) {
        return controlledMachines.contains(machinePos);
    }

    @Override
    public void cleanupInvalidMachines() {
        if (world == null || world.isClient) return;

        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos machinePos : controlledMachines) {
            BlockEntity be = world.getBlockEntity(machinePos);
            if (be == null || be.isRemoved() || !(be instanceof ICatNeedMachine)) {
                toRemove.add(machinePos);
            }
        }

        for (BlockPos machinePos : toRemove) {
            controlledMachines.remove(machinePos);
        }

        if (!toRemove.isEmpty()) {
            markDirty();
        }
    }

    @Override
    public String getControllerType() {
        return CONTROLLER_TYPE;
    }

    /**
     * 玩家与猫舍交互
     * @return 是否成功交互
     */
    public boolean interact(PlayerEntity player, ItemStack stack, Hand hand) {
        if (world == null || world.isClient) return false;

        if (hasCatStored()) {
            // 猫舍有猫，尝试取出
            return tryTakeOutCat(player, stack, hand);
        } else {
            // 猫舍没猫，尝试放入
            return tryPutInCat(player, stack, hand);
        }
    }

    /**
     * 尝试从猫舍取出猫
     */
    private boolean tryTakeOutCat(PlayerEntity player, ItemStack stack, Hand hand) {
        if (!stack.isEmpty()) {
            return false;
        }

        if (storedCatData == null) {
            return false;
        }

        ItemStack newCatBox = new ItemStack(ModItems.NEKO_BOX, 1);
        CatBoxItem.saveCatData(newCatBox, storedCatData.copy());

        if (!player.getInventory().insertStack(newCatBox)) {
            player.sendMessage(Text.translatable("block.neko-technology.cat_house.inventory_full"), true);
            return false;
        }

        storedCatData = null;
        markDirty();
        onControllerStateChanged(world, pos, getCachedState(), false);

        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_CAT_AMBIENT,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.0f);
        player.sendMessage(Text.translatable("block.neko-technology.cat_house.cat_taken"), true);
        return true;
    }

    /**
     * 尝试将猫放入猫舍
     */
    private boolean tryPutInCat(PlayerEntity player, ItemStack stack, Hand hand) {
        if (!(stack.getItem() instanceof CatBoxItem)) {
            return false;
        }

        Optional<NbtCompound> catData = CatBoxItem.getCatData(stack);
        if (catData.isEmpty()) {
            player.sendMessage(Text.translatable("block.neko-technology.cat_house.box_empty"), true);
            return false;
        }

        storedCatData = catData.get().copy();
        markDirty();

        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        onControllerStateChanged(world, pos, getCachedState(), true);

        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_CAT_PURR,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.0f);
        player.sendMessage(Text.translatable("block.neko-technology.cat_house.cat_stored"), true);
        return true;
    }

    /**
     * 检查猫舍中是否有猫
     */
    public boolean hasCatStored() {
        return storedCatData != null;
    }

    /**
     * 获取存储的猫的NBT数据
     */
    public NbtCompound getStoredCatData() {
        return storedCatData;
    }

    /**
     * 定期清理无效的机器注册
     */
    public static void tick(World world, BlockPos pos, BlockState state, CatHouseBlockEntity be) {
        if (world.isClient) return;

        if (world.getTime() % 40 == 0) {
            be.cleanupInvalidMachines();
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putString("ControllerId", controllerId);

        if (storedCatData != null) {
            nbt.put("StoredCat", storedCatData);
        }

        NbtList machineList = new NbtList();
        for (BlockPos pos : controlledMachines) {
            NbtCompound posNbt = new NbtCompound();
            posNbt.putInt("X", pos.getX());
            posNbt.putInt("Y", pos.getY());
            posNbt.putInt("Z", pos.getZ());
            machineList.add(posNbt);
        }
        nbt.put("ControlledMachines", machineList);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (nbt.contains("StoredCat", NbtElement.COMPOUND_TYPE)) {
            storedCatData = nbt.getCompound("StoredCat");
        } else {
            storedCatData = null;
        }

        controlledMachines.clear();
        NbtList machineList = nbt.getList("ControlledMachines", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < machineList.size(); i++) {
            NbtCompound posNbt = machineList.getCompound(i);
            BlockPos pos = new BlockPos(
                    posNbt.getInt("X"),
                    posNbt.getInt("Y"),
                    posNbt.getInt("Z")
            );
            controlledMachines.add(pos);
        }
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;
        }

        List<GoogleAbstractHUD> huds = new ArrayList<>();

        Text title = Text.translatable("block.neko-technology.cat_house").formatted(Formatting.GOLD);

        // 获取猫的名字，如果有的话
        String catName = null;
        if (storedCatData != null && storedCatData.contains("CustomName", NbtElement.STRING_TYPE)) {
            catName = storedCatData.getString("CustomName");
        }

        Text content = Text.translatable("block.neko-technology.cat_house.description",
                hasCatStored() ?
                        (catName != null ? Text.literal(catName) : Text.translatable("block.neko-technology.cat_house.unnamed_cat"))
                        : Text.translatable("block.neko-technology.no"),
                controlledMachines.size(),
                MAX_MACHINES
        );

        huds.add(new InfoBoxHUDData(pos, title, content));
        return huds;
    }
}