package com.nekotech.block.entity.machines;

import com.nekotech.block.custom.CatGeneratorBlock;
import com.nekotech.block.custom.CatGeneratorPart;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IGenerator;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.CatEntity;
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
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CatGeneratorBlockEntity extends MachineBlockEntity
        implements ComponentAdaptation, IHaveGoogleHUD, IGenerator {
    private static final float MAX_NEKO_FLUX = 1000f;

    private float nekoFlux = 0f;
    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents = new HashSet<>();

    @Nullable
    private UUID runningCatUUID = null;

    private boolean lastSyncedCatRunning = false;
    private float lastSyncedTrackSpeed = 0f;


    //发电效率 = 跑步速度 * 这个参数
    private final float FLUX_RISING_RATE = 3.2f;

    public CatGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAT_GENERATOR, pos, state);
        this.validComponents.add(ModItems.BRASS_FLUX_OUTPUTER);
        this.validComponents.add(ModItems.NEKO_COPPER_FLUX_OUTPUTER);
        this.validComponents.add(ModItems.WIRE_POLE);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CatGeneratorBlockEntity blockEntity) {
        if (!world.isClient()) {
            blockEntity.tickComponents(world);

            if (blockEntity.isMainPart()) {
                blockEntity.handleCatRunning((ServerWorld) world);
            }
        }
    }


    private void handleCatRunning(ServerWorld world) {
        BlockPos leftPos = this.pos;
        BlockPos rightPos = CatGeneratorBlock.getOtherPartPos(leftPos, getCachedState());

        Box box = new Box(
                Math.min(leftPos.getX(), rightPos.getX()),
                leftPos.getY() + 1,
                Math.min(leftPos.getZ(), rightPos.getZ()),
                Math.max(leftPos.getX(), rightPos.getX()) + 1,
                leftPos.getY() + 2,
                Math.max(leftPos.getZ(), rightPos.getZ()) + 1
        );

        java.util.List<CatEntity> cats = world.getEntitiesByClass(
                CatEntity.class, box, Entity::isAlive
        );

        CatEntity runningCat = null;
        if (runningCatUUID != null) {
            for (CatEntity cat : cats) {
                if (cat.getUuid().equals(runningCatUUID)) {
                    runningCat = cat;
                    break;
                }
            }
        }

        if (runningCat == null) {
            if (runningCatUUID != null) {
                CatEntity oldCat = (CatEntity) world.getEntity(runningCatUUID);
                if (oldCat != null) {
                    ((TreadmillCat) oldCat).neko_technology$stopRunningOnTreadmill();
                }
            }
            runningCatUUID = null;
        }

        if (runningCatUUID == null && !cats.isEmpty()) {
            runningCat = cats.getFirst();
            runningCatUUID = runningCat.getUuid();
        }

        if (runningCat != null) {
            controlRunningCat(runningCat, leftPos);
            generatePower(runningCat);
            catRunning = true;
            trackSpeed = (float) runningCat.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);

            if(runningCat.isSitting()){
                runningCat.setSitting(false);
            }

        } else {
            catRunning = false;
            trackSpeed = 0f;
        }

        if (catRunning != lastSyncedCatRunning || Math.abs(trackSpeed - lastSyncedTrackSpeed) > 0.001f) {
            lastSyncedCatRunning = catRunning;
            lastSyncedTrackSpeed = trackSpeed;
            markDirty();
            sync();
        }
    }

    private void controlRunningCat(CatEntity cat, BlockPos mainPos) {
        BlockState state = this.getCachedState();
        Direction facing = state.get(CatGeneratorBlock.FACING);
        Direction front = facing.rotateClockwise(Direction.Axis.Y);

        BlockPos rightPos = CatGeneratorBlock.getOtherPartPos(mainPos, state);
        double centerX = (mainPos.getX() + rightPos.getX()) / 2.0 + 0.5;
        double centerZ = (mainPos.getZ() + rightPos.getZ()) / 2.0 + 0.5;
        Vec3d center = new Vec3d(centerX, mainPos.getY() + 0.5, centerZ);

        float targetYaw = front.asRotation();

        ((TreadmillCat) cat).neko_technology$startRunningOnTreadmill(center, targetYaw);
    }

    private void generatePower(CatEntity cat) {
        double speed = cat.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        float increment = (float) (FLUX_RISING_RATE * speed / 20.0);
        setNekoFlux(getNekoFlux() + increment);
    }

    private boolean catRunning = false;  // 猫是否在跑（同步到客户端）
    private float trackSpeed = 0f;       // 履带滚动速度（同步到客户端）

    public boolean isCatRunning() {
        CatGeneratorBlockEntity main = getMainBlockEntity();
        return main == this ? this.catRunning : main.isCatRunning();
    }

    public float getCatRunningSpeed() {
        CatGeneratorBlockEntity main = getMainBlockEntity();
        return main == this ? this.trackSpeed : main.getCatRunningSpeed();
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
        return IGenerator.super.canTransfer();
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
            nbt.putBoolean("CatRunning", catRunning);
            nbt.putFloat("TrackSpeed", trackSpeed);
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
            this.catRunning = nbt.getBoolean("CatRunning");
            this.trackSpeed = nbt.getFloat("TrackSpeed");
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

    public @Nullable UUID getRunningCatUUID() {
        return runningCatUUID;
    }

    @Override
    public void markRemoved() {
        if (this.world != null && !this.world.isClient && this.runningCatUUID != null) {
            ServerWorld sw = (ServerWorld) this.world;
            Entity e = sw.getEntity(this.runningCatUUID);
            if (e instanceof TreadmillCat tc) {
                tc.neko_technology$stopRunningOnTreadmill();
            }
            this.runningCatUUID = null;
        }
        super.markRemoved();
    }
}
