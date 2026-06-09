package com.nekotech.block.entity.machines.conductor;

import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.electrical.IElectricStorager;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.machines.MachineBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FluxStorageBlockEntity extends MachineBlockEntity
        implements  ICatNeedMachine, ComponentAdaptation, IHaveGoogleHUD, IElectricStorager {

    private float nekoFlux = 500f;
    private final float maxNekoFlux = 1000f;

    @Nullable
    private BlockPos boundControllerPos = null;

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents ;

    private boolean isActive = false;

    public FluxStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_STORAGE, pos, state);
        validComponents= new HashSet<>();
        validComponents.add(ModItems.BRASS_FLUX_OUTPUTER);
        validComponents.add(ModItems.BRASS_FLUX_INPUTER);
        validComponents.add(ModItems.NEKO_COPPER_FLUX_INPUTER);
        validComponents.add(ModItems.NEKO_COPPER_FLUX_OUTPUTER);
        validComponents.add(ModItems.WIRE_POLE);
    }

    public static void tick(World world, BlockPos pos, BlockState state, FluxStorageBlockEntity blockEntity) {
        if (!world.isClient()) {
            blockEntity.serverTick(world, pos, state, blockEntity);
        }
    }

    private void serverTick(World world, BlockPos pos, BlockState state, FluxStorageBlockEntity blockEntity){
        blockEntity.isActive = blockEntity.canMachineRun();

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
            return null;
        }

        float currentFlux = getNekoFlux();
        float maxFlux = getMaxNekoFlux();
        float percentage = (maxFlux > 0) ? (currentFlux / maxFlux * 100) : 0;

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

        nbt.putFloat("NekoFlux", nekoFlux);

        if (boundControllerPos != null) {
            nbt.putInt("ControllerX", boundControllerPos.getX());
            nbt.putInt("ControllerY", boundControllerPos.getY());
            nbt.putInt("ControllerZ", boundControllerPos.getZ());
        }

        nbt.putBoolean("IsActive", isActive);

        NbtCompound componentsNbt = new NbtCompound();
        for (Map.Entry<Direction, Item> entry : attachedComponents.entrySet()) {
            String sideName = entry.getKey().getName();
            String itemId = Registries.ITEM.getId(entry.getValue()).toString();
            componentsNbt.putString(sideName, itemId);
        }
        nbt.put("AttachedComponents", componentsNbt);

        NbtList validList = new NbtList();
        for (Item item : validComponents) {
            validList.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        }
        nbt.put("ValidComponents", validList);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        nekoFlux = nbt.getFloat("NekoFlux");

        if (nbt.contains("ControllerX")) {
            boundControllerPos = new BlockPos(
                    nbt.getInt("ControllerX"),
                    nbt.getInt("ControllerY"),
                    nbt.getInt("ControllerZ")
            );
        } else if (nbt.contains("BoundCushion")) {
            boundControllerPos = BlockPos.fromLong(nbt.getLong("BoundCushion"));
        } else {
            boundControllerPos = null;
        }

        isActive = nbt.getBoolean("IsActive");

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
    public @Nullable BlockPos getBoundControllerPos() {
        return boundControllerPos;
    }

    @Override
    public void setBoundControllerPos(@Nullable BlockPos pos) {
        this.boundControllerPos = pos;
        this.markDirty();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registries);
        return nbt;
    }

    @Override
    public void markRemoved() {
        // 通知绑定的控制器
        onMachineRemoved();

        super.markRemoved();
    }
}