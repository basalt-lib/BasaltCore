package tlm.dev.basalt.api;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A virtual world: blocks made of packets on a {@link ChunkArea} of a real world, shared by its viewers.
 * A viewer sees the blocks of this world instead of the real blocks of the area, every other player sees the real world.
 * Changes made through this interface or by a viewer are seen by every viewer. What a viewer does to the blocks and the
 * entities of the world calls the cancellable events of {@code tlm.dev.basalt.api.event}, never the usual Bukkit
 * events, and nothing costs or drops by default.
 * <p>
 * Every method runs on the main server thread only and throws {@link IllegalStateException} on another thread.
 * Get a world from {@link Basalt#createWorld(Plugin, ChunkArea)}, never implement this interface.
 * A world is not kept: it disappears when it is closed, when its owner is disabled or when BasaltCore stops,
 * never because it has no viewer.
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
     * @return the area given to {@link Basalt#createWorld(Plugin, ChunkArea)}
     */
    @NotNull ChunkArea area();

    /**
     * Returns the plugin this world belongs to. Basalt closes the world when this plugin is disabled.
     *
     * @return the owner given to {@link Basalt#createWorld(Plugin, ChunkArea)}
     */
    @NotNull Plugin owner();

    /**
     * Returns whether this world is closed.
     *
     * @return true after {@link #close()}, the disabling of the owner or the stop of BasaltCore
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
     * Places a blueprint in this world with its minimum corner at a block position, and sends the changed chunks to the
     * viewers within the per tick budget of the BasaltCore config. It replaces the whole volume, air included, except its
     * voids, which keep what the world holds there. Blocks that have block entities in the real game are placed as block
     * states only. No rotation and no mirror.
     *
     * @param blueprint a blueprint from {@link Basalt}
     * @param x the x of the minimum corner
     * @param y the y of the minimum corner
     * @param z the z of the minimum corner
     * @throws IllegalArgumentException if the blueprint does not fit in the area and the height of the real world, or
     *                                  does not come from {@link Basalt}
     * @throws IllegalStateException if the world is closed
     */
    void place(@NotNull Blueprint blueprint, int x, int y, int z);

    /**
     * Creates an entity in this world. It appears to every viewer at the next tick, with the state it has then,
     * so it can be set up first without extra packets. It starts with 20 health out of 20, no name, and is not silent.
     *
     * @param type a type the server can summon, never {@link EntityType#PLAYER}
     * @param location where it stands, with its yaw and pitch, in the area of this world
     * @return the new entity
     * @throws IllegalArgumentException if the type cannot be summoned, or the location is in another real world or
     *                                  outside the area
     * @throws IllegalStateException if the world is closed or already holds the most entities allowed
     *                               ({@code entity.max-per-world} of the BasaltCore config)
     */
    @NotNull VirtualEntity spawnEntity(@NotNull EntityType type, @NotNull Location location);

    /**
     * Returns the entities of this world, dying ones included until they are removed.
     *
     * @return a copy of the entities, in the order they were created, empty once the world is closed
     */
    @NotNull @Unmodifiable List<VirtualEntity> entities();

    /**
     * Returns which players the viewers of this world see.
     *
     * @return the visibility of the players, {@link PlayerVisibility#WORLD_ONLY} until changed
     * @throws IllegalStateException if the world is closed
     */
    @NotNull PlayerVisibility playerVisibility();

    /**
     * Sets which players the viewers of this world see, and applies it at once to every player concerned.
     *
     * @param visibility the new visibility of the players
     * @throws IllegalStateException if the world is closed
     */
    void setPlayerVisibility(@NotNull PlayerVisibility visibility);

    /**
     * Closes this world. Every viewer gets the real world back and the blocks of this world are freed.
     * Does nothing if the world is already closed.
     * <p>
     * The viewers leave one after the other, each with a {@link tlm.dev.basalt.api.event.VirtualViewerRemoveEvent} of
     * reason {@code WORLD_CLOSED}. While the world closes, for instance inside one of those handlers, calling this method
     * again does nothing, {@link #addViewer(Player)} throws {@link IllegalStateException} and {@link #isClosed()} still
     * returns false; the world is closed once the last viewer has left.
     */
    void close();
}
