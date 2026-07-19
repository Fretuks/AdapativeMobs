package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveMemory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.IntSupplier;

public class AdaptiveThreatMemoryGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveThreatMemoryGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2 && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 40 + mob.getRandom().nextInt(50);
        Optional<net.minecraft.server.level.ServerPlayer> remembered = AdaptiveMemory.rememberedThreat(mob);
        if (remembered.isEmpty()) {
            return;
        }
        Player player = remembered.get();
        if (player.level() != mob.level()
                || !AdaptiveAIGoalUtils.isValidAdaptiveTarget(player) || !mob.canAttack(player)) {
            return;
        }
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            mob.setTarget(player);
        }
        if (!mob.hasLineOfSight(player) && mob.distanceToSqr(player) < 18.0D * 18.0D) {
            mob.getNavigation().moveTo(player, 1.0D);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "threat memory pursuit");
        }
    }
}
