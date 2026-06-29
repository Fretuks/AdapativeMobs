package net.fretux.adaptivemobs.ai;

import net.fretux.adaptivemobs.AdaptiveMobs;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.IntSupplier;

public final class AdaptiveAIGoalUtils {

    private AdaptiveAIGoalUtils() {
    }

    public static boolean advancedEnabled(int tier) {
        return AMConfig.AI_ENABLED.get() && AMConfig.ENABLE_ADVANCED_AI.get() && tier > 0;
    }

    public static boolean isBlockingShield(LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return false;
        }
        ItemStack stack = entity.getUseItem();
        return !stack.isEmpty() && stack.is(Items.SHIELD);
    }

    public static boolean isUsingRangedWeapon(LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return false;
        }
        ItemStack stack = entity.getUseItem();
        return stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT);
    }

    public static boolean isEating(LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return false;
        }
        ItemStack stack = entity.getUseItem();
        return stack.isEdible();
    }

    public static boolean isLikelyMeleeWeapon(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.IRON_SWORD)
                || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.NETHERITE_SWORD)
                || stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE) || stack.is(Items.IRON_AXE)
                || stack.is(Items.GOLDEN_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE);
    }

    public static int armorPieces(LivingEntity entity) {
        int pieces = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!stack.isEmpty()) {
                pieces++;
            }
        }
        return pieces;
    }

    public static double playerPriorityScore(Mob mob, Player player) {
        double score = 0.0D;
        score += (1.0D - healthFraction(player)) * 5.0D;
        if (isEating(player)) {
            score += 2.0D;
        }
        if (isUsingRangedWeapon(player)) {
            score += 1.5D;
        }
        score += Math.max(0, 4 - armorPieces(player)) * 0.7D;
        if (mob instanceof AbstractSkeleton && !isBlockingShield(player)) {
            score += 1.2D;
        }
        if (!(mob instanceof AbstractSkeleton) && armorPieces(player) <= 2) {
            score += 0.8D;
        }
        if (player.getLastHurtMob() instanceof Mob && player.getLastHurtMob() != mob) {
            score += 1.0D;
        }
        score -= Math.sqrt(mob.distanceToSqr(player)) * 0.08D;
        return score;
    }

    public static double healthFraction(LivingEntity entity) {
        if (entity == null || entity.getMaxHealth() <= 0.0F) {
            return 1.0D;
        }
        return entity.getHealth() / entity.getMaxHealth();
    }

    public static double followRange(Mob mob, double fallback) {
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) == null) {
            return fallback;
        }
        return mob.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    public static int nextCooldown(Mob mob) {
        return AMConfig.aiScanCooldown(mob.getRandom());
    }

    public static boolean hasServerLevel(Mob mob) {
        return mob.level() instanceof ServerLevel;
    }

    public static void debug(Mob mob, IntSupplier tier, String message) {
        if (!AMConfig.DEBUG_AI_LOGGING.get()) {
            return;
        }
        AdaptiveMobs.LOGGER.info("[AdaptiveMobs AI] {} tier {}: {}", mob.getType(), tier.getAsInt(), message);
    }
}
