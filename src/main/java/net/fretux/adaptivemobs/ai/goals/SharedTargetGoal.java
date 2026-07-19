package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class SharedTargetGoal extends Goal {

    private final Mob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double radius;
    private int cooldown;

    public SharedTargetGoal(Mob mob, IntSupplier tierSupplier, int minTier, double radius) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.minTier = minTier;
        this.radius = radius;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= minTier && AdaptiveAIGoalUtils.isValidAdaptiveTarget(mob.getTarget())
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 40 + mob.getRandom().nextInt(40);
        LivingEntity target = mob.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        AABB box = mob.getBoundingBox().inflate(radius);
        List<Mob> allies = mob.level().getEntitiesOfClass(Mob.class, box,
                other -> other != mob && other instanceof Enemy && other.getTarget() == null && other.canAttack(target));
        int shared = 0;
        for (Mob ally : allies) {
            if (shared++ >= 4) {
                break;
            }
            ally.setTarget(target);
        }
    }
}
