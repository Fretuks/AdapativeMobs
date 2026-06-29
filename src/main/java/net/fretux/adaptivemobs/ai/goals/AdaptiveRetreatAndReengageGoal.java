package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveRetreatAndReengageGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double triggerHealthLoss;
    private final double speed;
    private int cooldown;
    private float lastHealth;

    public AdaptiveRetreatAndReengageGoal(PathfinderMob mob, IntSupplier tierSupplier,
                                          int minTier, double triggerHealthLoss, double speed) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        this.minTier = minTier;
        this.triggerHealthLoss = triggerHealthLoss;
        this.speed = speed;
        this.lastHealth = mob.getHealth();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        float current = mob.getHealth();
        float lost = lastHealth - current;
        lastHealth = current;
        return AMConfig.AI_RETREAT_ENABLED.get()
                && tierSupplier.getAsInt() >= minTier
                && lost >= mob.getMaxHealth() * triggerHealthLoss
                && mob.getTarget() != null
                && mob.getTarget().isAlive()
                && cooldown-- <= 0;
    }

    @Override
    public void start() {
        cooldown = 80 + mob.getRandom().nextInt(80);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 5.0D);
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "retreat and re-engage");
    }
}
