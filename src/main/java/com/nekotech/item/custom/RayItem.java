package com.nekotech.item.custom;

import com.nekotech.item.api.chargeable_item.AbstractChargeableItem;
import com.nekotech.network.payload.s2c.RemoveRayPosPayload;
import com.nekotech.network.payload.s2c.SendRayPosPayload;
import com.nekotech.util.LaserTargetCache;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class RayItem extends AbstractChargeableItem {

    private static final double MAX_DISTANCE = 16.0D;

    public RayItem(Settings settings) {
        super(settings, 1200.0f);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!canUse(stack)) {
            if (!world.isClient) {
                user.sendMessage(
                        net.minecraft.text.Text.translatable("message.neko-technology.energy_depleted"),
                        true
                );
            }
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        float flux = getNekoFlux(stack) * 5;
        return Math.max(0, (int) flux);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!canUse(stack)) {
            cleanupLaser(player);
            player.stopUsingItem();
            return;
        }

        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(MAX_DISTANCE));

        BlockHitResult hit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d hitPos = hit.getPos();

            LaserTargetCache.set(player, hitPos);

            for (ServerPlayerEntity target : player.getServerWorld().getPlayers()) {
                ServerPlayNetworking.send(target,
                        new SendRayPosPayload(
                                player.getUuid(),
                                hitPos.x,
                                hitPos.y,
                                hitPos.z
                        ));
            }
        }

        consumeNekoFlux(stack, 0.2f);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof ServerPlayerEntity player) {
            cleanupLaser(player);
        }
    }

    @Override
    public boolean performAction(ItemStack stack, World world, PlayerEntity player, Hand hand) {
        return true;
    }

    @Override
    protected float getEnergyCostPerUse(ItemStack stack) {
        return 0;
    }

    private void cleanupLaser(ServerPlayerEntity player) {
        LaserTargetCache.remove(player);
        for (ServerPlayerEntity target : player.getServerWorld().getPlayers()) {
            ServerPlayNetworking.send(target, new RemoveRayPosPayload(player.getUuid()));
        }
    }
}