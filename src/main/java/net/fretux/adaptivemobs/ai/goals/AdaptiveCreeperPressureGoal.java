package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveCreeperPressureGoal extends Goal {

    private final Creeper creeper;
    private final IntSupplier tierSupplier;

    public AdaptiveCreeperPressureGoal(Creeper creeper, IntSupplier tierSupplier) {
        this.creeper = creeper;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = creeper.getTarget();
        return tierSupplier.getAsInt() >= 3 && target != null && target.isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = creeper.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double startSwell = tier >= 5 ? 5.0D : 4.0D;
        double stopSwell = tier >= 4 ? 7.0D : 6.0D;
        double dist = Math.sqrt(creeper.distanceToSqr(target));
        creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (AdaptiveAIGoalUtils.isBlockingShield(target) && dist <= stopSwell) {
            creeper.setSwellDir(-1);
            Vec3 dest = AdaptivePositioningUtils.sidePosition(creeper, target, Math.max(2.8D, dist),
                    creeper.getRandom().nextBoolean() ? 1.0D : -1.0D, tier >= 5 ? 0.25D : 0.0D);
            if (AdaptivePositioningUtils.isReasonableGround(creeper, dest)) {
                creeper.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.05D);
            }
            return;
        }
        if (dist <= startSwell || (tier >= 4 && target.getLastHurtMob() != null && dist <= stopSwell)) {
            creeper.setSwellDir(1);
            creeper.getNavigation().stop();
        } else {
            creeper.setSwellDir(-1);
        }
    }
}
