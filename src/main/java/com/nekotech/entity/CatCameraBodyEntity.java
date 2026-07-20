package com.nekotech.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CatCameraBodyEntity extends LivingEntity {
    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(CatCameraBodyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    public CatCameraBodyEntity(EntityType<? extends CatCameraBodyEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    public void copyAppearance(ServerPlayerEntity player) {
        getDataTracker().set(OWNER_UUID, Optional.of(player.getUuid()));
        setCustomName(player.getDisplayName());
        setCustomNameVisible(false);
        setPose(player.getPose());
        setYaw(player.getYaw());
        setPitch(player.getPitch());
        bodyYaw = player.bodyYaw;
        prevBodyYaw = player.prevBodyYaw;
        headYaw = player.headYaw;
        prevHeadYaw = player.prevHeadYaw;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipStack(slot, player.getEquippedStack(slot).copy());
        }
    }

    public UUID getOwnerUuid() { return getDataTracker().get(OWNER_UUID).orElse(new UUID(0L, 0L)); }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        setVelocity(0.0D, 0.0D, 0.0D);
        setNoGravity(true);
    }

    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean collidesWith(net.minecraft.entity.Entity other) { return false; }
    @Override public boolean canHit() { return false; }
    @Override public Arm getMainArm() { return Arm.RIGHT; }
    @Override public Iterable<ItemStack> getArmorItems() {
        return List.of(getEquippedStack(EquipmentSlot.FEET), getEquippedStack(EquipmentSlot.LEGS),
                getEquippedStack(EquipmentSlot.CHEST), getEquippedStack(EquipmentSlot.HEAD));
    }
    @Override public ItemStack getEquippedStack(EquipmentSlot slot) { return equipment.getOrDefault(slot, ItemStack.EMPTY); }
    @Override public void equipStack(EquipmentSlot slot, ItemStack stack) { equipment.put(slot, stack); }
}
