package tlm.dev.basalt.core.position;

import java.util.ArrayList;
import java.util.List;

public record ChunkRange(int minX, int minZ, int maxX, int maxZ) {

    public ChunkRange {
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("chunk range " + minX + " " + minZ + " to " + maxX + " " + maxZ + " is upside down");
        }
        new ChunkKey(minX, minZ);
        new ChunkKey(maxX, maxZ);
    }

    public static ChunkRange of(ChunkKey chunk) {
        return new ChunkRange(chunk.x(), chunk.z(), chunk.x(), chunk.z());
    }

    public static ChunkRange between(ChunkKey first, ChunkKey second) {
        return new ChunkRange(Math.min(first.x(), second.x()), Math.min(first.z(), second.z()),
                Math.max(first.x(), second.x()), Math.max(first.z(), second.z()));
    }

    public boolean contains(ChunkKey chunk) {
        return chunk.x() >= minX && chunk.x() <= maxX && chunk.z() >= minZ && chunk.z() <= maxZ;
    }

    public boolean containsBlock(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public boolean overlaps(int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ) {
        return otherMinX <= maxX && otherMaxX >= minX && otherMinZ <= maxZ && otherMaxZ >= minZ;
    }

    public long chunkCount() {
        return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
    }

    public List<ChunkKey> chunks() {
        if (chunkCount() > Integer.MAX_VALUE - 8) {
            throw new IllegalStateException("too many chunks to list: " + chunkCount());
        }
        List<ChunkKey> chunks = new ArrayList<>((int) chunkCount());
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                chunks.add(new ChunkKey(x, z));
            }
        }
        return List.copyOf(chunks);
    }
}
