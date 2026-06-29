package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveEndermanTacticsGoal extends Goal {

    private final EnderMan enderman;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveEndermanTacticsGoal(EnderMan enderman, IntSupplier tierSupplier) {
        this.enderman = enderman;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_ENDERMAN_AI.get()
                && tierSupplier.getAsInt() >= 2
                && enderman.getTarget() != null
                && enderman.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        LivingEntity target = enderman.getTarget();
        if (target == null) {
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
                            other -> other instanceof EnderMan && (other.getTarget() == null || other.getTarget() == target))
                    .forEach(other -> other.setTarget(target));
            AdaptiveAIGoalUtils.debug(enderman, tierSupplier, "enderman coordinated target");
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
}
