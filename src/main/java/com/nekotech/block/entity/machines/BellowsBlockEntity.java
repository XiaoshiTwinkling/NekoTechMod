package com.nekotech.block.entity.machines;

import com.nekotech.block.custom.DirectionalMachineBlock;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BellowsBlockEntity extends BlockEntity implements ICatNeedMachine {

    public float progress = 0.0f;
    public float lastProgress = 0.0f;
    public boolean compressing = true;

    private static final double MAX_WIND_FORCE = 0.2;
    private static final double WIND_RANGE = 5.0;
    private static final double WIND_CONE_ANGLE = Math.PI / 4;

    private boolean working = false;

    private BlockPos boundControllerPos = null;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELLOWS, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BellowsBlockEntity be) {
        float downSpeedBase = 0.08f;
        float upSpeed = 0.14f;

        if (!be.working) {
            be.progress = Math.max(0.0f, be.progress - upSpeed);
            be.compressing = true;
        } else {
            if (be.compressing) {
                float downSpeed = downSpeedBase * (1.0f - be.progress) + 0.01f;
                be.progress += downSpeed;

                if (be.progress >= 0.999f) {
                    be.progress = 1.0f;
                    be.compressing = false;
                }
            } else {
                be.progress -= upSpeed;

                if (be.progress <= 0.001f) {
                    be.progress = 0.0f;
                    be.compressing = true;
                }
            }
        }

        if (!world.isClient) {
            if (world.getTime() % 20 == 0) {
                boolean newWorking = be.canMachineRun();
                if (newWorking != be.working) {
                    be.working = newWorking;
                    be.markDirty();
                    world.updateListeners(pos, state, state, 3);
                }
            }

            if (world.getTime() % 200 == 0) {
                be.cleanupInvalidBindings();
            }

            if (be.working && be.compressing) {
                be.applyWind((ServerWorld) world, pos, state);
            }
        } else {
            be.lastProgress = be.progress;
        }
    }

    /**
     * 在鼓风机前方施加风力
     */
    private void applyWind(ServerWorld world, BlockPos pos, BlockState state) {
        if (progress < 0.2f) return;

        Direction facing = state.get(Properties.FACING);
        Vec3d origin = Vec3d.ofCenter(pos).add(
                facing.getOffsetX() * 0.51,
                0.5,
                facing.getOffsetZ() * 0.51
        );

        Vec3d windDir = Vec3d.of(facing.getVector());

        double force = MAX_WIND_FORCE * progress;

        Box searchBox = new Box(pos).expand(WIND_RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class, searchBox,
                entity -> entity.isAlive() && !entity.isSpectator()
        );

        for (LivingEntity entity : entities) {
            Vec3d entityPos = entity.getPos();
            Vec3d toEntity = entityPos.subtract(origin);
            double distance = toEntity.length();
            if (distance > WIND_RANGE) continue;

            Vec3d normalizedTo = toEntity.normalize();
            double dot = normalizedTo.dotProduct(windDir);
            if (dot < Math.cos(WIND_CONE_ANGLE)) continue;

            Vec3d target = entityPos.add(0, entity.getEyeHeight(entity.getPose()), 0);
            BlockHitResult hit = world.raycast(new RaycastContext(
                    origin, target,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    entity
            ));
            if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(entity.getBlockPos())) {
                if (!hit.getBlockPos().equals(BlockPos.ofFloored(entityPos))) {
                    continue;
                }
            }

            double attenuation = 1.0 - (distance / WIND_RANGE);
            double appliedForce = force * attenuation;

            Vec3d windVel = windDir.multiply(appliedForce);
            entity.addVelocity(windVel.x, 0, windVel.z);
            entity.velocityModified = true;
        }
    }

    public float getRenderProgress(float tickDelta) {
        return this.lastProgress + (this.progress - this.lastProgress) * tickDelta;
    }

    public boolean isWorking() {
        return this.working;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("working", working);

        if (boundControllerPos != null) {
            nbt.putInt("ControllerX", boundControllerPos.getX());
            nbt.putInt("ControllerY", boundControllerPos.getY());
            nbt.putInt("ControllerZ", boundControllerPos.getZ());
        }

        nbt.putFloat("progress", progress);
        nbt.putBoolean("compressing", compressing);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        working = nbt.getBoolean("working");

        if (nbt.contains("ControllerX")) {
            boundControllerPos = new BlockPos(
                    nbt.getInt("ControllerX"),
                    nbt.getInt("ControllerY"),
                    nbt.getInt("ControllerZ")
            );
        } else {
            boundControllerPos = null;
        }

        progress = nbt.getFloat("progress");
        compressing = nbt.getBoolean("compressing");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    @Nullable
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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

    @Override
    public void markRemoved() {
        // 通知绑定的控制器
        onMachineRemoved();

        super.markRemoved();
    }
}