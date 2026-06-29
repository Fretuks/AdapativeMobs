package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveSharedTargetGoal extends Goal {

    private final Mob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double radius;
    private int cooldown;

    public AdaptiveSharedTargetGoal(Mob mob, IntSupplier tierSupplier, int minTier, double radius) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.minTier = minTier;
        this.radius = radius;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= minTier && mob.getTarget() != null && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        List<Mob> allies = AdaptiveTargetingUtils.nearbyHostiles(mob, radius, other -> other.getTarget() == null);
        int shared = 0;
        for (Mob ally : allies) {
            if (shared++ >= 4) {
                break;
            }
            ally.setTarget(target);
        }
        if (shared > 0) {
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "shared target with " + shared + " allies");
        }
    }
}
