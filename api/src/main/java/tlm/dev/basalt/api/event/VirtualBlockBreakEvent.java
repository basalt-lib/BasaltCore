package tlm.dev.basalt.api.event;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * Called when a viewer breaks a block of a virtual world, after Basalt checked the break and before the block turns
 * to air. A handler may change the world: the break then applies to the block found at that position afterwards.
 * Cancelled, the block stays and the client gets it back. Nothing drops by default.
 */
@ApiStatus.Experimental
public final class VirtualBlockBreakEvent extends VirtualWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final BlockData block;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world
     * @param player the viewer who breaks the block
     * @param location the position of the block
     * @param block the block being broken
     */
    @ApiStatus.Internal
    public VirtualBlockBreakEvent(@NotNull VirtualWorld world, @NotNull Player player, @NotNull Location location, @NotNull BlockData block) {
        super(world);
        this.player = player;
        this.location = location.clone();
        this.block = block.clone();
    }

    /**
     * Returns the viewer who breaks the block.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    /**
     * Returns the position of the block.
     *
     * @return a new location of the block, in the real world of the area
     */
    public @NotNull Location location() {
        return location.clone();
    }

    /**
     * Returns the block being broken.
     *
     * @return a copy of the block data
     */
    public @NotNull BlockData block() {
        return block.clone();
    }

    /**
     * Returns whether the break is cancelled.
     *
     * @return true if the block stays
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the break or lets it happen.
     *
     * @param cancel true to keep the block
     */
    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    /**
     * Returns the handlers of this event.
     *
     * @return the handlers
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the handlers of this event, as Bukkit requires.
     *
     * @return the handlers
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
