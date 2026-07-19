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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveEndermanTacticsGoal extends Goal {

    private static final int ANTI_PILLAR_BUILD_COOLDOWN_MIN = 4;
    private static final int ANTI_PILLAR_BUILD_COOLDOWN_RANDOM = 4;
    private final EnderMan enderman;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private BlockPos rampTargetFeet;
    private Direction rampApproach;
    private int rampRise;
    private int rampBaseY;

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
        if (tier >= 4 && AMConfig.ENABLE_ANTI_CHEESE_AI.get() && isToweredTarget(target)) {
            cooldown = ANTI_PILLAR_BUILD_COOLDOWN_MIN + enderman.getRandom().nextInt(ANTI_PILLAR_BUILD_COOLDOWN_RANDOM);
            if (tryPlaceMobStep(target, inEnd)) {
                return;
            }
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
            return false;
        }
        BlockPos targetFeet = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        if (!targetFeet.equals(rampTargetFeet)) {
            rampTargetFeet = targetFeet;
            rampApproach = sharedRampDirection(targetFeet);
            rampRise = Math.min(8, Math.max(1, (int) Math.floor(target.getY() - enderman.getY())));
            rampBaseY = targetFeet.getY() - rampRise;
        }
        for (int step = 0; step < rampRise; step++) {
            int distance = Math.max(1, rampRise - step);
            BlockPos pos = new BlockPos(targetFeet.getX(), rampBaseY + step, targetFeet.getZ()).relative(rampApproach, distance);
            if (canPlaceRampStep(level, pos, rampApproach, step == 0)) {
                level.setBlock(pos, scaffoldState, 3);
                enderman.setCarriedBlock(null);
                enderman.randomTeleport(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, true);
                AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman placed anti-pillar scaffold");
                return true;
            }
        }
        return false;
    }

    private boolean isUsefulScaffoldBlock(BlockState state) {
        return state != null && !state.liquid()
                && !state.getCollisionShape(enderman.level(), enderman.blockPosition()).isEmpty();
    }

    private Direction sharedRampDirection(BlockPos targetFeet) {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int index = Math.floorMod(targetFeet.getX() * 31 + targetFeet.getZ() * 17, directions.length);
        return directions[index];
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
