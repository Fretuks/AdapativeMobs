package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveAntiCheeseGoal extends Goal {

    private static final double TRAP_VEHICLE_BREAK_DISTANCE_SQR = 9.0D;
    private static final double ZOMBIE_TOWER_COORDINATION_RADIUS = 12.0D;
    // A tight radius lets body collision create a permanent crowd around the selected tower.
    // Four blocks is still local, but allows the mount handoff before the crowd deadlocks.
    private static final double ZOMBIE_MOUNT_DISTANCE_SQR = 16.0D;
    private static final double ESTABLISHED_TOWER_MOUNT_DISTANCE_SQR =
            ZOMBIE_TOWER_COORDINATION_RADIUS * ZOMBIE_TOWER_COORDINATION_RADIUS;
    private static final String ZOMBIE_CLIMBER_UNTIL_KEY = "am_zombie_climber_until";
    private static final String LADDER_CLIMBER_UNTIL_KEY = "am_ladder_climber_until";

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private LivingEntity activePillarTarget;
    private int activePillarTicks;

    public AdaptiveAntiCheeseGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AMConfig.AI_ENABLED.get() || !AMConfig.ENABLE_ADVANCED_AI.get()
                || !AMConfig.ENABLE_ANTI_CHEESE_AI.get() || tierSupplier.getAsInt() < 3) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return false;
        }
        if (mob instanceof Zombie && mob.isPassenger() && AdaptiveAIGoalUtils.isAdaptiveStackedZombie(mob)) {
            return true;
        }
        if (mob instanceof Zombie
                && mob.getPersistentData().getLong(ZOMBIE_CLIMBER_UNTIL_KEY) > mob.level().getGameTime()) {
            return false;
        }
        if (isInTrapVehicle() || nearestTrapBlock() != null || trappedAllyVehicle() != null) {
            return true;
        }
        if (mob instanceof Zombie && isVerticalObstacleCheese(target)) {
            return true;
        }
        if (--cooldown > 0) {
            return false;
        }
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager) && !mob.hasLineOfSight(target)) {
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
        if (avoidTrap()) {
            cooldown = 8 + mob.getRandom().nextInt(16);
            return;
        }
        Entity trappedAllyVehicle = trappedAllyVehicle();
        if (trappedAllyVehicle != null && tryDestroyTrapVehicle(trappedAllyVehicle)) {
            cooldown = 10 + mob.getRandom().nextInt(20);
            return;
        }
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (mob instanceof Zombie && mob.isPassenger() && AdaptiveAIGoalUtils.isAdaptiveStackedZombie(mob)) {
            activePillarTarget = target;
            activePillarTicks = 40;
            if (shouldUnstack(target)) {
                mob.stopRiding();
                AdaptiveAIGoalUtils.clearAdaptiveStackedZombie(mob);
                AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie ladder released after target fell");
            } else {
                tryCorpseLadderPressure(target);
            }
            return;
        }

        if (mob instanceof Zombie && isVerticalObstacleCheese(target)) {
            activePillarTarget = target;
            activePillarTicks = 24 + mob.getRandom().nextInt(16);
            cooldown = 6 + mob.getRandom().nextInt(8);
            if (tryCorpseLadderPressure(target)) {
                return;
            }
            if (isPillaring(target) && tryZombieStack(target)) {
                return;
            }
            pressurePillarBase(target);
            return;
        }
        cooldown = 55 + mob.getRandom().nextInt(80);
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager) && !mob.hasLineOfSight(target)) {
            Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 7.0D, 0.0D);
            moveIfReasonable(dest, 1.0D, "ranged tiny-window reposition");
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
            mob.getNavigation().stop();
            mob.stopRiding();
            moveAwayFrom(vehicle.position(), 1.15D, "trap vehicle escape");
            return true;
        }
        BlockPos trapBlock = nearestTrapBlock();
        if (trapBlock != null) {
            tryDestroyTrapBlock(trapBlock);
            moveAwayFrom(Vec3.atCenterOf(trapBlock), 1.05D, "trap block avoidance");
            return true;
        }
        return false;
    }

    private boolean isInTrapVehicle() {
        Entity vehicle = mob.getVehicle();
        return vehicle instanceof Boat || vehicle instanceof AbstractMinecart;
    }

    private Entity trappedAllyVehicle() {
        List<Entity> vehicles = mob.level().getEntitiesOfClass(Entity.class, mob.getBoundingBox().inflate(8.0D),
                entity -> isTrapVehicle(entity) && hasTrappedHostilePassenger(entity));
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity vehicle : vehicles) {
            double dist = mob.distanceToSqr(vehicle);
            if (dist < best) {
                best = dist;
                nearest = vehicle;
            }
        }
        return nearest;
    }

    private boolean hasTrappedHostilePassenger(Entity vehicle) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger != mob && passenger instanceof Mob trapped && trapped instanceof Enemy && trapped.isAlive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return activePillarTicks > 0
                && mob instanceof Zombie
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(activePillarTarget)
                && ((mob.isPassenger() && AdaptiveAIGoalUtils.isAdaptiveStackedZombie(mob))
                || isVerticalObstacleCheese(activePillarTarget));
    }

    @Override
    public void tick() {
        if (activePillarTicks-- <= 0 || !(mob instanceof Zombie)
                || !AdaptiveAIGoalUtils.isValidAdaptiveTarget(activePillarTarget)) {
            return;
        }
        mob.getLookControl().setLookAt(activePillarTarget, 30.0F, 30.0F);
        if (mob.isPassenger()) {
            tryCorpseLadderPressure(activePillarTarget);
            return;
        }
        if (!tryZombieStack(activePillarTarget)) {
            pressurePillarBase(activePillarTarget);
        }
    }

    private boolean isTrapVehicle(Entity entity) {
        return entity instanceof Boat || entity instanceof AbstractMinecart;
    }

    private boolean tryDestroyTrapVehicle(Entity vehicle) {
        if (mob.isPassenger() && mob.getVehicle() != vehicle) {
            return false;
        }
        if (mob.distanceToSqr(vehicle) > TRAP_VEHICLE_BREAK_DISTANCE_SQR) {
            mob.getNavigation().moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), 1.12D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "moving to break ally trap vehicle");
            return true;
        }
        for (Entity passenger : List.copyOf(vehicle.getPassengers())) {
            if (passenger instanceof Mob trapped && trapped instanceof Enemy) {
                trapped.stopRiding();
            }
        }
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "freeing ally from trap vehicle");
        return true;
    }

    private BlockPos nearestTrapBlock() {
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = mob.level().getBlockState(mob.blockPosition().offset(x, y, z));
                    if (!isTrapBlock(state)) {
                        continue;
                    }
                    BlockPos pos = mob.blockPosition().offset(x, y, z);
                    double dist = mob.position().distanceToSqr(Vec3.atCenterOf(pos));
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

    private void tryDestroyTrapBlock(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        if (!isTrapBlock(state)) {
            return;
        }
        if (mob instanceof Spider && state.is(Blocks.COBWEB)) {
            return;
        }
        if (mob instanceof Zombie && !AMConfig.ALLOW_ZOMBIE_BLOCK_BREAK.get()) {
            return;
        }
        if (!ForgeHooks.canEntityDestroy(mob.level(), pos, mob)) {
            return;
        }
        if (state.is(Blocks.COBWEB)) {
            Block.popResource(mob.level(), pos, new ItemStack(Items.STRING));
        } else if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            Block.popResource(mob.level(), pos, new ItemStack(Items.SWEET_BERRIES));
        }
        mob.level().destroyBlock(pos, false, mob);
        mob.getNavigation().stop();
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "breaking trap block");
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
        List<Zombie> carriers = mob.level().getEntitiesOfClass(Zombie.class,
                mob.getBoundingBox().inflate(ZOMBIE_TOWER_COORDINATION_RADIUS),
                zombie -> zombie != mob
                        && zombie.isAlive()
                        && !zombie.isVehicle()
                        && zombie.getTarget() == target
                        && zombie.getY() < target.getY()
                        && !isInVehicleChain(zombie, mob)
                        && Math.abs(zombie.getY() - mob.getY()) < 8.0D);
        Zombie bestCarrier = null;
        int bestHeight = -1;
        int bestBaseId = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie carrier : carriers) {
            int height = stackHeight(carrier);
            int baseId = stackBase(carrier).getId();
            double distance = mob.distanceToSqr(carrier);
            if (height > bestHeight
                    || (height == bestHeight && baseId < bestBaseId)
                    || (height == bestHeight && baseId == bestBaseId && distance < bestDistance)) {
                bestHeight = height;
                bestBaseId = baseId;
                bestDistance = distance;
                bestCarrier = carrier;
            }
        }
        if (bestCarrier == null) {
            return false;
        }
        double mountDistanceSqr = bestHeight > 1
                ? ESTABLISHED_TOWER_MOUNT_DISTANCE_SQR : ZOMBIE_MOUNT_DISTANCE_SQR;
        if (bestDistance > mountDistanceSqr) {
            mob.getNavigation().moveTo(bestCarrier, 1.1D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie converging on existing tower");
            return true;
        }
        boolean mounted = mob.startRiding(bestCarrier, true);
        if (mounted) {
            AdaptiveAIGoalUtils.markAdaptiveStackedZombie(mob);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie stacking over obstacle");
        }
        return mounted;
    }

    private boolean tryCorpseLadderPressure(LivingEntity target) {
        if (!(mob instanceof Zombie) || !mob.isPassenger()) {
            return false;
        }
        Entity base = stackBase(mob);
        Vec3 baseDest = new Vec3(target.getX(), base.getY(), target.getZ());
        if (base instanceof PathfinderMob baseMob) {
            baseMob.getNavigation().moveTo(baseDest.x, baseDest.y, baseDest.z, 1.05D);
        }

        double vertical = target.getY() - mob.getY();
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        if (mob.isVehicle()) {
            return true;
        }
        if (horizontal <= 3.2D && vertical <= 1.0D) {
            long now = mob.level().getGameTime();
            if (base.getPersistentData().getLong(LADDER_CLIMBER_UNTIL_KEY) > now) {
                return true;
            }
            base.getPersistentData().putLong(LADDER_CLIMBER_UNTIL_KEY, now + 60L);
            mob.stopRiding();
            AdaptiveAIGoalUtils.clearAdaptiveStackedZombie(mob);
            mob.getPersistentData().putLong(ZOMBIE_CLIMBER_UNTIL_KEY, now + 80L);
            Vec3 toward = target.position().subtract(mob.position());
            double len = Math.max(0.001D, Math.sqrt(toward.x * toward.x + toward.z * toward.z));
            mob.setDeltaMovement(toward.x / len * 0.30D, 0.34D, toward.z / len * 0.30D);
            mob.hurtMarked = true;
            mob.getNavigation().moveTo(target, 1.2D);
            activePillarTicks = 0;
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "top zombie launched from ladder");
            return true;
        }
        if (horizontal > 3.2D || vertical > 5.5D) {
            return true;
        }

        Vec3 toward = target.position().subtract(mob.position());
        double len = Math.max(0.001D, Math.sqrt(toward.x * toward.x + toward.z * toward.z));
        double lift = tierSupplier.getAsInt() >= 5 ? 0.42D : 0.32D;
        mob.setDeltaMovement(mob.getDeltaMovement().add(toward.x / len * 0.18D, lift, toward.z / len * 0.18D));
        mob.hurtMarked = true;

        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "zombie corpse ladder pressure");
        return true;
    }

    private void pressurePillarBase(LivingEntity target) {
        Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 1.4D, 0.0D);
        if (!moveIfReasonable(dest, 1.08D, "zombie pillar gather")) {
            mob.getNavigation().moveTo(target.getX(), mob.getY(), target.getZ(), 1.08D);
        }
        double horizontal = Math.sqrt((target.getX() - mob.getX()) * (target.getX() - mob.getX())
                + (target.getZ() - mob.getZ()) * (target.getZ() - mob.getZ()));
        double vertical = target.getY() - mob.getY();
        if (horizontal < 2.0D && vertical > 1.8D && mob.onGround()) {
            Vec3 toward = target.position().subtract(mob.position());
            double len = Math.max(0.001D, Math.sqrt(toward.x * toward.x + toward.z * toward.z));
            mob.setDeltaMovement(mob.getDeltaMovement().add(toward.x / len * 0.10D, 0.26D, toward.z / len * 0.10D));
            mob.hurtMarked = true;
        }
    }

    private boolean shouldUnstack(LivingEntity target) {
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            AdaptiveAIGoalUtils.clearAdaptiveStackedZombie(mob);
            return true;
        }
        Entity base = stackBase(mob);
        double verticalFromBase = target.getY() - base.getY();
        double horizontalFromBase = Math.sqrt((target.getX() - base.getX()) * (target.getX() - base.getX())
                + (target.getZ() - base.getZ()) * (target.getZ() - base.getZ()));
        // Preserve the ladder while its base approaches an elevated target. Rider-to-target
        // distance includes tower height and caused tall, otherwise valid ladders to oscillate.
        if (verticalFromBase > 2.5D
                && horizontalFromBase <= ZOMBIE_TOWER_COORDINATION_RADIUS + 4.0D) {
            return false;
        }
        AdaptiveAIGoalUtils.clearAdaptiveStackedZombie(mob);
        return true;
    }

    private int stackHeight(Entity entity) {
        int height = 1;
        Entity cursor = entity;
        while (cursor.getVehicle() instanceof Zombie) {
            height++;
            cursor = cursor.getVehicle();
        }
        return height;
    }

    private boolean isInVehicleChain(Entity start, Entity candidate) {
        Entity cursor = start;
        while (cursor != null) {
            if (cursor == candidate) {
                return true;
            }
            cursor = cursor.getVehicle();
        }
        return false;
    }

    private Entity stackBase(Entity entity) {
        Entity cursor = entity;
        while (cursor.getVehicle() instanceof Zombie) {
            cursor = cursor.getVehicle();
        }
        return cursor;
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

    private boolean moveIfReasonable(Vec3 dest, double speed, String debug) {
        if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, debug);
            return true;
        }
        return false;
    }
}
