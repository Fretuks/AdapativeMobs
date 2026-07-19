package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveGoalInjector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;

import java.util.EnumSet;

public class AdaptiveTargetSanitizerGoal extends Goal {

    private final Mob mob;

    public AdaptiveTargetSanitizerGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!AdaptiveGoalInjector.isAIEnabledFor(mob)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        return (target != null && !AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) || shouldReleaseZombieStack(target);
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null && !AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            mob.setTarget(null);
        }
        if (shouldReleaseZombieStack(target)) {
            mob.stopRiding();
            AdaptiveAIGoalUtils.clearAdaptiveStackedZombie(mob);
        }
    }

    private boolean shouldReleaseZombieStack(LivingEntity target) {
        if (!(mob instanceof Zombie) || !(mob.getVehicle() instanceof Zombie)) {
            return false;
        }
        if (!AdaptiveAIGoalUtils.isAdaptiveStackedZombie(mob)) {
            return true;
        }
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return true;
        }
        if (mob.distanceToSqr(target) > 8.0D * 8.0D) {
            return true;
        }
        Entity base = stackBase(mob);
        double verticalFromBase = target.getY() - base.getY();
        double horizontalFromBase = Math.sqrt((target.getX() - base.getX()) * (target.getX() - base.getX())
                + (target.getZ() - base.getZ()) * (target.getZ() - base.getZ()));
        return verticalFromBase <= 2.5D || horizontalFromBase >= 5.5D;
    }

    private Entity stackBase(Entity entity) {
        Entity cursor = entity;
        while (cursor.getVehicle() instanceof Mob) {
            cursor = cursor.getVehicle();
        }
        return cursor;
    }
}
