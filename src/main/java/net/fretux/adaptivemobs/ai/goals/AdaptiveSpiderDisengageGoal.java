package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveSpiderDisengageGoal extends Goal {

    private static final int DISENGAGE_TICKS = 100;

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private final int minTier;
    private final double triggerHealthLoss;
    private final double speed;
    private int cooldown;
    private float lastHealth;

    public AdaptiveSpiderDisengageGoal(PathfinderMob mob, IntSupplier tierSupplier,
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
        cooldown = 110 + mob.getRandom().nextInt(90);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DISENGAGE_TICKS, 0));
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DISENGAGE_TICKS, 4));

        Vec3 dest = AdaptivePositioningUtils.retreatPosition(mob, target, 5.0D);
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "spider invisible regeneration disengage");
    }
}
