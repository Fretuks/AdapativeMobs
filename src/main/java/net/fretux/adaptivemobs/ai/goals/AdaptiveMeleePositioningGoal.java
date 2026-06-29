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

public class AdaptiveMeleePositioningGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final double speed;
    private final boolean conservative;
    private int cooldown;

    public AdaptiveMeleePositioningGoal(PathfinderMob mob, IntSupplier tierSupplier, double speed, boolean conservative) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.speed = speed;
        this.conservative = conservative;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_MELEE_AI.get()
                && tierSupplier.getAsInt() >= 2
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(mob.distanceToSqr(target));
        double follow = AdaptiveAIGoalUtils.followRange(mob, 16.0D);
        if (distance > follow) {
            return;
        }
        mob.getLookControl().setLookAt(target, 35.0F, 35.0F);

        int attackers = AdaptiveTargetingUtils.nearbyAttackers(mob, target, 5.0D);
        boolean crowding = attackers >= (conservative ? 3 : 2);
        boolean sideApproach = tier >= 3 && attackers >= 2;
        boolean intercept = tier >= 5;
        if (!crowding && !sideApproach && !intercept) {
            return;
        }

        double side = mob.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double lead = intercept ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
        double radius = Math.max(2.0D, Math.min(5.0D, distance * 0.6D));
        Vec3 dest = AdaptivePositioningUtils.sidePosition(mob, target, radius, side, lead);
        if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "melee reposition");
        }
    }
}
