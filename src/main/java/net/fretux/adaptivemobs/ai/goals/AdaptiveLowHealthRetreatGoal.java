package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveLowHealthRetreatGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double healthThreshold;
    private final double speed;
    private int cooldown;

    public AdaptiveLowHealthRetreatGoal(PathfinderMob mob, IntSupplier tierSupplier,
                                        int minTier, double healthThreshold, double speed) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.minTier = minTier;
        this.healthThreshold = healthThreshold;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.AI_RETREAT_ENABLED.get()
                && tierSupplier.getAsInt() >= minTier
                && AdaptiveAIGoalUtils.healthFraction(mob) <= healthThreshold
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 120 + mob.getRandom().nextInt(100);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 6.0D);
        if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "low health retreat");
        }
    }
}
