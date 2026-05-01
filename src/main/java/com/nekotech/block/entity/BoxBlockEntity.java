package com.nekotech.block.entity;

import com.nekotech.block.entity.machines.FluxStorageBlockEntity;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BoxBlockEntity extends LootableContainerBlockEntity implements IHaveGoogleHUD, ComponentAdaptation {
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents = ModItems.getAllComponents();


    public BoxBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        validComponents.add(ModItems.brass_item_inputer);
    }
    public BoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(ModBlockEntities.basic_storage_enclosure, blockPos, blockState);
        validComponents.add(ModItems.brass_item_inputer);
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
        }

        int columns = 9;
        int rows = 3;
        Text title = Text.translatable("container.box");

        return new ContainerHUDData(pos, items, title, columns, rows);
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
    public void removeComponent(Direction side) {
        attachedComponents.remove(side);
        this.markDirty();
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

    /**
     * 服务器端tick方法喵~
     * 在方块实体类型注册时需要注册tick方法喵~
     */
    public static void tick(World world, BlockPos pos, BlockState state, FluxStorageBlockEntity blockEntity) {
        if (world.isClient()) return;
        blockEntity.tickComponents();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

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
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

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
