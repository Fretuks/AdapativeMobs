package net.fretux.adaptivemobs.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveSkeletonShieldGoal extends Goal {

    private final PathfinderMob mob;
    private final IntSupplier tierSupplier;
    private int cooldown;

    public AdaptiveSkeletonShieldGoal(PathfinderMob mob, IntSupplier tierSupplier) {
        this.mob = mob;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return tierSupplier.getAsInt() >= 3
                && target != null
                && target.isAlive()
                && target.isUsingItem()
                && isShield(target.getUseItem())
                && mob.hasLineOfSight(target)
                && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 30 + mob.getRandom().nextInt(30);
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 away = mob.position().subtract(target.position());
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).normalize()
                .scale(mob.getRandom().nextBoolean() ? 5.0D : -5.0D);
        Vec3 dest = mob.position().add(side).add(away.normalize().scale(2.0D));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.SHIELD);
    }
}
