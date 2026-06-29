package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveBiomeTacticsGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveBiomeTacticsGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 3 && mob.getTarget() != null && mob.getTarget().isAlive() && --cooldown <= 0;
    }

    @Override
    public void start() {
        int tier = tierSupplier.getAsInt();
        cooldown = tier >= 5 ? 45 + mob.getRandom().nextInt(55) : 80 + mob.getRandom().nextInt(90);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (isNetherMob() && mob.level().dimension() == Level.NETHER && mob.distanceToSqr(target) < 18.0D * 18.0D) {
            double speed = tier >= 5 ? 1.25D : (tier >= 4 ? 1.18D : 1.12D);
            mob.getNavigation().moveTo(target, speed);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "nether aggression");
            return;
        }
        if (mob instanceof Drowned && mob.isInWater() && target.isInWater()) {
            mob.getNavigation().moveTo(target, 1.12D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "drowned water pressure");
            return;
        }
        if (mob instanceof Spider && !mob.level().canSeeSky(mob.blockPosition())) {
            Vec3 elevated = AdaptivePositioningUtils.elevatedPosition(mob, target, 3.0D);
            mob.getNavigation().moveTo(elevated.x, elevated.y, elevated.z, 1.12D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "cave vertical spider pressure");
            return;
        }
        if (mob instanceof AbstractSkeleton && mob.level().canSeeSky(mob.blockPosition())) {
            Vec3 dest = AdaptivePositioningUtils.sidePosition(mob, target, 9.0D, mob.getRandom().nextBoolean() ? 1.0D : -1.0D,
                    AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tierSupplier.getAsInt()) * 0.5D);
            if (AdaptivePositioningUtils.isReasonableGround(mob, dest)) {
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
                AdaptiveAIGoalUtils.debug(mob, tierSupplier, "skeleton open terrain spacing");
            }
        }
    }

    private boolean isNetherMob() {
        return mob instanceof Piglin || mob instanceof PiglinBrute || mob instanceof Hoglin
                || mob instanceof Zoglin
                || mob instanceof Blaze
                || mob.getType() == EntityType.GHAST
                || mob.getType() == EntityType.MAGMA_CUBE;
    }
}
