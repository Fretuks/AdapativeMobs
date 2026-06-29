package net.fretux.adaptivemobs.ai;

import net.minecraft.core.BlockPos;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AdaptivePositioningUtils {

    private AdaptivePositioningUtils() {
    }

    public static Vec3 predictedPosition(LivingEntity target, double strength) {
        if (target == null || strength <= 0.0D) {
            return target == null ? Vec3.ZERO : target.position();
        }
        return target.position().add(target.getDeltaMovement().scale(8.0D * strength));
    }

    public static Vec3 sidePosition(Mob mob, LivingEntity target, double radius, double side, double leadStrength) {
        Vec3 aim = predictedPosition(target, leadStrength);
        Vec3 away = mob.position().subtract(aim);
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        if (len < 1.0E-3D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
            len = 1.0D;
        }
        double angle = Math.toRadians(55.0D * side);
        double nx = away.x * Math.cos(angle) - away.z * Math.sin(angle);
        double nz = away.x * Math.sin(angle) + away.z * Math.cos(angle);
        double scale = radius / len;
        return new Vec3(aim.x + nx * scale, aim.y, aim.z + nz * scale);
    }

    public static Vec3 darkerSidePosition(Mob mob, LivingEntity target, double radius, double leadStrength) {
        Vec3 left = sidePosition(mob, target, radius, 1.0D, leadStrength);
        Vec3 right = sidePosition(mob, target, radius, -1.0D, leadStrength);
        if (!AMConfig.ENABLE_DARK_APPROACH_AI.get()) {
            return mob.getRandom().nextBoolean() ? left : right;
        }
        int leftLight = localLight(mob, left);
        int rightLight = localLight(mob, right);
        if (leftLight == rightLight) {
            return mob.getRandom().nextBoolean() ? left : right;
        }
        return leftLight < rightLight ? left : right;
    }

    public static Vec3 retreatPosition(Mob mob, LivingEntity target, double distance) {
        Vec3 away = mob.position().subtract(target.position());
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        if (len < 1.0E-3D) {
            away = new Vec3(mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D, 0.0D);
            len = 1.0D;
        }
        Vec3 dir = new Vec3(away.x / len, 0.0D, away.z / len);
        return mob.position().add(dir.scale(distance));
    }

    public static boolean hasLineOfSight(Mob mob, LivingEntity target) {
        if (target == null) {
            return false;
        }
        HitResult hit = mob.level().clip(new ClipContext(mob.getEyePosition(), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static boolean isReasonableGround(Mob mob, Vec3 pos) {
        BlockPos block = BlockPos.containing(pos);
        return mob.level().getWorldBorder().isWithinBounds(block)
                && !mob.level().getBlockState(block).liquid()
                && mob.level().getBlockState(block).getCollisionShape(mob.level(), block).isEmpty();
    }

    public static boolean isOpenToSky(Mob mob) {
        return mob.level().canSeeSky(mob.blockPosition());
    }

    public static int localLight(Mob mob, Vec3 pos) {
        return mob.level().getMaxLocalRawBrightness(BlockPos.containing(pos));
    }

    public static Vec3 elevatedPosition(Mob mob, LivingEntity target, double radius) {
        Vec3 base = sidePosition(mob, target, radius, mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D);
        for (int y = 2; y >= 0; y--) {
            Vec3 candidate = base.add(0.0D, y, 0.0D);
            if (isReasonableGround(mob, candidate)) {
                return candidate;
            }
        }
        return base;
    }
}
