package com.nekotech.item.custom;

import com.nekotech.item.ModItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CatnipItem extends ModItem {
    public CatnipItem(Settings settings, String tooltipTranslationKey) {
        super(settings, tooltipTranslationKey);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // 猫薄荷只能对猫使用，不能自己吃
        return new TypedActionResult<>(ActionResult.PASS, stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof CatEntity cat) {
            World world = cat.getWorld();

            if (!world.isClient) {
                applyCatnipEffect(cat, user);

                if (!user.getAbilities().creativeMode) {
                    stack.decrement(1);
                }

                world.playSound(null, cat.getX(), cat.getY(), cat.getZ(),
                        SoundEvents.ENTITY_CAT_EAT, cat.getSoundCategory(), 1.0F, 1.0F);

                // 生成粒子效果
                spawnParticles(world, cat);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private void applyCatnipEffect(CatEntity cat, PlayerEntity user) {
        World world = cat.getWorld();

        cat.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, 200, 0, false, true, true));

        double speedMultiplier = 2.0; // 冲刺速度

        float yaw = cat.getYaw();
        float pitch = cat.getPitch();

        double motionX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double motionZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double motionY = -Math.sin(Math.toRadians(pitch));

        cat.addVelocity(motionX * speedMultiplier, motionY * speedMultiplier, motionZ * speedMultiplier);

        cat.fallDistance = 0.0F;

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.scheduleBlockTick(cat.getBlockPos(), cat.getBlockStateAtPos().getBlock(), 200);
        }
    }

    private void spawnParticles(World world, CatEntity cat) {
        if (world instanceof ServerWorld serverWorld) {

            for (int i = 0; i < 15; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
                double offsetY = world.random.nextDouble() * 0.5;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;

                serverWorld.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        cat.getX() + offsetX,
                        cat.getY() + offsetY + 0.5,
                        cat.getZ() + offsetZ,
                        1, 0, 0, 0, 0
                );
            }
        }
    }
}
