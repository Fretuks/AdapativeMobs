package net.fretux.adaptivemobs.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Crash-safe registry for temporary blocks. Cobwebs only replace air, so restoration is unambiguous. */
public final class TemporaryBlockSavedData extends SavedData {
    private static final String DATA_NAME = "adaptivemobs_temporary_blocks";
    private final List<Entry> entries = new ArrayList<>();

    public static TemporaryBlockSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TemporaryBlockSavedData::load,
                TemporaryBlockSavedData::new, DATA_NAME);
    }

    public void add(ServerLevel level, BlockPos pos, long expireTick) {
        entries.add(new Entry(level.dimension().location(), pos.immutable(), expireTick));
        setDirty();
    }

    public void remove(ServerLevel level, BlockPos pos) {
        if (entries.removeIf(entry -> entry.dimension.equals(level.dimension().location()) && entry.pos.equals(pos))) {
            setDirty();
        }
    }

    public Iterator<Entry> entries() {
        return entries.iterator();
    }

    public void changed() {
        setDirty();
    }

    public static TemporaryBlockSavedData load(CompoundTag tag) {
        TemporaryBlockSavedData data = new TemporaryBlockSavedData();
        ListTag list = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension != null) {
                data.entries.add(new Entry(dimension, BlockPos.of(entry.getLong("Pos")), entry.getLong("Expires")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag encoded = new CompoundTag();
            encoded.putString("Dimension", entry.dimension.toString());
            encoded.putLong("Pos", entry.pos.asLong());
            encoded.putLong("Expires", entry.expireTick);
            list.add(encoded);
        }
        tag.put("Blocks", list);
        return tag;
    }

    public record Entry(ResourceLocation dimension, BlockPos pos, long expireTick) {
    }
}
