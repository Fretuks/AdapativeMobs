package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveMemory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.IntSupplier;

public class AdaptiveSoundInvestigationGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveSoundInvestigationGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2
                && mob.getTarget() == null
                && mob.level() instanceof ServerLevel
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 80 + mob.getRandom().nextInt(80);
        ServerLevel level = (ServerLevel) mob.level();
        Optional<AdaptiveMemory.SoundEntry> sound = AdaptiveMemory.nearestSound(level, mob, 14.0D);
        if (sound.isEmpty()) {
            return;
        }
        mob.getNavigation().moveTo(sound.get().pos().x, sound.get().pos().y, sound.get().pos().z, 1.0D);
        AdaptiveAIGoalUtils.debug(mob, tierSupplier, "investigating " + sound.get().kind());
    }
}
