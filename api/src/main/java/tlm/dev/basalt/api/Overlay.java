package tlm.dev.basalt.api;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sparse virtual blocks and entities laid over an area of a real world, seen by the viewers of the overlay only.
 * <p>
 * The real world stays: its chunks, blocks, light, biomes and real entities reach the viewers as usual, and the sounds
 * and particles of the server are not filtered. An overlay block replaces the real block at its position for the
 * viewers, also when the server sends that chunk or that block again. A viewer's click or break on an overlay block,
 * or click on an overlay entity, reaches Basalt instead of the server: see
 * {@link tlm.dev.basalt.api.event.OverlayBlockUseEvent}, {@link tlm.dev.basalt.api.event.OverlayBlockBreakEvent} and
 * {@link tlm.dev.basalt.api.event.OverlayEntityUseEvent}. The real world never changes.
 * <p>
 * Every viewer of an overlay sees the same blocks and entities, so one overlay per player gives each player their own,
 * and overlays on the same area never see each other. A player views one overlay or one virtual world at a time.
 * Player visibility is not touched: hide players with the Paper API if needed.
 * <p>
 * Every method runs on the main server thread only and throws {@link IllegalStateException} on another thread.
 * Get an overlay from {@link Basalt#createOverlay(Plugin, ChunkArea)}, never implement this interface.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Overlay {

    /**
     * Returns the unique id of this overlay.
     *
     * @return the id, stable for the life of the overlay
     */
    @NotNull UUID id();

    /**
     * Returns the area of the real world this overlay stands on.
     *
     * @return the area given to {@link Basalt#createOverlay(Plugin, ChunkArea)}
     */
    @NotNull ChunkArea area();

    /**
     * Returns the plugin this overlay belongs to.
     *
     * @return the owner given to {@link Basalt#createOverlay(Plugin, ChunkArea)}
     */
    @NotNull Plugin owner();

    /**
     * Returns whether this overlay is closed.
     *
     * @return true once {@link #close()} ran, the owner was disabled or BasaltCore stopped
     */
    boolean isClosed();

    /**
     * Returns the overlay block at a position.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @return a copy of the block data, or null where the overlay holds no block and the real block shows
     * @throws IllegalArgumentException if the position is outside the world
     * @throws IllegalStateException if the overlay is closed
     */
    @Nullable BlockData getBlock(int x, int y, int z);

    /**
     * Puts an overlay block at a position, shown at once to the viewers whose client holds the chunk and to the others
     * with the chunk. Any block works, air included: air hides the real block. The light stays the light of the real world.
     *
     * @param x the block x, inside the area
     * @param y the block y, inside the height of the real world
     * @param z the block z, inside the area
     * @param data the block
     * @throws IllegalArgumentException if the position is outside the area or the height of the real world
     * @throws IllegalStateException if the overlay is closed or already holds {@code overlay.max-blocks} blocks
     */
    void setBlock(int x, int y, int z, @NotNull BlockData data);

    /**
     * Removes the overlay block at a position: the viewers see the real block again. Nothing if the overlay holds no
     * block there.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @throws IllegalArgumentException if the position is outside the area or the height of the real world
     * @throws IllegalStateException if the overlay is closed
     */
    void removeBlock(int x, int y, int z);

    /**
     * Returns the number of overlay blocks.
     *
     * @return the blocks set and not removed
     * @throws IllegalStateException if the overlay is closed
     */
    int blockCount();

    /**
     * Plays a block action on the overlay block at a position for the viewers whose client holds the chunk, like the
     * block event packet of the server. For a chest, a trapped chest, an ender chest or a shulker box, action 1 with
     * parameter 1 opens the lid and parameter 0 closes it. Not kept: a chunk sent again shows the block at rest.
     *
     * @param x the block x
     * @param y the block y
     * @param z the block z
     * @param action the action, 0 to 255
     * @param parameter the parameter of the action, 0 to 255
     * @throws IllegalArgumentException if no overlay block stands there or if a value is outside 0 to 255
     * @throws IllegalStateException if the overlay is closed
     */
    void playBlockAction(int x, int y, int z, int action, int parameter);

    /**
     * Spawns an entity made of packets, shown at once to the viewers whose client holds its chunk and to the others
     * with the chunk. It has no gravity, no AI and no sound. An item ({@link EntityType#ITEM}) or an item display
     * ({@link EntityType#ITEM_DISPLAY}) shows the stack given to {@link OverlayEntity#setItem(org.bukkit.inventory.ItemStack)}.
     *
     * @param type the type, one the server can summon
     * @param location where the entity stands, inside the area and the height of the real world
     * @return the new entity
     * @throws IllegalArgumentException if the server cannot summon the type, if the location is in another world,
     *                                  outside the area or the height, or not finite
     * @throws IllegalStateException if the overlay is closed or already holds {@code overlay.max-entities} entities
     */
    @NotNull OverlayEntity spawnEntity(@NotNull EntityType type, @NotNull Location location);

    /**
     * Returns the entities of this overlay.
     *
     * @return a copy of the entities, in spawn order, empty once closed
     */
    @NotNull @Unmodifiable List<OverlayEntity> entities();

    /**
     * Adds a viewer: the overlay blocks and entities of the chunks its client holds are sent at once, the others with
     * their chunk. Adding the viewers after the blocks and entities sends everything in one go. A viewer who quits or
     * changes real world leaves the overlay, which stays open.
     *
     * @param player an online player in the real world of the area
     * @throws IllegalStateException if the overlay is closed, if the player is offline, in another real world,
     *                               or already views an overlay or a virtual world
     */
    void addViewer(@NotNull Player player);

    /**
     * Removes a viewer: the player sees the real blocks again and the overlay entities go. Nothing if the player is not
     * a viewer.
     *
     * @param player the player
     */
    void removeViewer(@NotNull Player player);

    /**
     * Returns whether a player views this overlay.
     *
     * @param player the player
     * @return true if the player is a viewer
     */
    boolean hasViewer(@NotNull Player player);

    /**
     * Returns the viewers of this overlay.
     *
     * @return a copy of the online viewers, empty once closed
     */
    @NotNull @Unmodifiable Set<Player> viewers();

    /**
     * Closes this overlay: every viewer sees the real world again and every later change throws. Nothing if already
     * closed. Basalt also closes the overlays of a plugin when it is disabled, and every overlay when BasaltCore stops.
     */
    void close();
}
