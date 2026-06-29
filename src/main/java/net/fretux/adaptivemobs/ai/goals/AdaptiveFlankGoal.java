package net.fretux.adaptivemobs.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

/**
 * A cooperative approach goal that makes a mob close on its target from an angle (flanking /
 * circling) instead of marching straight in, and — at high tiers — aim at where the target is
 * heading rather than where it currently is.
 *
 * <p>It only takes over movement when the mob is at medium range. Once the mob is close it yields
 * (canContinueToUse returns false) so the vanilla melee/attack goal resumes and actually hits.</p>
 */
public class AdaptiveFlankGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double minDist;
    private final double speed;
    private final boolean leadAtTier5;

    private int side;
    private int recalcCooldown;
    private Vec3 destination;

    public AdaptiveFlankGoal(PathfinderMob mob, IntSupplier tierSupplier, int minTier,
                             double minDist, double speed, boolean leadAtTier5) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.minTier = minTier;
        this.minDist = minDist;
        this.speed = speed;
        this.leadAtTier5 = leadAtTier5;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private double followRange() {
        return mob.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    @Override
    public boolean canUse() {
        if (tierSupplier.getAsInt() < minTier) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSqr = mob.distanceToSqr(target);
        double follow = followRange();
        return distSqr > minDist * minDist && distSqr < follow * follow;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || tierSupplier.getAsInt() < minTier) {
            return false;
        }
        double distSqr = mob.distanceToSqr(target);
        // Stop a bit before contact so the melee goal can take the final approach and the hit.
        double inner = (minDist * 0.7);
        double follow = followRange();
        return distSqr > inner * inner && distSqr < follow * follow;
    }

    @Override
    public void start() {
        // Alternate sides per-entity so a group fans out instead of stacking.
        this.side = (mob.getId() % 2 == 0) ? 1 : -1;
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.destination = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (recalcCooldown-- > 0) {
            return;
        }
        recalcCooldown = 10; // ~0.5s, avoids per-tick pathfinding

        Vec3 aim = target.position();
        if (leadAtTier5 && tierSupplier.getAsInt() >= 5) {
            // Intercept: aim where the target will be shortly.
            aim = aim.add(target.getDeltaMovement().scale(8.0));
        }

        // Rotate the (target -> mob) vector toward one side to produce an angled approach.
        Vec3 toMob = mob.position().subtract(aim);
        double len = Math.sqrt(toMob.x * toMob.x + toMob.z * toMob.z);
        if (len < 1.0E-3) {
            return;
        }
        double angle = Math.toRadians(45.0 * side);
        double nx = toMob.x * Math.cos(angle) - toMob.z * Math.sin(angle);
        double nz = toMob.x * Math.sin(angle) + toMob.z * Math.cos(angle);
        double radius = Math.max(minDist, len * 0.75);
        double scale = radius / len;

        double dx = aim.x + nx * scale;
        double dz = aim.z + nz * scale;
        this.destination = new Vec3(dx, aim.y, dz);
        mob.getNavigation().moveTo(dx, aim.y, dz, speed);
    }
}
