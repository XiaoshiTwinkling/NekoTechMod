package com.nekotech.block.entity.machines.conductor;

import com.nekotech.block.custom.CatGeneratorBlock;
import com.nekotech.block.custom.CatGeneratorPart;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IGenerator;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.MachineBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
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

public class CatGeneratorBlockEntity extends MachineBlockEntity
        implements ComponentAdaptation, IHaveGoogleHUD, IGenerator {
    private static final float MAX_NEKO_FLUX = 1000f;

    private float nekoFlux = 0f;
    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents = new HashSet<>();

    public CatGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_GENERATOR, pos, state);
        this.validComponents.add(ModItems.BRASS_FLUX_OUTPUTER);
        this.validComponents.add(ModItems.NEKO_COPPER_FLUX_OUTPUTER);
        this.validComponents.add(ModItems.WIRE_POLE);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CatGeneratorBlockEntity blockEntity) {
        if (!world.isClient()) {
            blockEntity.tickComponents(world);
        }
    }

    @Override
    public void lazytick(World world, BlockPos pos, BlockState state) {
    }

    public boolean isMainPart() {
        BlockState state = this.getCachedState();
        return state.contains(CatGeneratorBlock.PART) && state.get(CatGeneratorBlock.PART) == CatGeneratorPart.LEFT;
    }

    @Override
    public boolean canTransfer() {
        return true;
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

        this.attachedComponents.put(side, component);
        this.markDirty();
        this.sync();

        if (this.world instanceof ServerWorld serverWorld) {
            ConductorSystem.onComponentChanged(serverWorld, this.pos, side);
            serverWorld.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }

        return true;
    }

    @Override
    public void removeComponent(Direction side) {
        this.attachedComponents.remove(side);
        this.markDirty();
        this.sync();

        if (this.world instanceof ServerWorld serverWorld) {
            ConductorSystem.onComponentChanged(serverWorld, this.pos, side);
            serverWorld.updateListeners(this.pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void tickComponents(World world) {
        for (Direction side : Direction.values()) {
            if (getComponent(side) != null) {
                componentTick(world, side);
            }
        }
    }

    @Override
    public float getNekoFlux() {
        CatGeneratorBlockEntity main = getMainBlockEntity();
        return main == this ? this.nekoFlux : main.getNekoFlux();
    }

    @Override
    public void setNekoFlux(float value) {
        CatGeneratorBlockEntity main = getMainBlockEntity();
        if (main != this) {
            main.setNekoFlux(value);
            return;
        }

        this.nekoFlux = Math.max(0, Math.min(value, getMaxNekoFlux()));
        this.markDirty();
    }

    @Override
    public float getMaxNekoFlux() {
        return MAX_NEKO_FLUX;
    }

    @Override
    @Nullable
    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
            return null;
        }

        Text title = Text.translatable("block.neko-technology.cat_generator").formatted(Formatting.GOLD);
        Text content = Text.translatable("block.neko-technology.cat_generator.description",
                (int) getNekoFlux(),
                (int) getMaxNekoFlux()
        );

        return new InfoBoxHUDData(pos, title, content);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        if (isMainPart()) {
            nbt.putFloat("NekoFlux", this.nekoFlux);
        }

        NbtCompound componentsNbt = new NbtCompound();
        for (Map.Entry<Direction, Item> entry : this.attachedComponents.entrySet()) {
            componentsNbt.putString(entry.getKey().getName(), Registries.ITEM.getId(entry.getValue()).toString());
        }
        nbt.put("AttachedComponents", componentsNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (isMainPart() && nbt.contains("NekoFlux")) {
            this.nekoFlux = nbt.getFloat("NekoFlux");
        }

        this.attachedComponents.clear();
        if (nbt.contains("AttachedComponents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound componentsNbt = nbt.getCompound("AttachedComponents");
            for (String sideName : componentsNbt.getKeys()) {
                Direction side = Direction.byName(sideName);
                Identifier itemId = Identifier.tryParse(componentsNbt.getString(sideName));
                if (side != null && itemId != null) {
                    Item item = Registries.ITEM.get(itemId);
                    if (item != Items.AIR) {
                        this.attachedComponents.put(side, item);
                    }
                }
            }
        }
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

    private CatGeneratorBlockEntity getMainBlockEntity() {
        if (this.world == null || isMainPart()) {
            return this;
        }

        BlockState state = getCachedState();
        if (!state.contains(CatGeneratorBlock.PART)) {
            return this;
        }

        BlockPos mainPos = CatGeneratorBlock.getMainPos(this.pos, state);
        BlockEntity blockEntity = this.world.getBlockEntity(mainPos);
        if (blockEntity instanceof CatGeneratorBlockEntity main) {
            return main;
        }

        return this;
    }

    private void sync() {
        if (this.world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(this.pos);
        }
    }
}
