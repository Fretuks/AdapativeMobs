package net.fretux.adaptivemobs.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * World-scoped progression state. Stored on the overworld's data storage so it acts as a single
 * global value for the whole server, regardless of which dimension a kill happened in.
 */
public class AdaptiveSavedData extends SavedData {

    public static final String DATA_NAME = "adaptivemobs_progression";
    private static final int CURRENT_VERSION = 1;

    private long totalHostileKills;
    private final Map<String, Integer> killsByType = new HashMap<>();
    private int manualGlobalTier = -1; // -1 = no manual override
    private int version = CURRENT_VERSION;

    public AdaptiveSavedData() {
    }

    /** Resolves the single global instance, always backed by the overworld's storage. */
    public static AdaptiveSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(AdaptiveSavedData::load, AdaptiveSavedData::new, DATA_NAME);
    }

    public static AdaptiveSavedData load(CompoundTag tag) {
        AdaptiveSavedData data = new AdaptiveSavedData();
        data.totalHostileKills = tag.getLong("totalHostileKills");
        data.manualGlobalTier = tag.contains("manualGlobalTier") ? tag.getInt("manualGlobalTier") : -1;
        data.version = tag.contains("version") ? tag.getInt("version") : CURRENT_VERSION;
        CompoundTag byType = tag.getCompound("killsByType");
        for (String key : byType.getAllKeys()) {
            data.killsByType.put(key, byType.getInt(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("totalHostileKills", totalHostileKills);
        tag.putInt("manualGlobalTier", manualGlobalTier);
        tag.putInt("version", version);
        CompoundTag byType = new CompoundTag();
        killsByType.forEach(byType::putInt);
        tag.put("killsByType", byType);
        return tag;
    }

    // --- Accessors ---------------------------------------------------------

    public long getTotalHostileKills() {
        return totalHostileKills;
    }

    public int getKillsForType(String typeKey) {
        return killsByType.getOrDefault(typeKey, 0);
    }

    public Map<String, Integer> getKillsByType() {
        return killsByType;
    }

    public int getManualGlobalTier() {
        return manualGlobalTier;
    }

    public int getVersion() {
        return version;
    }

    // --- Mutators (mark dirty) --------------------------------------------

    public void addKill(String typeKey) {
        if (totalHostileKills < Long.MAX_VALUE) {
            totalHostileKills++;
        }
        killsByType.compute(typeKey, (key, current) ->
                current == null ? 1 : current < Integer.MAX_VALUE ? current + 1 : Integer.MAX_VALUE);
        setDirty();
    }

    public void setManualGlobalTier(int tier) {
        this.manualGlobalTier = tier;
        setDirty();
    }

    public void clearManualGlobalTier() {
        this.manualGlobalTier = -1;
        setDirty();
    }

    public void resetProgression() {
        totalHostileKills = 0;
        killsByType.clear();
        manualGlobalTier = -1;
        version = CURRENT_VERSION;
        setDirty();
    }
}
