package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveSpiderTacticsGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final boolean caveSpider;
    private int cooldown;

    public AdaptiveSpiderTacticsGoal(PathfinderMob mob, IntSupplier tierSupplier, boolean caveSpider) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.caveSpider = caveSpider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        int tier = tierSupplier.getAsInt();
        int min = caveSpider ? 2 : 3;
        return tier >= min && mob.getTarget() != null && mob.getTarget().isAlive() && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = caveSpider ? 30 + mob.getRandom().nextInt(40) : AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(mob.distanceToSqr(target));
        if (tier >= 4) {
            Vec3 elevated = AdaptivePositioningUtils.elevatedPosition(mob, target, 3.0D);
            mob.getNavigation().moveTo(elevated.x, elevated.y, elevated.z, caveSpider ? 1.2D : 1.1D);
        } else if (tier >= 2) {
            Vec3 side = AdaptivePositioningUtils.sidePosition(mob, target, Math.max(2.0D, distance * 0.5D),
                    mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D);
            mob.getNavigation().moveTo(side.x, side.y, side.z, caveSpider ? 1.2D : 1.1D);
        }
        if (tier >= 3 && distance < 7.0D && AdaptiveAIGoalUtils.isUsingRangedWeapon(target) && mob.onGround()) {
            Vec3 leap = target.position().subtract(mob.position()).normalize().scale(caveSpider ? 0.42D : 0.36D);
            mob.setDeltaMovement(mob.getDeltaMovement().add(leap.x, 0.35D, leap.z));
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "spider lunge");
        }
    }
}
