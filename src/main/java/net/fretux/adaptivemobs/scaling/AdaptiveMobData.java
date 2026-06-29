package net.fretux.adaptivemobs.scaling;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Helper for the small chunk of persistent NBT we keep on each adapted mob (via Forge's
 * {@link Entity#getPersistentData()}). Storing the applied tier lets us avoid re-applying
 * attribute scaling every time the entity is loaded from disk.
 */
public final class AdaptiveMobData {

    private static final String ROOT = "AdaptiveMobs";
    private static final String KEY_SCALED = "scaled";
    private static final String KEY_TIER = "tier";

    private AdaptiveMobData() {
    }

    private static CompoundTag root(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ROOT)) {
            data.put(ROOT, new CompoundTag());
        }
        return data.getCompound(ROOT);
    }

    public static boolean isScaled(Entity entity) {
        return entity.getPersistentData().getCompound(ROOT).getBoolean(KEY_SCALED);
    }

    public static void markScaled(Entity entity, int tier) {
        CompoundTag root = root(entity);
        root.putBoolean(KEY_SCALED, true);
        root.putInt(KEY_TIER, tier);
        // re-attach in case the sub-tag was freshly created
        entity.getPersistentData().put(ROOT, root);
    }

    /** Returns the tier stored on the mob, or -1 if it was never scaled. */
    public static int getTier(Entity entity) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        return root.contains(KEY_TIER) ? root.getInt(KEY_TIER) : -1;
    }
}
