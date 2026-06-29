package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveRangedCoverGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveRangedCoverGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 3 && mob.getTarget() != null && mob.getTarget().isAlive() && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 95 + mob.getRandom().nextInt(90);
        LivingEntity target = mob.getTarget();
        if (target == null || mob.distanceToSqr(target) > 16.0D * 16.0D) {
            return;
        }
        Vec3 retreat = AdaptivePositioningUtils.retreatPosition(mob, target, 4.0D);
        Vec3 side = AdaptivePositioningUtils.darkerSidePosition(mob, target, 8.0D, 0.0D);
        Vec3 dest = mob.getRandom().nextBoolean() ? retreat : side;
        if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.95D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "imperfect ranged cover");
        }
    }
}
