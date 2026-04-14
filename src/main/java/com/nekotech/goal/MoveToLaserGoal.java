package com.nekotech.goal;

import com.nekotech.util.LaserTargetCache;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class MoveToLaserGoal extends Goal {

    private final PathAwareEntity mob;
    private final double speed;
    private int repathCooldown;

    private Vec3d target;

    public MoveToLaserGoal(PathAwareEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (LaserTargetCache.isEmpty()) {
            return false;
        }

        Vec3d pos = LaserTargetCache.getNearest(mob.getPos(), 4.0);

        if (pos == null) {
            return false;
        }

        this.target = pos;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return target != null && LaserTargetCache.contains(target);
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        if (target == null) return;

        double dx = target.x - mob.getX();
        double dz = target.z - mob.getZ();

        float targetYaw = (float)(MathHelper.atan2(dz, dx) * 57.295776) - 90.0F;

        float newYaw = MathHelper.stepUnwrappedAngleTowards(
                mob.getYaw(),
                targetYaw,
                10.0F
        );

        mob.setYaw(newYaw);
        mob.bodyYaw = newYaw;

        mob.getLookControl().lookAt(
                target.x,
                target.y,
                target.z,
                30.0F,
                30.0F
        );

        if (--repathCooldown <= 0) {
            repathCooldown = 10;
            move();
        }
    }

    private void move() {
        mob.getNavigation().startMovingTo(
                target.x,
                target.y,
                target.z,
                speed
        );
    }
}