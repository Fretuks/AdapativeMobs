package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveRavagerTacticsGoal extends Goal {

    private final Ravager ravager;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private LivingEntity rememberedTarget;

    public AdaptiveRavagerTacticsGoal(Ravager ravager, IntSupplier tierSupplier) {
        this.ravager = ravager;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_RAID_MOB_AI.get()
                && AMConfig.ENABLE_RAVAGER_ADVANCED_AI.get()
                && AMConfig.AI_RAVAGER.get()
                && tierSupplier.getAsInt() >= 3
                && currentTarget() != null
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 55 + ravager.getRandom().nextInt(65);
        LivingEntity target = currentTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        rememberedTarget = target;
        if (ravager.getTarget() == null) {
            ravager.setTarget(target);
        }
        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(ravager.distanceToSqr(target));
        boolean tooClose = distance < 3.5D;
        boolean coordinated = tier >= 4 && AdaptiveTargetingUtils.nearbyHostiles(ravager, 10.0D,
                other -> other instanceof AbstractIllager && other.getTarget() == target).size() > 0;
        if (tooClose || coordinated || (tier >= 5 && distance < 10.0D && ravager.hasLineOfSight(target))) {
            Vec3 dest = tooClose
                    ? AdaptivePositioningUtils.retreatPosition(ravager, target, 3.0D)
                    : AdaptivePositioningUtils.sidePosition(ravager, target, 5.0D, ravager.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.08D);
            if (AdaptivePositioningUtils.isReasonableGround(ravager, dest)) {
                ravager.getNavigation().moveTo(dest.x, dest.y, dest.z, tooClose ? 0.85D : 1.05D);
                AdaptiveAIGoalUtils.debug(ravager, tierSupplier, "ravager deliberate pressure");
            }
        }
    }

    private LivingEntity currentTarget() {
        LivingEntity target = ravager.getTarget();
        if (AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return target;
        }
        if (AdaptiveAIGoalUtils.isValidAdaptiveTarget(rememberedTarget)
                && ravager.distanceToSqr(rememberedTarget) < 20.0D * 20.0D
                && ravager.canAttack(rememberedTarget)) {
            return rememberedTarget;
        }
        rememberedTarget = null;
        return null;
    }
}
