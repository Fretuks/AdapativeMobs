package net.fretux.adaptivemobs.ai;

import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AdaptiveTargetingUtils {

    private AdaptiveTargetingUtils() {
    }

    public static List<Mob> nearbyHostiles(Mob mob, double radius, Predicate<Mob> predicate) {
        AABB box = mob.getBoundingBox().inflate(radius);
        List<Mob> found = mob.level().getEntitiesOfClass(Mob.class, box,
                other -> other != mob && other instanceof Enemy && predicate.test(other));
        int max = AMConfig.AI_MAX_NEARBY_MOBS_SCANNED.get();
        if (found.size() <= max) {
            return found;
        }
        return new ArrayList<>(found.subList(0, max));
    }

    public static int nearbyAttackers(Mob mob, LivingEntity target, double radius) {
        if (target == null) {
            return 0;
        }
        return nearbyHostiles(mob, radius, other -> other.getTarget() == target).size();
    }

    public static boolean hasAllyBetween(Mob mob, LivingEntity target, double width) {
        if (target == null) {
            return false;
        }
        Vec3 start = mob.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 line = end.subtract(start);
        double lenSqr = line.lengthSqr();
        if (lenSqr < 1.0E-4D) {
            return false;
        }
        List<Mob> allies = nearbyHostiles(mob, Math.min(16.0D, Math.sqrt(lenSqr) + 2.0D),
                other -> other.getTarget() == target || other.getTarget() == null);
        for (Mob ally : allies) {
            Vec3 toAlly = ally.position().add(0.0D, ally.getBbHeight() * 0.5D, 0.0D).subtract(start);
            double t = toAlly.dot(line) / lenSqr;
            if (t <= 0.05D || t >= 0.95D) {
                continue;
            }
            Vec3 closest = start.add(line.scale(t));
            if (closest.distanceTo(ally.position().add(0.0D, ally.getBbHeight() * 0.5D, 0.0D)) <= width + ally.getBbWidth()) {
                return true;
            }
        }
        return false;
    }
}
