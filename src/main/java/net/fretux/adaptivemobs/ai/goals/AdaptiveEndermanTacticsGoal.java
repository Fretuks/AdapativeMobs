package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveEndermanTacticsGoal extends Goal {

    private static final int ANTI_PILLAR_BUILD_COOLDOWN_MIN = 4;
    private static final int ANTI_PILLAR_BUILD_COOLDOWN_RANDOM = 4;
    private static final float MAX_MOVABLE_HARDNESS = 5.0F;
    private static final int BLOCK_SEARCH_RADIUS = 4;
    private static final double BUILD_REACH_SQR = 3.5D * 3.5D;
    private final EnderMan enderman;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private BlockPos rampTargetFeet;
    private Direction rampApproach;
    private int rampRise;
    private int rampBaseY;
    private LivingEntity activeRampTarget;
    private int activeRampTicks;

    public AdaptiveEndermanTacticsGoal(EnderMan enderman, IntSupplier tierSupplier) {
        this.enderman = enderman;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.AI_ENABLED.get() && AMConfig.ENABLE_ADVANCED_AI.get()
                && AMConfig.AI_ENDERMAN.get() && AMConfig.ENABLE_ENDERMAN_AI.get()
                && tierSupplier.getAsInt() >= 2
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(enderman.getTarget())
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        LivingEntity target = enderman.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        boolean inEnd = enderman.level().dimension() == Level.END;
        cooldown = inEnd && tier >= 5 ? 35 + enderman.getRandom().nextInt(45) : 60 + enderman.getRandom().nextInt(80);
        double dist = Math.sqrt(enderman.distanceToSqr(target));
        boolean waterNearby = enderman.level().getFluidState(enderman.blockPosition()).is(FluidTags.WATER);
        boolean strongMelee = tier >= 5 && dist < 4.0D && !AdaptiveAIGoalUtils.isUsingRangedWeapon(target);
        if (inEnd && tier >= 4) {
            double radius = tier >= 5 ? 18.0D : 12.0D;
            AdaptiveTargetingUtils.nearbyHostiles(enderman, radius,
                            other -> other instanceof EnderMan
                                    && (other.getTarget() == null || other.getTarget() == target)
                                    && other.canAttack(target))
                    .forEach(other -> other.setTarget(target));
            AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman coordinated target");
        }
        boolean toweredTarget = tier >= 4 && AMConfig.ENABLE_ANTI_CHEESE_AI.get() && isToweredTarget(target);
        if (toweredTarget) {
            cooldown = ANTI_PILLAR_BUILD_COOLDOWN_MIN + enderman.getRandom().nextInt(ANTI_PILLAR_BUILD_COOLDOWN_RANDOM);
            activeRampTarget = target;
            activeRampTicks = 160;
            // Always plan a new segment from the Enderman's current height. This matters when
            // it has just climbed a completed segment but still cannot reach the player.
            rampTargetFeet = null;
            tryPlaceMobStep(target, inEnd);
            // A tower is not a low-ceiling shelter. Never fall through into shelter dismantling
            // when a ramp is temporarily blocked or already complete.
            return;
        }
        if (tier >= 4 && AMConfig.ENABLE_ANTI_CHEESE_AI.get() && tryDismantleLowShelter(target)) {
            cooldown = 6 + enderman.getRandom().nextInt(6);
            return;
        }
        if (tier >= 3 || waterNearby || strongMelee) {
            Vec3 side = AdaptivePositioningUtils.sidePosition(enderman, target, strongMelee ? 8.0D : (inEnd && tier >= 5 ? 5.0D : 3.0D),
                    enderman.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D);
            int attempts = inEnd && tier >= 5 ? 6 : 4;
            for (int i = 0; i < attempts; i++) {
                Vec3 candidate = side.add(enderman.getRandom().nextInt(5) - 2, 0.0D, enderman.getRandom().nextInt(5) - 2);
                BlockPos pos = BlockPos.containing(candidate);
                if (!enderman.level().getFluidState(pos).is(FluidTags.WATER)
                        && AdaptivePositioningUtils.isReasonableGround(enderman, candidate)
                        && enderman.randomTeleport(candidate.x, candidate.y, candidate.z, true)) {
                    AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman angular teleport");
                    return;
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return activeRampTicks > 0
                && AMConfig.AI_ENABLED.get() && AMConfig.ENABLE_ADVANCED_AI.get()
                && AMConfig.ENABLE_ANTI_CHEESE_AI.get()
                && tierSupplier.getAsInt() >= 4
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(activeRampTarget)
                && activeRampTarget.level() == enderman.level()
                && enderman.distanceToSqr(activeRampTarget) <= 16.0D * 16.0D;
    }

    @Override
    public void tick() {
        if (activeRampTicks-- <= 0 || !AdaptiveAIGoalUtils.isValidAdaptiveTarget(activeRampTarget)) {
            return;
        }
        enderman.getLookControl().setLookAt(activeRampTarget, 30.0F, 30.0F);
        if (isCurrentRampComplete()) {
            if (isToweredTarget(activeRampTarget)) {
                // The blocks exist but the Enderman has not successfully climbed them yet.
                // Keep approaching the top instead of starting another identical ramp plan.
                if (enderman.level() instanceof ServerLevel level) {
                    approachHighestRampStep(level, rampTargetFeet);
                }
                return;
            } else {
                // The staircase has done its job. Release MOVE promptly so melee AI can attack
                // instead of holding an idle construction goal for the remainder of the timer.
                activeRampTicks = 0;
                cooldown = 5;
                enderman.getNavigation().moveTo(activeRampTarget, 1.2D);
                return;
            }
        }
        tryPlaceMobStep(activeRampTarget, enderman.level().dimension() == Level.END);
    }

    @Override
    public void stop() {
        activeRampTarget = null;
        activeRampTicks = 0;
    }

    private boolean isToweredTarget(LivingEntity target) {
        double vertical = target.getY() - enderman.getY();
        double horizontal = Math.sqrt((target.getX() - enderman.getX()) * (target.getX() - enderman.getX())
                + (target.getZ() - enderman.getZ()) * (target.getZ() - enderman.getZ()));
        return vertical > 2.5D && vertical <= 16.0D && horizontal < 9.0D;
    }

    private boolean tryPlaceMobStep(LivingEntity target, boolean inEnd) {
        if (!(enderman.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!ForgeEventFactory.getMobGriefingEvent(level, enderman)) {
            return false;
        }
        BlockState scaffoldState = enderman.getCarriedBlock();
        if (!isUsefulScaffoldBlock(scaffoldState)) {
            return tryAcquireNearbyBlock(level, target);
        }
        BlockPos targetFeet = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        if (!targetFeet.equals(rampTargetFeet)) {
            rampTargetFeet = targetFeet;
            rampApproach = nearestRampDirection(targetFeet);
            rampRise = Math.min(8, Math.max(1, (int) Math.floor(target.getY() - enderman.getY())));
            rampBaseY = targetFeet.getY() - rampRise;
        }
        for (int step = 0; step < rampRise; step++) {
            int distance = Math.max(1, rampRise - step);
            BlockPos pos = new BlockPos(targetFeet.getX(), rampBaseY + step, targetFeet.getZ()).relative(rampApproach, distance);
            if (canPlaceRampStep(level, pos, rampApproach, step == 0)) {
                if (enderman.distanceToSqr(Vec3.atCenterOf(pos)) > BUILD_REACH_SQR) {
                    moveToBuildPosition(pos);
                    return true;
                }
                BlockSnapshot replaced = BlockSnapshot.create(level.dimension(), level, pos);
                if (ForgeEventFactory.onBlockPlace(enderman, replaced, Direction.UP)) {
                    continue;
                }
                level.setBlock(pos, scaffoldState, 3);
                level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(enderman, scaffoldState));
                enderman.setCarriedBlock(null);
                enderman.randomTeleport(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, true);
                AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman placed anti-pillar scaffold");
                return true;
            }
        }
        return approachHighestRampStep(level, targetFeet);
    }

    private boolean approachHighestRampStep(ServerLevel level, BlockPos targetFeet) {
        for (int distance = 1; distance <= rampRise; distance++) {
            BlockPos step = new BlockPos(targetFeet.getX(), targetFeet.getY() - distance, targetFeet.getZ())
                    .relative(rampApproach, distance);
            if (!level.getBlockState(step).getCollisionShape(level, step).isEmpty()) {
                moveToBuildPosition(step);
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentRampComplete() {
        if (!(enderman.level() instanceof ServerLevel level) || rampTargetFeet == null
                || rampApproach == null || rampRise <= 0) {
            return false;
        }
        for (int distance = 1; distance <= rampRise; distance++) {
            BlockPos step = new BlockPos(rampTargetFeet.getX(), rampTargetFeet.getY() - distance,
                    rampTargetFeet.getZ()).relative(rampApproach, distance);
            if (level.getBlockState(step).isAir()) {
                return false;
            }
        }
        return true;
    }

    private void moveToBuildPosition(BlockPos step) {
        BlockPos standing = step.relative(rampApproach);
        Vec3 destination = Vec3.atBottomCenterOf(standing);
        if (!AdaptivePositioningUtils.isReasonableGround(enderman, destination)) {
            destination = Vec3.atBottomCenterOf(step);
        }
        enderman.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.18D);
        AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman approaching staircase work area");
    }

    private boolean isUsefulScaffoldBlock(BlockState state) {
        return state != null && !state.liquid()
                && !state.getCollisionShape(enderman.level(), enderman.blockPosition()).isEmpty();
    }

    private boolean tryAcquireNearbyBlock(ServerLevel level, LivingEntity target) {
        BlockPos origin = enderman.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = -2; y <= 2; y++) {
            for (int x = -BLOCK_SEARCH_RADIUS; x <= BLOCK_SEARCH_RADIUS; x++) {
                for (int z = -BLOCK_SEARCH_RADIUS; z <= BLOCK_SEARCH_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (Math.abs(candidate.getX() - origin.getX()) <= 1
                            && Math.abs(candidate.getZ() - origin.getZ()) <= 1
                            && candidate.getY() <= origin.getY()) {
                        continue;
                    }
                    if (Math.abs(candidate.getX() - target.getBlockX()) <= 1
                            && Math.abs(candidate.getZ() - target.getBlockZ()) <= 1) {
                        continue;
                    }
                    if (isReservedRampBlock(candidate, target.blockPosition())) {
                        continue;
                    }
                    if (!isMovableBlock(level, candidate)) {
                        continue;
                    }
                    double distance = candidate.distSqr(origin);
                    if (distance < bestDistance) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        if (best == null) {
            return false;
        }
        return pickUpBlock(level, best, "enderman acquired scaffold block");
    }

    private boolean isReservedRampBlock(BlockPos candidate, BlockPos targetFeet) {
        int dx = candidate.getX() - targetFeet.getX();
        int dz = candidate.getZ() - targetFeet.getZ();
        int distance = Math.abs(dx) + Math.abs(dz);
        boolean cardinal = (dx == 0) != (dz == 0);
        return cardinal && distance >= 1 && distance <= 8
                && candidate.getY() == targetFeet.getY() - distance;
    }

    private boolean tryDismantleLowShelter(LivingEntity target) {
        if (!(enderman.level() instanceof ServerLevel level)
                || !ForgeEventFactory.getMobGriefingEvent(level, enderman)
                || enderman.distanceToSqr(target) > 8.0D * 8.0D) {
            return false;
        }
        BlockPos feet = target.blockPosition();
        // A player standing in a two-block-high opening has a solid ceiling two blocks above
        // their feet. Check that ceiling and its four neighbours so Endermen can open a route
        // without indiscriminately tearing through the rest of the structure.
        Direction[] offsets = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction offset : offsets) {
            BlockPos candidate = offset == Direction.DOWN ? feet.above(2) : feet.above(2).relative(offset);
            if (!isMovableBlock(level, candidate)) {
                continue;
            }
            if (enderman.getCarriedBlock() != null && !tryDepositCarriedBlock(level)) {
                return false;
            }
            if (pickUpBlock(level, candidate, "enderman dismantled low shelter")) {
                enderman.randomTeleport(candidate.getX() + 0.5D, feet.getY(), candidate.getZ() + 0.5D, true);
                return true;
            }
        }
        return false;
    }

    private boolean tryDepositCarriedBlock(ServerLevel level) {
        BlockState carried = enderman.getCarriedBlock();
        if (!isUsefulScaffoldBlock(carried)) {
            enderman.setCarriedBlock(null);
            return true;
        }
        BlockPos origin = enderman.blockPosition();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = origin.relative(direction, 2);
            if (!canPlaceRampStep(level, pos, direction.getOpposite(), true)) {
                continue;
            }
            BlockSnapshot replaced = BlockSnapshot.create(level.dimension(), level, pos);
            if (ForgeEventFactory.onBlockPlace(enderman, replaced, Direction.UP)) {
                continue;
            }
            level.setBlock(pos, carried, 3);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(enderman, carried));
            enderman.setCarriedBlock(null);
            return true;
        }
        return false;
    }

    private boolean pickUpBlock(ServerLevel level, BlockPos pos, String debugMessage) {
        if (enderman.getCarriedBlock() != null || !isMovableBlock(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!level.removeBlock(pos, false)) {
            return false;
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(enderman, state));
        enderman.setCarriedBlock(state);
        AdaptiveAIGoalUtils.debug(enderman, tierSupplier, debugMessage);
        return true;
    }

    private boolean isMovableBlock(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        return !state.isAir() && !state.liquid()
                && hardness >= 0.0F && hardness <= MAX_MOVABLE_HARDNESS
                && state.isCollisionShapeFullBlock(level, pos)
                && !isProtectedBlock(state.getBlock());
    }

    private boolean isProtectedBlock(Block block) {
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.NETHERITE_BLOCK || block == Blocks.REINFORCED_DEEPSLATE
                || block == Blocks.END_PORTAL_FRAME || block == Blocks.END_PORTAL
                || block == Blocks.NETHER_PORTAL || block == Blocks.BARRIER
                || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.JIGSAW || block == Blocks.MOVING_PISTON;
    }

    private Direction nearestRampDirection(BlockPos targetFeet) {
        double dx = enderman.getX() - (targetFeet.getX() + 0.5D);
        double dz = enderman.getZ() - (targetFeet.getZ() + 0.5D);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private boolean canPlaceRampStep(ServerLevel level, BlockPos pos, Direction approach, boolean firstStep) {
        return level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).isAir()
                && !level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos).is(FluidTags.LAVA)
                && (firstStep ? hasGroundSupport(level, pos) : hasPreviousRampStep(level, pos, approach));
    }

    private boolean hasPreviousRampStep(ServerLevel level, BlockPos pos, Direction approach) {
        BlockPos previousStep = pos.relative(approach).below();
        return !level.getBlockState(previousStep).getCollisionShape(level, previousStep).isEmpty();
    }

    private boolean hasGroundSupport(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }
}
