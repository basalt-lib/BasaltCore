package tlm.dev.basalt.api;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A rectangle of chunks of a real world, from the minimum chunk to the maximum chunk, both included.
 * A virtual world stands on such an area, at the same coordinates, over the whole height of the real world.
 *
 * @param world the real world
 * @param minChunkX the smallest chunk x of the area
 * @param minChunkZ the smallest chunk z of the area
 * @param maxChunkX the largest chunk x of the area
 * @param maxChunkZ the largest chunk z of the area
 */
@ApiStatus.Experimental
public record ChunkArea(@NotNull World world, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {

    /**
     * The largest chunk coordinate of a Minecraft world, 30 million blocks from the center.
     */
    public static final int MAX_CHUNK_COORDINATE = 1_875_000;

    /**
     * Checks the area.
     *
     * @throws NullPointerException if the world is null
     * @throws IllegalArgumentException if a minimum is above its maximum or a chunk is beyond {@link #MAX_CHUNK_COORDINATE}
     */
    public ChunkArea {
        Objects.requireNonNull(world, "world");
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            throw new IllegalArgumentException("the minimum chunk " + minChunkX + " " + minChunkZ
                    + " is above the maximum chunk " + maxChunkX + " " + maxChunkZ);
        }
        if (beyondLimit(minChunkX) || beyondLimit(minChunkZ) || beyondLimit(maxChunkX) || beyondLimit(maxChunkZ)) {
            throw new IllegalArgumentException("chunk coordinates go from -" + MAX_CHUNK_COORDINATE + " to " + MAX_CHUNK_COORDINATE);
        }
    }

    /**
     * Returns the area of the chunks under a box of blocks. The box covers the blocks from the floor of its minimum corner
     * to the block before the ceiling of its maximum corner, and at least one block on each axis, so
     * {@code BoundingBox.of(firstBlock, lastBlock)} covers exactly the blocks between those two.
     *
     * @param world the real world
     * @param box the box of blocks, in block coordinates
     * @return the smallest area that holds every block of the box
     * @throws NullPointerException if the world or the box is null
     * @throws IllegalArgumentException if the box has a coordinate that is not finite or reaches beyond the world limits
     */
    public static @NotNull ChunkArea ofBlocks(@NotNull World world, @NotNull BoundingBox box) {
        Objects.requireNonNull(box, "box");
        int minX = firstBlock(box.getMinX());
        int minZ = firstBlock(box.getMinZ());
        return new ChunkArea(world, minX >> 4, minZ >> 4,
                lastBlock(minX, box.getMaxX()) >> 4, lastBlock(minZ, box.getMaxZ()) >> 4);
    }

    /**
     * Returns the number of chunks in the area.
     *
     * @return the width times the depth of the area, in chunks
     */
    public long chunkCount() {
        return ((long) maxChunkX - minChunkX + 1) * ((long) maxChunkZ - minChunkZ + 1);
    }

    private static boolean beyondLimit(int chunk) {
        return chunk < -MAX_CHUNK_COORDINATE || chunk > MAX_CHUNK_COORDINATE;
    }

    private static int firstBlock(double min) {
        if (!Double.isFinite(min)) {
            throw new IllegalArgumentException("a box corner is not finite: " + min);
        }
        return (int) Math.floor(min);
    }

    private static int lastBlock(int first, double max) {
        if (!Double.isFinite(max)) {
            throw new IllegalArgumentException("a box corner is not finite: " + max);
        }
        return Math.max(first, (int) Math.ceil(max) - 1);
    }
}
