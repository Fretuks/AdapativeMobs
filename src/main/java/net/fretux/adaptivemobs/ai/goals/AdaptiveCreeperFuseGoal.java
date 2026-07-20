package net.fretux.adaptivemobs.ai.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

import java.util.function.IntSupplier;

/** Keeps the creeper fuse synchronized with the current config-gated adaptive tier. */
public final class AdaptiveCreeperFuseGoal extends Goal {
    private final Creeper creeper;
    private final IntSupplier tierSupplier;
    private int updateCooldown;

    public AdaptiveCreeperFuseGoal(Creeper creeper, IntSupplier tierSupplier) {
        this.creeper = creeper;
        this.tierSupplier = tierSupplier;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        if (--updateCooldown > 0) {
            return;
        }
        updateCooldown = 20;
        int tier = tierSupplier.getAsInt();
        creeper.maxSwell = tier <= 0 ? 30 : Math.max(18, 30 - Math.max(0, tier - 1) * 3);
    }
}
