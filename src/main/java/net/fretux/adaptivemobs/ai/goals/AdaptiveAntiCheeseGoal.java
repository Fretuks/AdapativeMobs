package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveAntiCheeseGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveAntiCheeseGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AMConfig.ENABLE_ANTI_CHEESE_AI.get() || tierSupplier.getAsInt() < 3) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (isInTrapVehicle() || isNearTrapVehicle() || nearestTrapBlock() != null) {
            return true;
        }
        if (--cooldown > 0) {
            return false;
        }
        if (mob instanceof Zombie && isVerticalObstacleCheese(target)) {
            return true;
        }
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager) && !mob.hasLineOfSight(target)) {
            return true;
        }
        if (mob instanceof Creeper creeper && target.hasLineOfSight(creeper)
                && AdaptivePositioningUtils.isOpenToSky(mob)
                && Math.sqrt(mob.distanceToSqr(target)) > 4.0D) {
            return true;
        }
        if (mob instanceof EnderMan && (mob.level().getFluidState(mob.blockPosition()).is(FluidTags.WATER)
                || target.level().getFluidState(target.blockPosition()).is(FluidTags.WATER))) {
            return true;
        }
        // Nothing to correct right now; keep the cooldown short so we re-check soon instead of
        // burning a long cooldown on a tick where no cheese pattern was actually detected.
        cooldown = 10 + mob.getRandom().nextInt(15);
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (avoidTrap()) {
            cooldown = 8 + mob.getRandom().nextInt(16);
            return;
        }
        cooldown = 55 + mob.getRandom().nextInt(80);
        if (mob instanceof Zombie && isVerticalObstacleCheese(target)) {
            if (tryCorpseLadderSurge(target)) {
                return;
            }
            if (tryZombieStack(target)) {
                return;
            }
            Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 2.5D, 0.0D);
            moveIfReasonable(dest, 1.0D, "zombie pillar gather");
            return;
        }
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager) && !mob.hasLineOfSight(target)) {
            Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 7.0D, 0.0D);
            moveIfReasonable(dest, 1.0D, "ranged tiny-window reposition");
            return;
        }
        if (mob instanceof Creeper creeper && target.hasLineOfSight(creeper)
                && AdaptivePositioningUtils.isOpenToSky(mob)
                && Math.sqrt(mob.distanceToSqr(target)) > 4.0D) {
            creeper.setSwellDir(-1);
            Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 5.0D, 0.0D);
            moveIfReasonable(dest, 1.0D, "creeper open-ground hesitation");
            return;
        }
        if (mob instanceof EnderMan && (mob.level().getFluidState(mob.blockPosition()).is(FluidTags.WATER)
                || target.level().getFluidState(target.blockPosition()).is(FluidTags.WATER))) {
            Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 7.0D);
            moveIfReasonable(dest, 1.0D, "enderman water disengage");
        }
    }

    private boolean avoidTrap() {
        Entity vehicle = mob.getVehicle();
        if (vehicle instanceof Boat || vehicle instanceof AbstractMinecart) {
            mob.stopRiding();
            moveAwayFrom(vehicle.position(), 1.15D, "trap vehicle escape");
            return true;
        }
        Entity trapVehicle = nearestTrapVehicle();
        if (trapVehicle != null && mob.distanceToSqr(trapVehicle) < 2.9D) {
            moveAwayFrom(trapVehicle.position(), 1.05D, "trap vehicle avoidance");
            return true;
        }
        Vec3 trapBlock = nearestTrapBlock();
        if (trapBlock != null) {
            moveAwayFrom(trapBlock, 1.05D, "trap block avoidance");
            return true;
        }
        return false;
    }

    private boolean isInTrapVehicle() {
        Entity vehicle = mob.getVehicle();
        return vehicle instanceof Boat || vehicle instanceof AbstractMinecart;
    }

    private boolean isNearTrapVehicle() {
        Entity trapVehicle = nearestTrapVehicle();
        return trapVehicle != null && mob.distanceToSqr(trapVehicle) < 2.9D;
    }

    private Entity nearestTrapVehicle() {
        List<Entity> vehicles = mob.level().getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(2.5D),
                entity -> entity instanceof Boat || entity instanceof AbstractMinecart);
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : vehicles) {
            double dist = mob.distanceToSqr(entity);
            if (dist < best) {
                best = dist;
                nearest = entity;
            }
        }
        return nearest;
    }

    private Vec3 nearestTrapBlock() {
        Vec3 nearest = null;
        double best = Double.MAX_VALUE;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = mob.level().getBlockState(mob.blockPosition().offset(x, y, z));
                    if (!isTrapBlock(state)) {
                        continue;
                    }
                    Vec3 pos = Vec3.atCenterOf(mob.blockPosition().offset(x, y, z));
                    double dist = mob.position().distanceToSqr(pos);
                    if (dist < best) {
                        best = dist;
                        nearest = pos;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isTrapBlock(BlockState state) {
        return state.is(Blocks.COBWEB)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    private void moveAwayFrom(Vec3 threat, double speed, String debug) {
        Vec3 away = mob.position().subtract(threat);
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        if (len < 1.0E-3D) {
            away = new Vec3(mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D, 0.0D);
            len = 1.0D;
        }
        Vec3 dest = mob.position().add(away.x / len * 4.0D, 0.0D, away.z / len * 4.0D);
        moveIfReasonable(dest, speed, debug);
    }

    private boolean tryZombieStack(LivingEntity target) {
        if (mob.isPassenger() || mob.isVehicle()) {
            return false;
        }
        List<Zombie> carriers = mob.level().getEntitiesOfClass(Zombie.class, mob.getBoundingBox().inflate(2.0D),
                zombie -> zombie != mob
                        && zombie.isAlive()
                        && !zombie.isVehicle()
                        && !zombie.isPassenger()
                        && zombie.getTarget() == target
                        && Math.abs(zombie.getY() - mob.getY()) < 1.25D);
        Zombie bestCarrier = null;
        double best = Double.MAX_VALUE;
        for (Zombie carrier : carriers) {
            double dist = mob.distanceToSqr(carrier);
            if (dist < best) {
                best = dist;
                bestCarrier = carrier;
            }
        }
        if (bestCarrier == null) {
            return false;
        }
        boolean mounted = mob.startRiding(bestCarrier, true);
        if (mounted) {
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie stacking over obstacle");
        }
        return mounted;
    }

    private boolean tryCorpseLadderSurge(LivingEntity target) {
        if (tierSupplier.getAsInt() < 5 || !mob.isPassenger()) {
            return false;
        }
        double vertical = target.getY() - mob.getY();
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        if (vertical < 0.75D || vertical > 2.25D || horizontal > 2.0D) {
            return false;
        }
        List<Zombie> nearby = mob.level().getEntitiesOfClass(Zombie.class, mob.getBoundingBox().inflate(2.5D),
                zombie -> zombie.isAlive() && zombie.getTarget() == target);
        if (nearby.size() < 3) {
            return false;
        }
        mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, 0.45D, 0.0D));
        mob.hurtMarked = true;
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie corpse ladder surge");
        return true;
    }

    private boolean isVerticalObstacleCheese(LivingEntity target) {
        return isPillaring(target) || isSimpleWallBlocked(target);
    }

    private boolean isPillaring(LivingEntity target) {
        double vertical = target.getY() - mob.getY();
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        return vertical > 2.5D && horizontal < 5.5D;
    }

    private boolean isSimpleWallBlocked(LivingEntity target) {
        double vertical = Math.abs(target.getY() - mob.getY());
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        if (vertical > 2.0D || horizontal > 5.5D || mob.hasLineOfSight(target)) {
            return false;
        }
        Vec3 direction = target.position().subtract(mob.position());
        int steps = Math.max(2, (int) Math.ceil(horizontal));
        for (int i = 1; i < steps; i++) {
            double scale = i / (double) steps;
            BlockPos base = BlockPos.containing(mob.getX() + direction.x * scale, mob.getY(), mob.getZ() + direction.z * scale);
            if (isSolidObstacle(base.above()) || isSolidObstacle(base.above(2))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSolidObstacle(BlockPos pos) {
        return !mob.level().getBlockState(pos).getCollisionShape(mob.level(), pos).isEmpty();
    }

    private void moveIfReasonable(Vec3 dest, double speed, String debug) {
        if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, debug);
        }
    }
}
