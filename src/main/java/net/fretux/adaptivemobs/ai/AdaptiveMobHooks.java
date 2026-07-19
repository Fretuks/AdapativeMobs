package net.fretux.adaptivemobs.ai;

import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.difficulty.AdaptiveDifficultyManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public final class AdaptiveMobHooks {

    private AdaptiveMobHooks() {
    }

    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!AMConfig.enabled || !AMConfig.AI_ENABLED.get() || !AMConfig.ENABLE_ADVANCED_AI.get()
                || !AMConfig.ENABLE_RANGED_AI.get() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Projectile projectile) || !(projectile.getOwner() instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        int tier = AdaptiveDifficultyManager.getMobTier(level, mob.getType());
        if (tier <= 1) {
            return;
        }

        if (tier >= 3 && AdaptiveAIGoalUtils.isBlockingShield(target) && mob.getRandom().nextFloat() < 0.45F) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.94D));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "shield-blocked shot softened");
            return;
        }
        if (tier >= 4 && AdaptiveTargetingUtils.hasAllyBetween(mob, target, 0.8D)) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.90D));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "ally in ranged line");
            return;
        }

        double accuracy = AMConfig.tierValue(AMConfig.AI_ACCURACY_BONUS.get(), tier);
        if (accuracy <= 0.0D) {
            return;
        }
        double lead = tier >= 5 ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
        Vec3 aim = AdaptivePositioningUtils.predictedPosition(target, lead)
                .add(0.0D, target.getBbHeight() * aimHeight(mob, target, tier), 0.0D);
        Vec3 wanted = aim.subtract(projectile.position()).normalize().scale(projectile.getDeltaMovement().length());
        Vec3 blended = projectile.getDeltaMovement().scale(1.0D - accuracy).add(wanted.scale(accuracy));
        projectile.setDeltaMovement(blended);
        AdaptiveAIGoalUtils.debug(mob, () -> tier, "projectile aim adjusted");
    }

    private static double aimHeight(Mob mob, LivingEntity target, int tier) {
        if ((mob instanceof AbstractSkeleton || mob instanceof Pillager) && tier >= 4) {
            double chance = AMConfig.tierValue(AMConfig.AI_HEADSHOT_CHANCE.get(), tier);
            if (mob.getRandom().nextDouble() < chance) {
                return 0.82D;
            }
        }
        return 0.55D;
    }
}
