package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveZombiePackTacticsGoal extends Goal {

    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));

    private final Zombie zombie;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveZombiePackTacticsGoal(Zombie zombie, IntSupplier tierSupplier) {
        this.zombie = zombie;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        int tier = tierSupplier.getAsInt();
        LivingEntity target = zombie.getTarget();
        return tier >= 4
                && target != null
                && target.isAlive()
                && !zombie.isPassenger()
                && zombie.distanceToSqr(target) <= 9.0D * 9.0D
                && AdaptiveTargetingUtils.nearbyAttackers(zombie, target, 6.0D) >= 3
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        int tier = tierSupplier.getAsInt();
        cooldown = tier >= 5 ? 8 + zombie.getRandom().nextInt(13) : 18 + zombie.getRandom().nextInt(22);
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        zombie.getLookControl().setLookAt(target, 35.0F, 35.0F);

        List<Zombie> pack = zombie.level().getEntitiesOfClass(Zombie.class, zombie.getBoundingBox().inflate(6.0D),
                other -> other != zombie && other.isAlive() && other.getTarget() == target);
        if (pack.isEmpty()) {
            return;
        }

        Vec3 dest = packSlot(target, pack.size() + 1);
        dest = dest.add(localSeparation(pack));
        if (!AdaptivePositioningUtils.isReasonableGround(zombie, dest)) {
            dest = AdaptivePositioningUtils.sidePosition(zombie, target, 2.5D, zombie.getId() % 2 == 0 ? 1.0D : -1.0D,
                    tier >= 5 ? 0.12D : 0.0D);
        }
        if (AdaptivePositioningUtils.isReasonableGround(zombie, dest)) {
            zombie.getNavigation().moveTo(dest.x, dest.y, dest.z, tier >= 5 ? 1.12D : 1.04D);
            AdaptiveAIGoalUtils.debug(zombie, tierSupplier, "zombie pack spread");
        }
    }

    private Vec3 packSlot(LivingEntity target, int packSize) {
        double radius = Math.min(4.0D, 2.0D + packSize * 0.18D);
        double angle = (Math.floorMod(zombie.getId(), 16) * GOLDEN_ANGLE) + (packSize * 0.17D);
        return new Vec3(target.getX() + Math.cos(angle) * radius, target.getY(), target.getZ() + Math.sin(angle) * radius);
    }

    private Vec3 localSeparation(List<Zombie> pack) {
        double pushX = 0.0D;
        double pushZ = 0.0D;
        for (Zombie other : pack) {
            double distSqr = zombie.distanceToSqr(other);
            if (distSqr < 0.01D || distSqr > 2.25D) {
                continue;
            }
            Vec3 away = zombie.position().subtract(other.position());
            double scale = (2.25D - distSqr) / 2.25D;
            pushX += away.x * scale;
            pushZ += away.z * scale;
        }
        return new Vec3(pushX, 0.0D, pushZ);
    }
}
