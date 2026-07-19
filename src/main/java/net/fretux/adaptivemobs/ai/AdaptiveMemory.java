package net.fretux.adaptivemobs.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public final class AdaptiveMemory {

    private static final int MAX_SOUND_ENTRIES_PER_LEVEL = 256;

    private static final Map<Mob, ThreatEntry> THREATS = new WeakHashMap<>();
    private static final Map<ServerLevel, List<SoundEntry>> SOUNDS = new WeakHashMap<>();

    private AdaptiveMemory() {
    }

    public static void rememberThreat(Mob mob, ServerPlayer player, int ticks) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(player) || !mob.canAttack(player)) {
            return;
        }
        THREATS.put(mob, new ThreatEntry(player.getUUID(), level.getGameTime() + ticks));
    }

    public static Optional<ServerPlayer> rememberedThreat(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        ThreatEntry entry = THREATS.get(mob);
        if (entry == null || entry.expiresAt < level.getGameTime()) {
            THREATS.remove(mob);
            return Optional.empty();
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.playerId);
        return AdaptiveAIGoalUtils.isValidAdaptiveTarget(player) && mob.canAttack(player)
                ? Optional.of(player)
                : Optional.empty();
    }

    public static void rememberSound(ServerLevel level, BlockPos pos, SoundKind kind, int ticks) {
        rememberSound(level, Vec3.atCenterOf(pos), kind, ticks);
    }

    public static void rememberSound(ServerLevel level, Vec3 pos, SoundKind kind, int ticks) {
        List<SoundEntry> entries = SOUNDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        pruneExpired(entries, level.getGameTime());
        if (entries.size() >= MAX_SOUND_ENTRIES_PER_LEVEL) {
            entries.remove(0);
        }
        entries.add(new SoundEntry(pos, kind, level.getGameTime() + ticks));
    }

    public static Optional<SoundEntry> nearestSound(ServerLevel level, Mob mob, double radius) {
        List<SoundEntry> entries = SOUNDS.get(level);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        double maxSqr = radius * radius;
        SoundEntry best = null;
        double bestSqr = Double.MAX_VALUE;
        Iterator<SoundEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            SoundEntry entry = iterator.next();
            if (entry.expiresAt < now) {
                iterator.remove();
                continue;
            }
            double dist = mob.distanceToSqr(entry.pos);
            if (dist < maxSqr && dist < bestSqr) {
                best = entry;
                bestSqr = dist;
            }
        }
        return Optional.ofNullable(best);
    }

    public static void clear(ServerLevel level) {
        SOUNDS.remove(level);
    }

    private static void pruneExpired(List<SoundEntry> entries, long now) {
        entries.removeIf(entry -> entry.expiresAt < now);
    }

    public enum SoundKind {
        BLOCK_BREAK,
        EATING,
        BOW_SHOT,
        EXPLOSION
    }

    private record ThreatEntry(UUID playerId, long expiresAt) {
    }

    public record SoundEntry(Vec3 pos, SoundKind kind, long expiresAt) {
    }
}
