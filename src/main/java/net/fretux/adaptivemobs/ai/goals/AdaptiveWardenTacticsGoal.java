package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.warden.Warden;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveWardenTacticsGoal extends Goal {

    private final Warden warden;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private LivingEntity rememberedTarget;
    private LivingEntity lastHurtBy;
    private int repeatedRangedTicks;

    public AdaptiveWardenTacticsGoal(Warden warden, IntSupplier tierSupplier) {
        this.warden = warden;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_WARDEN_AI.get()
                && AMConfig.ENABLE_WARDEN_ADVANCED_AI.get()
                && AMConfig.AI_WARDEN.get()
                && tierSupplier.getAsInt() >= 4
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 80 + warden.getRandom().nextInt(80);
        LivingEntity target = warden.getTarget();
        if (target != null && target.isAlive()) {
            rememberedTarget = target;
            warden.getLookControl().setLookAt(target, 25.0F, 25.0F);
            return;
        }
        if (rememberedTarget != null && rememberedTarget.isAlive() && warden.distanceToSqr(rememberedTarget) < 24.0D * 24.0D) {
            warden.setAttackTarget(rememberedTarget);
            AdaptiveAIGoalUtils.debug(warden, tierSupplier, "warden conservative target persistence");
        }

        LivingEntity hurtBy = warden.getLastHurtByMob();
        if (tierSupplier.getAsInt() >= 5 && hurtBy != null && hurtBy.isAlive() && AdaptiveAIGoalUtils.isUsingRangedWeapon(hurtBy)) {
            repeatedRangedTicks = hurtBy == lastHurtBy ? repeatedRangedTicks + 1 : 1;
            lastHurtBy = hurtBy;
            if (repeatedRangedTicks >= 2) {
                warden.setAttackTarget(hurtBy);
                AdaptiveAIGoalUtils.debug(warden, tierSupplier, "warden repeated ranged response");
            }
        }
    }
}
