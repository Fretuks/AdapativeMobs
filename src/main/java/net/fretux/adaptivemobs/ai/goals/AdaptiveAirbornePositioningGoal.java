package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveAirbornePositioningGoal extends Goal {

    private final Mob mob;
    private final IntSupplier tierSupplier;
    private final double range;
    private final double verticalOffset;
    private int cooldown;

    public AdaptiveAirbornePositioningGoal(Mob mob, IntSupplier tierSupplier, double range, double verticalOffset) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.range = range;
        this.verticalOffset = verticalOffset;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2 && mob.getTarget() != null && mob.getTarget().isAlive() && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        boolean spread = tier >= 4 && AdaptiveTargetingUtils.nearbyAttackers(mob, target, range) > 1;
        double lead = tier >= 5 ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
        Vec3 dest = AdaptivePositioningUtils.sidePosition(mob, target, range,
                spread && mob.getRandom().nextBoolean() ? -1.0D : 1.0D, lead).add(0.0D, verticalOffset, 0.0D);
        mob.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, 1.0D);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "airborne reposition");
    }
}
