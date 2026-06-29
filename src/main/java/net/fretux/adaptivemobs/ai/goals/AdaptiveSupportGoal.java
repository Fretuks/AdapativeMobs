package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveSupportGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveSupportGoal(PathfinderMob mob, IntSupplier tierSupplier) {
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
        cooldown = 70 + mob.getRandom().nextInt(90);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (isMeleeSupporter(mob) && target.getLastHurtMob() instanceof Mob hurt
                && (hurt instanceof AbstractSkeleton || hurt instanceof Pillager || hurt instanceof Witch)
                && mob.distanceToSqr(target) < 12.0D * 12.0D) {
            mob.getNavigation().moveTo(target, 1.08D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "peeling for ranged ally");
            return;
        }
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager || mob instanceof Witch)
                && AdaptiveTargetingUtils.nearbyAttackers(mob, target, 7.0D) > 1) {
            Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 3.0D);
            if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.9D);
                AdaptiveAIGoalUtils.debug(mob, tierSupplier, "holding behind melee pressure");
            }
        }
    }

    private static boolean isMeleeSupporter(Mob mob) {
        return !(mob instanceof AbstractSkeleton) && !(mob instanceof Pillager) && !(mob instanceof Witch);
    }
}
