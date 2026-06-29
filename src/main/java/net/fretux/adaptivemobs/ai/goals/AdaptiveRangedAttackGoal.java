package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveRangedAttackGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final double optimalRange;
    private final double speed;
    private int cooldown;
    private int side;

    public AdaptiveRangedAttackGoal(PathfinderMob mob, IntSupplier tierSupplier, double optimalRange, double speed) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.optimalRange = optimalRange;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_RANGED_AI.get()
                && tierSupplier.getAsInt() >= 2
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = AdaptiveAIGoalUtils.nextCooldown(mob);
        side = mob.getRandom().nextBoolean() ? 1 : -1;
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        mob.getLookControl().setLookAt(target, 40.0F, 40.0F);

        boolean shield = tier >= 3 && AdaptiveAIGoalUtils.isBlockingShield(target);
        boolean badSight = tier >= 3 && !mob.hasLineOfSight(target);
        boolean allyInLine = tier >= 4 && AdaptiveTargetingUtils.hasAllyBetween(mob, target, 0.8D);
        double distance = Math.sqrt(mob.distanceToSqr(target));

        if (shield || badSight || allyInLine || distance < optimalRange * 0.65D || distance > optimalRange * 1.25D) {
            double radius = distance < optimalRange * 0.75D ? optimalRange : Math.max(4.0D, optimalRange * 0.85D);
            double lead = tier >= 5 ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
            Vec3 dest = AdaptivePositioningUtils.sidePosition(mob, target, radius, side, lead);
            if (!AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
                dest = AdaptivePositioningUtils.retreatPosition(mob, target, 4.0D);
            }
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "ranged reposition");
        }
    }
}
