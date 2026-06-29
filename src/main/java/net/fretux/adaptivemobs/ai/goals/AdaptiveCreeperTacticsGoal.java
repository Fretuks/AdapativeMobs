package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveCreeperTacticsGoal extends Goal {

    private final Creeper creeper;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private int retreatTicks;
    private int blinkCooldown;

    public AdaptiveCreeperTacticsGoal(Creeper creeper, IntSupplier tierSupplier) {
        this.creeper = creeper;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2 && creeper.getTarget() != null && creeper.getTarget().isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = creeper.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double dist = Math.sqrt(creeper.distanceToSqr(target));
        creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (AdaptiveAIGoalUtils.isBlockingShield(target) && dist <= 7.0D) {
            creeper.setSwellDir(-1);
            if (cooldown-- <= 0) {
                cooldown = 30 + creeper.getRandom().nextInt(35);
                Vec3 dest = AdaptivePositioningUtils.sidePosition(creeper, target, Math.max(3.0D, dist),
                        creeper.getRandom().nextBoolean() ? 1.0D : -1.0D, tier >= 5 ? 0.25D : 0.0D);
                if (AdaptivePositioningUtils.isReasonableGround(creeper, dest)) {
                    creeper.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.05D);
                    AdaptiveAIGoalUtils.debug(creeper, tierSupplier, "creeper shield sidestep");
                }
            }
            return;
        }
        if (tier >= 2 && dist > 4.5D && creeper.getSwellDir() > 0) {
            creeper.setSwellDir(-1);
        }
        if (tier >= 3 && dist > 3.2D && creeper.getSwellDir() > 0) {
            creeper.setSwellDir(-1);
        }
        if (tier >= 4 && AdaptiveTargetingUtils.nearbyAttackers(creeper, target, 7.0D) > 0 && dist <= 5.0D) {
            creeper.setSwellDir(1);
        }
        if (tier >= 5 && dist <= 4.0D && blinkCooldown-- <= 0 && creeper.getSwellDir() <= 0) {
            blinkCooldown = 180 + creeper.getRandom().nextInt(120);
            creeper.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
            creeper.level().playSound(null, creeper.blockPosition(), SoundEvents.CREEPER_PRIMED,
                    SoundSource.HOSTILE, 0.8F, 1.35F);
            AdaptiveAIGoalUtils.debug(creeper, tierSupplier, "creeper blink shroud");
        }
        if (tier >= 5 && retreatTicks-- <= 0 && cooldown-- <= 0 && target.hasLineOfSight(creeper) && dist > 5.0D) {
            cooldown = 120 + creeper.getRandom().nextInt(80);
            retreatTicks = 25;
            Vec3 dest = AdaptivePositioningUtils.retreatPosition(creeper, target, 5.0D);
            creeper.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.05D);
            AdaptiveAIGoalUtils.debug(creeper, tierSupplier, "creeper brief retreat");
        }
    }
}
