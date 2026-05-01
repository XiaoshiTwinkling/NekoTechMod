package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.CushionBlockEntity;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.machines.api.ComponentAdaptation;
import com.nekotech.block.entity.machines.api.ICatNeedMachine;
import com.nekotech.block.entity.machines.api.IElectricalMachine;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.custom.component.AbstractComponentItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FluxStorageBlockEntity extends MachineBlockEntity
        implements IElectricalMachine, ICatNeedMachine, ComponentAdaptation, IHaveGoogleHUD {

    private float nekoFlux = 500f;
    private final float maxNekoFlux = 1000f;  // 最大存储容量

    @Nullable
    private BlockPos boundCushionPos = null;

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents = ModItems.getAllComponents();

    private boolean isActive = false;

    public FluxStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.flux_storage, pos, state);
    }

    /**
     * 服务器端tick方法喵~
     * 在方块实体类型注册时需要注册tick方法喵~
     */
    public static void tick(World world, BlockPos pos, BlockState state, FluxStorageBlockEntity blockEntity) {
        if (world.isClient()) return;

        // 检查机器是否可以运行（需要猫）
        blockEntity.isActive = blockEntity.canMachineRun();

        // 如果机器激活，执行安装的零件
        if (blockEntity.isActive) {
            blockEntity.tickComponents();
        }
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {

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

    // 执行所有安装的零件
    public void tickComponents() {
        if (world == null || world.isClient()) return;

        for (Direction side : Direction.values()) {
            Item component = getComponent(side);
            if (component != null) {
                componentTick(world, side);
            }
        }
    }

    @Override
    public void setBoundCushion(BlockPos pos) {
        this.boundCushionPos = pos;
        this.markDirty();
    }

    @Override
    @Nullable
    public CushionBlockEntity getBoundCushion() {
        if (this.world == null || this.boundCushionPos == null) {
            return null;
        }

        if (world.getBlockEntity(boundCushionPos) instanceof CushionBlockEntity cushion) {
            return cushion;
        }
        return null;
    }

    public boolean canRunWithCat() {
        return canMachineRun();
    }

    @Override
    public float getNekoFlux() {
        return this.nekoFlux;
    }

    @Override
    public void setNekoFlux(float value) {
        this.nekoFlux = Math.max(0, Math.min(value, getMaxNekoFlux()));
        this.markDirty();
    }

    @Override
    public float getMaxNekoFlux() {
        return this.maxNekoFlux;
    }

    @Override
    @Nullable
    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;  // 只在服务端返回数据喵~
        }

        // 创建HUD数据
        float currentFlux = getNekoFlux();
        float maxFlux = getMaxNekoFlux();
        float percentage = (maxFlux > 0) ? (currentFlux / maxFlux * 100) : 0;


        // 显示安装的零件
        int componentCount = attachedComponents.size();

        Text title = Text.translatable("block.neko-technology.flux_storage").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.flux_storage.description"
                ,(int) currentFlux, (int)maxFlux
                ,(int)percentage
                ,canMachineRun() ? Text.translatable("block.neko-technology.yes") : Text.translatable("block.neko-technology.no")
                ,componentCount
        );

        return new InfoBoxHUDData(pos, title, content);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        // 保存能量
        nbt.putFloat("NekoFlux", nekoFlux);

        // 保存猫垫位置
        if (boundCushionPos != null) {
            nbt.putLong("BoundCushion", boundCushionPos.asLong());
        }

        // 保存机器状态
        nbt.putBoolean("IsActive", isActive);

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

        // 读取能量
        nekoFlux = nbt.getFloat("NekoFlux");

        // 读取猫垫位置
        if (nbt.contains("BoundCushion")) {
            boundCushionPos = BlockPos.fromLong(nbt.getLong("BoundCushion"));
        } else {
            boundCushionPos = null;
        }

        // 读取机器状态
        isActive = nbt.getBoolean("IsActive");

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
