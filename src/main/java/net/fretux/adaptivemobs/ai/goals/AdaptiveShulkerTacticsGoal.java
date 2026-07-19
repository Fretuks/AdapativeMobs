package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveShulkerTacticsGoal extends Goal {

    private final Shulker shulker;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private int blockedTicks;

    public AdaptiveShulkerTacticsGoal(Shulker shulker, IntSupplier tierSupplier) {
        this.shulker = shulker;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_END_MOB_AI.get()
                && AMConfig.ENABLE_SHULKER_ADVANCED_AI.get()
                && AMConfig.AI_SHULKER.get()
                && tierSupplier.getAsInt() >= 2
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(shulker.getTarget())
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        int tier = tierSupplier.getAsInt();
        boolean inEnd = shulker.level().dimension() == Level.END;
        cooldown = inEnd && tier >= 5 ? 30 + shulker.getRandom().nextInt(35)
                : 50 + shulker.getRandom().nextInt(tier >= 5 ? 45 : 70);
        LivingEntity target = shulker.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        if (!shulker.hasLineOfSight(target)) {
            blockedTicks++;
            if (blockedTicks >= 2) {
                shulker.setTarget(null);
                blockedTicks = 0;
                AdaptiveAIGoalUtils.debug(shulker, tierSupplier, "shulker held fire without line of sight");
            }
            return;
        }
        blockedTicks = 0;
        if (tier >= 4) {
            double radius = inEnd && tier >= 5 ? 18.0D : 12.0D;
            AdaptiveTargetingUtils.nearbyHostiles(shulker, radius,
                            other -> other instanceof Shulker
                                    && (other.getTarget() == null || other.getTarget() == target)
                                    && other.canAttack(target))
                    .forEach(other -> other.setTarget(target));
            AdaptiveAIGoalUtils.debug(shulker, tierSupplier, "shulker coordinated target");
        }
    }
}
