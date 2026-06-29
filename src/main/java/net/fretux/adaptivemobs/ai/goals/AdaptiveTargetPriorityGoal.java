package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveTargetPriorityGoal extends Goal {

    private final Mob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveTargetPriorityGoal(Mob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return AMConfig.ENABLE_TARGET_PRIORITY_AI.get()
                && tierSupplier.getAsInt() >= 3
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 80 + mob.getRandom().nextInt(80);
        double radius = AdaptiveAIGoalUtils.followRange(mob, 16.0D);
        List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()
                        && mob.canAttack(player) && mob.hasLineOfSight(player));
        Player best = null;
        double bestScore = currentScore();
        for (Player player : players) {
            double score = AdaptiveAIGoalUtils.playerPriorityScore(mob, player);
            if (score > bestScore + 1.0D) {
                bestScore = score;
                best = player;
            }
        }
        if (best != null) {
            mob.setTarget(best);
            AdaptiveAIGoalUtils.debug(mob, tierSupplier, "priority target selected");
        }
    }

    private double currentScore() {
        if (mob.getTarget() instanceof Player player) {
            return AdaptiveAIGoalUtils.playerPriorityScore(mob, player);
        }
        return 0.0D;
    }
}
