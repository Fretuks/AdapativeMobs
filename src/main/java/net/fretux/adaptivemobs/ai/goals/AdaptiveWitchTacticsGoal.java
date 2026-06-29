package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.IntSupplier;

public class AdaptiveWitchTacticsGoal extends Goal {

    private final Witch witch;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private int potionCooldown;

    public AdaptiveWitchTacticsGoal(Witch witch, IntSupplier tierSupplier) {
        this.witch = witch;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return tierSupplier.getAsInt() >= 2 && witch.getTarget() != null && witch.getTarget().isAlive() && --cooldown <= 0;
    }

    @Override
    public void start() {
        cooldown = 45 + witch.getRandom().nextInt(55);
        potionCooldown = Math.max(0, potionCooldown - cooldown);
        LivingEntity target = witch.getTarget();
        if (target == null) {
            return;
        }
        int tier = tierSupplier.getAsInt();
        double dist = Math.sqrt(witch.distanceToSqr(target));
        witch.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (dist < 8.0D || (tier >= 4 && AdaptiveTargetingUtils.nearbyAttackers(witch, target, 8.0D) > 0)) {
            Vec3 dest = tier >= 4
                    ? AdaptivePositioningUtils.sidePosition(witch, target, 9.0D, witch.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D)
                    : AdaptivePositioningUtils.retreatPosition(witch, target, 5.0D);
            witch.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
            AdaptiveAIGoalUtils.debug(witch, tierSupplier, "witch spacing");
        }

        if ((tier >= 3 && target.getDeltaMovement().horizontalDistanceSqr() > 0.05D)
                || (tier >= 4 && dist < 7.0D)
                || (tier >= 5 && AdaptiveAIGoalUtils.healthFraction(target) < 0.45D)) {
            witch.performRangedAttack(target, 1.0F);
            AdaptiveAIGoalUtils.debug(witch, tierSupplier, "witch extra potion opportunity");
        }

        if (AMConfig.ENABLE_WITCH_ADVANCED_POTION_AI.get() && potionCooldown <= 0 && !witch.isDrinkingPotion()) {
            if (tryDrinkHealing(tier)) {
                potionCooldown = 140 + witch.getRandom().nextInt(80);
                AdaptiveAIGoalUtils.debug(witch, tierSupplier, "witch early heal");
            } else if (trySupportWoundedAlly(tier)) {
                potionCooldown = 120 + witch.getRandom().nextInt(80);
            } else if (tryThrowSpecificPotion(target, tier, dist)) {
                potionCooldown = 90 + witch.getRandom().nextInt(70);
            }
        }
    }

    private boolean tryDrinkHealing(int tier) {
        double threshold = tier >= 5 ? 0.62D : tier >= 4 ? 0.52D : 0.42D;
        if (AdaptiveAIGoalUtils.healthFraction(witch) > threshold) {
            return false;
        }
        ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
        witch.setItemSlot(EquipmentSlot.MAINHAND, potion);
        witch.setUsingItem(true);
        return true;
    }

    private boolean trySupportWoundedAlly(int tier) {
        if (!AMConfig.ENABLE_SUPPORT_AI.get() || tier < 3) {
            return false;
        }
        for (Mob ally : AdaptiveTargetingUtils.nearbyHostiles(witch, 8.0D,
                other -> other != witch && AdaptiveAIGoalUtils.healthFraction(other) < 0.45D)) {
            if (witch.hasLineOfSight(ally)) {
                throwPotion(ally, Potions.REGENERATION);
                AdaptiveAIGoalUtils.debug(witch, tierSupplier, "witch support potion");
                return true;
            }
        }
        return false;
    }

    private boolean tryThrowSpecificPotion(LivingEntity target, int tier, double dist) {
        Potion potion = null;
        String debug = null;
        if (tier >= 5 && AdaptiveAIGoalUtils.healthFraction(target) < 0.40D) {
            potion = Potions.HARMING;
            debug = "witch harming pressure";
        } else if ((tier >= 4 && dist < 6.0D && isLikelyMelee(target.getMainHandItem()))
                || (tier >= 3 && dist < 7.0D && isHighDamageMelee(target.getMainHandItem()))) {
            potion = Potions.WEAKNESS;
            debug = "witch weakness punish";
        } else if (tier >= 3 && (target.isSprinting() || target.getDeltaMovement().horizontalDistanceSqr() > 0.08D)) {
            potion = Potions.SLOWNESS;
            debug = "witch slowness punish";
        }
        if (potion == null || !witch.hasLineOfSight(target)) {
            return false;
        }
        throwPotion(target, potion);
        AdaptiveAIGoalUtils.debug(witch, tierSupplier, debug);
        return true;
    }

    private void throwPotion(LivingEntity target, Potion potion) {
        ThrownPotion thrown = new ThrownPotion(witch.level(), witch);
        thrown.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        double dx = target.getX() + target.getDeltaMovement().x - witch.getX();
        double dz = target.getZ() + target.getDeltaMovement().z - witch.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.5D) - thrown.getY() + dist * 0.2D;
        thrown.shoot(dx, dy, dz, 0.75F, 8.0F);
        witch.level().addFreshEntity(thrown);
    }

    private static boolean isLikelyMelee(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.IRON_SWORD)
                || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.IRON_AXE)
                || stack.is(Items.GOLDEN_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE);
    }

    private static boolean isHighDamageMelee(ItemStack stack) {
        return stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.IRON_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE);
    }
}
