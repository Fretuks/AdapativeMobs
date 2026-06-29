package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveMoraleGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveMoraleGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_MORALE_AI.get()
                && tierSupplier.getAsInt() >= 3
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 70 + mob.getRandom().nextInt(80);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        int allies = AdaptiveTargetingUtils.nearbyHostiles(mob, 10.0D,
                other -> other.getTarget() == target || other.getTarget() == null).size() + 1;
        int players = Math.max(1, mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(10.0D),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()).size());

        if (allies > players + 1) {
            Vec3 dest = AdaptivePositioningUtils.darkerSidePosition(mob, target, 2.5D + mob.getId() % 3, 0.0D);
            if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.08D);
                AdaptiveAIGoalUtils.debug(mob, tierSupplier, "group morale pressure");
            }
        } else if (allies <= 1 && AdaptiveAIGoalUtils.healthFraction(mob) < 0.35D) {
            Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 5.0D);
            if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
                AdaptiveAIGoalUtils.debug(mob, tierSupplier, "isolated defensive morale");
            }
        }
    }
}
