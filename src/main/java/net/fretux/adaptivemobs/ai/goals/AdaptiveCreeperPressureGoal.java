package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveCreeperPressureGoal extends Goal {

    private final Creeper creeper;
    private final IntSupplier tierSupplier;
    private int pathCheckCooldown;
    private boolean targetReachable;

    public AdaptiveCreeperPressureGoal(Creeper creeper, IntSupplier tierSupplier) {
        this.creeper = creeper;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = creeper.getTarget();
        return tierSupplier.getAsInt() >= 3 && AdaptiveAIGoalUtils.isValidAdaptiveTarget(target);
    }

    @Override
    public void tick() {
        LivingEntity target = creeper.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double startSwell = tier >= 5 ? 5.0D : 4.0D;
        double stopSwell = tier >= 4 ? 7.0D : 6.0D;
        double dist = Math.sqrt(creeper.distanceToSqr(target));
        creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);
        updateReachability(target);

        if (avoidTrapOnApproach(target)) {
            creeper.setSwellDir(-1);
            return;
        }

        if (!targetReachable) {
            keepDistance(target, dist);
            return;
        }

        if (AdaptiveAIGoalUtils.isBlockingShield(target) && dist <= stopSwell) {
            creeper.setSwellDir(-1);
            Vec3 dest = behindTarget(target);
            if (!AdaptivePositioningUtils.isReasonableGround(creeper, dest)) {
                dest = AdaptivePositioningUtils.sidePosition(creeper, target, Math.max(2.8D, dist),
                        creeper.getRandom().nextBoolean() ? 1.0D : -1.0D, tier >= 5 ? 0.25D : 0.0D);
            }
            moveIfReasonable(dest, 1.12D, "creeper shield backstep");
            return;
        }
        if (dist <= startSwell || (tier >= 4 && target.getLastHurtMob() != null && dist <= stopSwell)) {
            creeper.setSwellDir(1);
            creeper.getNavigation().stop();
        } else {
            creeper.setSwellDir(-1);
            creeper.getNavigation().moveTo(target, tier >= 5 ? 1.12D : 1.05D);
        }
    }

    private void updateReachability(LivingEntity target) {
        if (pathCheckCooldown-- > 0) {
            return;
        }
        pathCheckCooldown = 15 + creeper.getRandom().nextInt(10);
        Path path = creeper.getNavigation().createPath(target, 0);
        targetReachable = path != null && path.canReach();
    }

    private void keepDistance(LivingEntity target, double dist) {
        creeper.setSwellDir(-1);
        double desired = 8.0D;
        if (dist < desired) {
            Vec3 dest = AdaptivePositioningUtils.retreatPosition(creeper, target, desired - dist + 3.0D);
            if (!moveIfReasonable(dest, 1.05D, "creeper unreachable spacing")) {
                Vec3 side = AdaptivePositioningUtils.sidePosition(creeper, target, desired, 1.0D, 0.0D);
                moveIfReasonable(side, 1.0D, "creeper unreachable side spacing");
            }
        } else {
            creeper.getNavigation().stop();
        }
    }

    private Vec3 behindTarget(LivingEntity target) {
        Vec3 look = target.getLookAngle();
        double len = Math.sqrt(look.x * look.x + look.z * look.z);
        if (len < 1.0E-3D) {
            return target.position();
        }
        return target.position().subtract(look.x / len * 1.8D, 0.0D, look.z / len * 1.8D);
    }

    private boolean avoidTrapOnApproach(LivingEntity target) {
        Entity trapVehicle = nearestTrapVehicle();
        if (trapVehicle != null && creeper.distanceToSqr(trapVehicle) < 9.0D) {
            return moveAwayFrom(trapVehicle.position(), 1.1D, "creeper trap vehicle avoidance");
        }
        Vec3 trapBlock = nearestTrapBlock();
        if (trapBlock != null && creeper.position().distanceToSqr(trapBlock) < 6.25D) {
            return moveAwayFrom(trapBlock, 1.1D, "creeper trap block avoidance");
        }
        Vec3 toTarget = target.position().subtract(creeper.position());
        double len = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (len < 1.0E-3D) {
            return false;
        }
        Vec3 ahead = creeper.position().add(toTarget.x / len * 2.0D, 0.0D, toTarget.z / len * 2.0D);
        BlockState aheadState = creeper.level().getBlockState(BlockPos.containing(ahead));
        return isTrapBlock(aheadState) && moveAwayFrom(ahead, 1.1D, "creeper approach trap avoidance");
    }

    private Entity nearestTrapVehicle() {
        List<Entity> vehicles = creeper.level().getEntitiesOfClass(Entity.class, creeper.getBoundingBox().inflate(2.8D),
                entity -> entity instanceof Boat || entity instanceof AbstractMinecart);
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : vehicles) {
            double dist = creeper.distanceToSqr(entity);
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
        BlockPos origin = creeper.blockPosition();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!isTrapBlock(creeper.level().getBlockState(pos))) {
                        continue;
                    }
                    Vec3 center = Vec3.atCenterOf(pos);
                    double dist = creeper.position().distanceToSqr(center);
                    if (dist < best) {
                        best = dist;
                        nearest = center;
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

    private boolean moveAwayFrom(Vec3 threat, double speed, String debug) {
        Vec3 away = creeper.position().subtract(threat);
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        if (len < 1.0E-3D) {
            away = new Vec3(creeper.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D, 0.0D);
            len = 1.0D;
        }
        Vec3 dest = creeper.position().add(away.x / len * 4.0D, 0.0D, away.z / len * 4.0D);
        return moveIfReasonable(dest, speed, debug);
    }

    private boolean moveIfReasonable(Vec3 dest, double speed, String debug) {
        if (AdaptivePositioningUtils.isReasonableGround(creeper, dest)) {
            creeper.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(creeper, tierSupplier, debug);
            return true;
        }
        return false;
    }
}
