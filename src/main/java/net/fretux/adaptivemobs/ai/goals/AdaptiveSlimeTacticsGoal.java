package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveSlimeTacticsGoal extends Goal {

    private final Slime slime;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveSlimeTacticsGoal(Slime slime, IntSupplier tierSupplier) {
        this.slime = slime;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        boolean magma = slime instanceof MagmaCube;
        return AMConfig.ENABLE_MELEE_AI.get()
                && (magma ? AMConfig.ENABLE_MAGMA_CUBE_ADVANCED_AI.get() : AMConfig.ENABLE_SLIME_ADVANCED_AI.get())
                && AMConfig.AI_SLIME.get()
                && tierSupplier.getAsInt() >= 2
                && slime.getTarget() != null
                && slime.getTarget().isAlive()
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        int tier = tierSupplier.getAsInt();
        cooldown = Math.max(16, 52 - tier * 5) + slime.getRandom().nextInt(25);
        LivingEntity target = slime.getTarget();
        if (target == null) {
            return;
        }
        double distance = Math.sqrt(slime.distanceToSqr(target));
        double side = slime.getId() % 2 == 0 ? 1.0D : -1.0D;
        Vec3 dest = AdaptivePositioningUtils.sidePosition(slime, target, Math.max(2.0D, distance * 0.55D), side, 0.0D);
        if (!AdaptivePositioningUtils.isReasonableGround(slime, dest)) {
            dest = target.position();
        }
        double speed = tier >= 5 && slime.getSize() >= 3 ? 1.25D : 1.0D;
        slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
        slime.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speed);
        if (slime.onGround() && distance < 9.0D) {
            Vec3 leap = target.position().subtract(slime.position()).normalize().scale(tier >= 5 && slime.getSize() >= 3 ? 0.34D : 0.24D);
            slime.setDeltaMovement(slime.getDeltaMovement().add(leap.x, 0.28D, leap.z));
        }
        AdaptiveAIGoalUtils.debug(slime, tierSupplier, "slime angled jump pressure");
    }
}
