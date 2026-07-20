package net.fretux.adaptivemobs.difficulty;

import net.fretux.adaptivemobs.AdaptiveMobs;
import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.data.AdaptiveSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Central authority for "how hard should mobs be right now".
 *
 * <p>The global tier is the dominant signal (world day + total hostile kills). A per-mob-type
 * bonus can nudge specific mob families up once players have killed a lot of them.</p>
 */
public final class AdaptiveDifficultyManager {

    private AdaptiveDifficultyManager() {
    }

    /** Current world day, derived from the overworld day-time. */
    public static long getWorldDay(ServerLevel level) {
        return level.getServer().overworld().getDayTime() / 24000L;
    }

    /** Global progression tier in the range [0, maxTier]. */
    public static int getGlobalTier(ServerLevel level) {
        if (!AMConfig.enabled) {
            return 0;
        }
        AdaptiveSavedData data = AdaptiveSavedData.get(level);
        int max = AMConfig.maxTier;

        if (data.getManualGlobalTier() >= 0) {
            return clamp(data.getManualGlobalTier(), max);
        }

        int dayTier = tierFromDay(getWorldDay(level));
        int killTier = tierFromKills(data.getTotalHostileKills());
        return clamp(Math.max(dayTier, killTier), max);
    }

    /** Effective tier for a specific mob type (global tier plus an optional per-type bonus). */
    public static int getMobTier(ServerLevel level, EntityType<?> type) {
        if (!AMConfig.enabled) {
            return 0;
        }
        int global = getGlobalTier(level);
        int perTypeThreshold = AMConfig.MOB_TYPE_KILL_BONUS_THRESHOLD.get();
        if (perTypeThreshold <= 0) {
            return global;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null) {
            return global;
        }
        int typeKills = AdaptiveSavedData.get(level).getKillsForType(key.toString());
        int bonus = typeKills / perTypeThreshold;
        return clamp(global + bonus, AMConfig.maxTier);
    }

    /** Records a player kill if the victim is a hostile mob. */
    public static void recordKill(ServerPlayer player, LivingEntity killed) {
        if (!AMConfig.enabled || player == null || killed == null) {
            return;
        }
        if (!(killed instanceof Enemy)) {
            return; // only count hostile mobs
        }
        ServerLevel level = player.serverLevel();
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(killed.getType());
        if (key == null) {
            return;
        }
        AdaptiveSavedData data = AdaptiveSavedData.get(level);
        data.addKill(key.toString());
        if (AMConfig.debug) {
            AdaptiveMobs.LOGGER.info("[AdaptiveMobs] Recorded kill of {} (total={}, type={}, globalTier={})",
                    key, data.getTotalHostileKills(), data.getKillsForType(key.toString()), getGlobalTier(level));
        }
    }

    private static int tierFromDay(long day) {
        int tier = 0;
        int configuredTiers = Math.min(5, AMConfig.DAY_THRESHOLDS.get().size());
        for (int t = 1; t <= configuredTiers; t++) {
            if (day >= AMConfig.tierValueInt(AMConfig.DAY_THRESHOLDS.get(), t)) {
                tier = t;
            }
        }
        return tier;
    }

    private static int tierFromKills(long kills) {
        int tier = 0;
        int configuredTiers = Math.min(5, AMConfig.KILL_THRESHOLDS.get().size());
        for (int t = 1; t <= configuredTiers; t++) {
            if (kills >= AMConfig.tierValueInt(AMConfig.KILL_THRESHOLDS.get(), t)) {
                tier = t;
            }
        }
        return tier;
    }

    private static int clamp(int tier, int max) {
        return Math.max(0, Math.min(tier, max));
    }
}
