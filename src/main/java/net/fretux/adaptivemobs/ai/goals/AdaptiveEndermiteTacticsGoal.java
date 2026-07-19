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

public class AdaptiveEndermiteTacticsGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private LivingEntity rememberedTarget;

    public AdaptiveEndermiteTacticsGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_MELEE_AI.get()
                && AMConfig.ENABLE_ENDERMITE_AI.get()
                && AMConfig.AI_SMALL_SWARM.get()
                && tierSupplier.getAsInt() >= 2
                && currentTarget() != null
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 18 + mob.getRandom().nextInt(28);
        LivingEntity target = currentTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        rememberedTarget = target;
        if (mob.getTarget() == null) {
            mob.setTarget(target);
        }

        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(mob.distanceToSqr(target));
        double side = mob.getId() % 2 == 0 ? 1.0D : -1.0D;
        double lead = tier >= 5 ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
        Vec3 dest = AdaptivePositioningUtils.sidePosition(mob, target, Math.max(1.5D, distance * 0.45D), side, lead);
        if (!AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
            dest = target.position();
        }
        mob.getLookControl().setLookAt(target, 35.0F, 35.0F);
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, tier >= 5 ? 1.25D : 1.15D);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "endermite side pursuit");
    }

    private LivingEntity currentTarget() {
        LivingEntity target = mob.getTarget();
        if (AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return target;
        }
        if (AdaptiveAIGoalUtils.isValidAdaptiveTarget(rememberedTarget)
                && mob.distanceToSqr(rememberedTarget) < 16.0D * 16.0D
                && mob.canAttack(rememberedTarget)) {
            return rememberedTarget;
        }
        rememberedTarget = null;
        return null;
    }
}
