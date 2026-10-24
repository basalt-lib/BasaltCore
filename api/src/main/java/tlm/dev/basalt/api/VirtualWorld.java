package tlm.dev.basalt.api;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.UUID;

/**
 * A virtual world: blocks made of packets on a {@link ChunkArea} of a real world, shared by its viewers.
 * A viewer sees the blocks of this world instead of the real blocks of the area, every other player sees the real world.
 * Changes made through this interface or by a viewer are seen by every viewer.
 * <p>
 * Every method runs on the main server thread only and throws {@link IllegalStateException} on another thread.
 * Get a world from {@link Basalt#createWorld(ChunkArea)}, never implement this interface.
 * A world is not kept: it disappears when it is closed or when BasaltCore stops.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface VirtualWorld {

    /**
     * Returns the id of this world.
     *
     * @return a random id, unique among the worlds of this server run
     */
    @NotNull UUID id();

    /**
     * Returns the area of the real world this world stands on.
     *
     * @return the area given to {@link Basalt#createWorld(ChunkArea)}
     */
    @NotNull ChunkArea area();

    /**
     * Returns whether this world is closed.
     *
     * @return true after {@link #close()} or when BasaltCore stopped
     */
    boolean isClosed();

    /**
     * Returns the block at a position of this world. A position never set holds air.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @return the block data at that position
     * @throws IllegalArgumentException if the position is outside the area or the height of the real world
     * @throws IllegalStateException if the world is closed
     */
    @NotNull BlockData getBlock(int x, int y, int z);

    /**
     * Sets the block at a position of this world and shows it to every viewer.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @param data the new block
     * @throws IllegalArgumentException if the position is outside the area or the height of the real world
     * @throws IllegalStateException if the world is closed
     */
    void setBlock(int x, int y, int z, @NotNull BlockData data);

    /**
     * Sets every block of a box to the same block, then sends the changed chunks to every viewer again.
     * The box covers the blocks from the floor of its minimum corner to the block before the ceiling of its maximum corner,
     * and at least one block on each axis, so {@code BoundingBox.of(firstBlock, lastBlock)} fills exactly the blocks
     * between those two.
     *
     * @param box the box of blocks, in block coordinates
     * @param data the new block
     * @throws IllegalArgumentException if a block of the box is outside the area or the height of the real world
     * @throws IllegalStateException if the world is closed
     */
    void fill(@NotNull BoundingBox box, @NotNull BlockData data);

    /**
     * Shows this world to a player. From then on the player sees the blocks of this world instead of the real blocks
     * of the area, and the real entities of the area are hidden from them.
     *
     * @param player an online player standing in the real world of the area
     * @throws IllegalStateException if the world is closed, if the player is offline, in another real world,
     *                               or already views a virtual world
     */
    void addViewer(@NotNull Player player);

    /**
     * Gives the real world back to a player who views this world. Does nothing if the player does not view it.
     *
     * @param player the player
     */
    void removeViewer(@NotNull Player player);

    /**
     * Returns whether a player views this world.
     *
     * @param player the player
     * @return true if the player views this world
     */
    boolean hasViewer(@NotNull Player player);

    /**
     * Returns the players who view this world. A viewer who leaves the server or changes real world stops viewing it.
     *
     * @return a copy of the viewers, empty once the world is closed
     */
    @NotNull @Unmodifiable Set<Player> viewers();

    /**
     * Closes this world. Every viewer gets the real world back and the blocks of this world are freed.
     * Does nothing if the world is already closed.
     */
    void close();
}
