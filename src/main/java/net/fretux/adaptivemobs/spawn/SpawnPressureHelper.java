package net.fretux.adaptivemobs.spawn;

import net.fretux.adaptivemobs.AdaptiveMobs;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Adds modest spawn pressure: on a natural hostile spawn there is a tier-scaled chance to spawn
 * one extra mob of the same type nearby. Capped at one extra per event, gated by local density,
 * and guarded against recursion so extra spawns can never trigger further extra spawns.
 */
public final class SpawnPressureHelper {

    private static final ThreadLocal<Boolean> SPAWNING_EXTRA = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SpawnPressureHelper() {
    }

    public static void maybeSpawnExtra(ServerLevel level, Mob original, int tier, MobSpawnType spawnType) {
        if (!AMConfig.SPAWN_PRESSURE_ENABLED.get() || tier <= 0) {
            return;
        }
        // Recursion guard: an extra spawn must never produce more extra spawns.
        if (SPAWNING_EXTRA.get()) {
            return;
        }
        // Natural spawns only.
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        RandomSource random = level.random;
        double chance = AMConfig.tierValue(AMConfig.EXTRA_SPAWN_CHANCE.get(), tier);
        if (random.nextDouble() > chance) {
            return;
        }

        // Respect local density so we don't snowball.
        int radius = AMConfig.SPAWN_DENSITY_RADIUS.get();
        AABB box = original.getBoundingBox().inflate(radius);
        int nearby = level.getEntitiesOfClass(Monster.class, box).size();
        if (nearby >= AMConfig.SPAWN_DENSITY_CAP.get()) {
            return;
        }

        EntityType<?> type = original.getType();
        Entity created = type.create(level);
        if (!(created instanceof Mob extra)) {
            if (created != null) {
                created.discard();
            }
            return;
        }

        SPAWNING_EXTRA.set(Boolean.TRUE);
        try {
            for (int attempt = 0; attempt < 6; attempt++) {
                double dx = (random.nextDouble() - 0.5) * radius;
                double dz = (random.nextDouble() - 0.5) * radius;
                BlockPos spawnPos = nearbySpawnPos(original, original.getX() + dx, original.getZ() + dz, random);
                if (!level.hasChunkAt(spawnPos) || !level.getWorldBorder().isWithinBounds(spawnPos)) {
                    continue;
                }
                extra.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        random.nextFloat() * 360.0F, 0.0F);
                // Honour collision, light/biome spawn rules and obstruction checks.
                if (level.noCollision(extra)
                        && extra.checkSpawnRules(level, spawnType)
                        && extra.checkSpawnObstruction(level)) {
                    ForgeEventFactory.onFinalizeSpawn(extra, level,
                            level.getCurrentDifficultyAt(extra.blockPosition()), spawnType, null, null);
                    level.addFreshEntity(extra);
                    if (AMConfig.debug) {
                        AdaptiveMobs.LOGGER.info("[AdaptiveMobs] Extra spawn ({}) at tier {} near {}",
                                type, tier, original.blockPosition());
                    }
                    return;
                }
            }
            // No valid position found.
            extra.discard();
        } finally {
            SPAWNING_EXTRA.set(Boolean.FALSE);
        }
    }

    private static BlockPos nearbySpawnPos(Mob original, double x, double z, RandomSource random) {
        int blockX = BlockPos.containing(x, 0.0D, z).getX();
        int blockZ = BlockPos.containing(x, 0.0D, z).getZ();
        // Stay in the original mob's vertical habitat. Spawn rules and collision checks below
        // decide whether a candidate suits ground, cave, aquatic, or airborne mobs.
        int y = original.blockPosition().getY() + random.nextInt(9) - 4;
        return new BlockPos(blockX, y, blockZ);
    }
}
