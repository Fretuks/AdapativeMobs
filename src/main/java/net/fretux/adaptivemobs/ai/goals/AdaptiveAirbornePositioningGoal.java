package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveAirbornePositioningGoal extends Goal {

    private static final double MIN_REPOSITION_DISTANCE_SQR = 4.0D;

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
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2 && AdaptiveAIGoalUtils.isValidAdaptiveTarget(mob.getTarget())
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = AdaptiveAIGoalUtils.nextCooldown(mob);
        LivingEntity target = mob.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        boolean spread = tier >= 4 && AdaptiveTargetingUtils.nearbyAttackers(mob, target, range) > 1;
        double lead = tier >= 5 ? AMConfig.tierValue(AMConfig.AI_INTERCEPTION_STRENGTH.get(), tier) : 0.0D;
        Vec3 preferred = AdaptivePositioningUtils.sidePosition(mob, target, range,
                spread && mob.getRandom().nextBoolean() ? -1.0D : 1.0D, lead).add(0.0D, verticalOffset, 0.0D);
        Vec3 dest = safeDestination(preferred);
        if (dest == null || mob.position().distanceToSqr(dest) < MIN_REPOSITION_DISTANCE_SQR) {
            return;
        }
        mob.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, 1.0D);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "airborne reposition");
    }

    private Vec3 safeDestination(Vec3 preferred) {
        double minY = mob.level().getMinBuildHeight() + 1.0D;
        double maxY = mob.level().getMaxBuildHeight() - Math.max(1.0D, mob.getBbHeight());
        double y = Math.max(minY, Math.min(maxY, preferred.y));
        Vec3 base = new Vec3(preferred.x, y, preferred.z);
        if (isClear(base)) {
            return base;
        }
        for (int offset = 1; offset <= 4; offset++) {
            Vec3 above = base.add(0.0D, offset, 0.0D);
            if (above.y <= maxY && isClear(above)) {
                return above;
            }
            Vec3 below = base.add(0.0D, -offset, 0.0D);
            if (below.y >= minY && isClear(below)) {
                return below;
            }
        }
        return null;
    }

    private boolean isClear(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        if (!mob.level().getWorldBorder().isWithinBounds(blockPos)) {
            return false;
        }
        AABB movedBox = mob.getBoundingBox().move(pos.subtract(mob.position()));
        return mob.level().noCollision(mob, movedBox);
    }
}
