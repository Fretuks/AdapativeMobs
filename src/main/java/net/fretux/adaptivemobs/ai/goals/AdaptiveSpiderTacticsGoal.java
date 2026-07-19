package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveSpiderTacticsGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final boolean caveSpider;
    private int cooldown;
    private LivingEntity climbTarget;
    private int climbTicks;

    public AdaptiveSpiderTacticsGoal(PathfinderMob mob, IntSupplier tierSupplier, boolean caveSpider) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.caveSpider = caveSpider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        int tier = tierSupplier.getAsInt();
        int min = caveSpider ? 2 : 3;
        return tier >= min && AdaptiveAIGoalUtils.isValidAdaptiveTarget(mob.getTarget()) && --cooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return climbTicks > 0 && AdaptiveAIGoalUtils.isValidAdaptiveTarget(climbTarget) && isPillaring(climbTarget);
    }

    @Override
    public void tick() {
        if (climbTarget == null || climbTicks-- <= 0) {
            return;
        }
        climbPillar(climbTarget);
    }

    @Override
    public void start() {
        cooldown = caveSpider ? 30 + mob.getRandom().nextInt(40) : AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        climbTarget = null;
        climbTicks = 0;
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(mob.distanceToSqr(target));
        if (tier >= 3 && isPillaring(target)) {
            climbTarget = target;
            climbTicks = caveSpider ? 45 : 35;
            climbPillar(target);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "spider pillar climb");
            return;
        }
        if (tier >= 4) {
            Vec3 elevated = AdaptivePositioningUtils.elevatedPosition(mob, target, 3.0D);
            mob.getNavigation().moveTo(elevated.x, elevated.y, elevated.z, caveSpider ? 1.2D : 1.1D);
        } else if (tier >= 2) {
            Vec3 side = AdaptivePositioningUtils.sidePosition(mob, target, Math.max(2.0D, distance * 0.5D),
                    mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D);
            mob.getNavigation().moveTo(side.x, side.y, side.z, caveSpider ? 1.2D : 1.1D);
        }
        if (tier >= 3 && distance < 7.0D && AdaptiveAIGoalUtils.isUsingRangedWeapon(target) && mob.onGround()) {
            Vec3 leap = target.position().subtract(mob.position()).normalize().scale(caveSpider ? 0.42D : 0.36D);
            mob.setDeltaMovement(mob.getDeltaMovement().add(leap.x, 0.35D, leap.z));
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "spider lunge");
        }
    }

    private boolean isPillaring(LivingEntity target) {
        double vertical = target.getY() - mob.getY();
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        return vertical > 2.2D && vertical <= 10.0D && horizontal < 6.5D;
    }

    private void climbPillar(LivingEntity target) {
        Vec3 side = pillarSidePosition(target);
        mob.getLookControl().setLookAt(target, 35.0F, 35.0F);
        mob.getNavigation().moveTo(side.x, mob.getY(), side.z, caveSpider ? 1.25D : 1.15D);

        double horizontal = Math.sqrt((side.x - mob.getX()) * (side.x - mob.getX()) + (side.z - mob.getZ()) * (side.z - mob.getZ()));
        if (horizontal < 1.4D || mob.horizontalCollision) {
            Vec3 toward = new Vec3(target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
            double len = Math.max(0.001D, Math.sqrt(toward.x * toward.x + toward.z * toward.z));
            mob.setDeltaMovement(mob.getDeltaMovement().add(toward.x / len * 0.08D, caveSpider ? 0.26D : 0.22D,
                    toward.z / len * 0.08D));
            mob.hurtMarked = true;
        }
    }

    private Vec3 pillarSidePosition(LivingEntity target) {
        BlockPos targetBase = BlockPos.containing(target.getX(), mob.getY(), target.getZ());
        Vec3 away = mob.position().subtract(Vec3.atCenterOf(targetBase));
        if (Math.abs(away.x) > Math.abs(away.z)) {
            targetBase = targetBase.offset(away.x >= 0.0D ? 1 : -1, 0, 0);
        } else {
            targetBase = targetBase.offset(0, 0, away.z >= 0.0D ? 1 : -1);
        }
        return Vec3.atCenterOf(targetBase);
    }
}
