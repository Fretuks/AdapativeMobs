package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveEscapeDenialGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveEscapeDenialGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 3
                && (mob instanceof Zombie || mob instanceof Spider)
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 90 + mob.getRandom().nextInt(90);
        LivingEntity target = mob.getTarget();
        if (target == null || mob.distanceToSqr(target) > 12.0D * 12.0D) {
            return;
        }
        Vec3 look = target.getLookAngle();
        Vec3 exitBias = target.position().add(look.x * 3.0D, 0.0D, look.z * 3.0D);
        if (!AdaptivePositioningUtils.isReasonableGround(mob, exitBias)) {
            exitBias = AdaptivePositioningUtils.darkerSidePosition(mob, target, 3.5D, 0.0D);
        }
        if (AdaptivePositioningUtils.isReasonableGround(mob, exitBias)) {
            mob.getNavigation().moveTo(exitBias.x, exitBias.y, exitBias.z, mob instanceof Spider ? 1.15D : 1.0D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "escape route pressure");
        }
    }
}
