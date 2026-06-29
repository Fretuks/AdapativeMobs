package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveEvokerTacticsGoal extends Goal {

    private final Evoker evoker;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private int lastSpellTicks;
    private int stillTicks;
    private Vec3 lastPos = Vec3.ZERO;

    public AdaptiveEvokerTacticsGoal(Evoker evoker, IntSupplier tierSupplier) {
        this.evoker = evoker;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_RAID_MOB_AI.get()
                && AMConfig.ENABLE_EVOKER_AI.get()
                && AMConfig.AI_EVOKER.get()
                && tierSupplier.getAsInt() >= 2
                && evoker.getTarget() != null
                && evoker.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = evoker.isCastingSpell() ? 35 + evoker.getRandom().nextInt(35) : 55 + evoker.getRandom().nextInt(55);
        LivingEntity target = evoker.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double distance = Math.sqrt(evoker.distanceToSqr(target));
        boolean justCast = lastSpellTicks > 0 || evoker.isCastingSpell();
        lastSpellTicks = evoker.isCastingSpell() ? 60 : Math.max(0, lastSpellTicks - cooldown);
        updateStillTicks();

        boolean meleeRush = distance < 7.0D && !AdaptiveAIGoalUtils.isUsingRangedWeapon(target);
        boolean exposed = evoker.hasLineOfSight(target) && !AdaptiveAIGoalUtils.isBlockingShield(target);
        boolean useCover = tier >= 4 && AdaptiveTargetingUtils.nearbyHostiles(evoker, 8.0D,
                other -> other instanceof AbstractIllager && other != evoker && other.getTarget() == target).size() > 0;

        if (meleeRush || justCast || stillTicks > 3 || (useCover && exposed)) {
            Vec3 dest = useCover
                    ? AdaptivePositioningUtils.sidePosition(evoker, target, 10.0D, evoker.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D)
                    : AdaptivePositioningUtils.retreatPosition(evoker, target, meleeRush ? 6.0D : 4.0D);
            if (AdaptivePositioningUtils.isReasonableGround(evoker, dest)) {
                evoker.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
                AdaptiveAIGoalUtils.debug(evoker, tierSupplier, "evoker reposition");
            }
        }
    }

    private void updateStillTicks() {
        Vec3 pos = evoker.position();
        if (pos.distanceToSqr(lastPos) < 0.08D) {
            stillTicks++;
        } else {
            stillTicks = 0;
        }
        lastPos = pos;
    }
}
