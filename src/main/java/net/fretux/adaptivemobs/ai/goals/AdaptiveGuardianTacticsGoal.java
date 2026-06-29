package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveGuardianTacticsGoal extends Goal {

    private final Guardian guardian;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveGuardianTacticsGoal(Guardian guardian, IntSupplier tierSupplier) {
        this.guardian = guardian;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_AQUATIC_HOSTILE_AI.get()
                && AMConfig.ENABLE_GUARDIAN_ADVANCED_AI.get()
                && AMConfig.AI_GUARDIAN.get()
                && tierSupplier.getAsInt() >= 2
                && guardian.getTarget() != null
                && guardian.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        boolean elder = guardian instanceof ElderGuardian;
        cooldown = elder ? 90 + guardian.getRandom().nextInt(80) : AdaptiveAIGoalUtils.nextCooldown(guardian);
        LivingEntity target = guardian.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        boolean blocked = !guardian.hasLineOfSight(target);
        boolean coordinate = tier >= 4 && !elder && AdaptiveTargetingUtils.nearbyAttackers(guardian, target, 12.0D) > 1;
        if (blocked || coordinate) {
            Vec3 dest = AdaptivePositioningUtils.sidePosition(guardian, target, elder ? 9.0D : 7.0D,
                    guardian.getRandom().nextBoolean() ? 1.0D : -1.0D, tier >= 5 ? 0.10D : 0.0D);
            guardian.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, elder ? 0.65D : 0.9D);
            AdaptiveAIGoalUtils.debug(guardian, tierSupplier, "guardian pressure reposition");
        }
        if (tier >= 5 && !elder) {
            retargetLowHealthGuardianTarget(target);
        }
    }

    private void retargetLowHealthGuardianTarget(LivingEntity current) {
        List<LivingEntity> candidates = guardian.level().getEntitiesOfClass(LivingEntity.class,
                guardian.getBoundingBox().inflate(12.0D),
                entity -> entity != guardian && entity.isAlive() && guardian.canAttack(entity)
                        && guardian.hasLineOfSight(entity)
                        && AdaptiveAIGoalUtils.healthFraction(entity) < AdaptiveAIGoalUtils.healthFraction(current));
        if (!candidates.isEmpty()) {
            guardian.setTarget(candidates.get(0));
            AdaptiveAIGoalUtils.debug(guardian, tierSupplier, "guardian exposed target pressure");
        }
    }
}
