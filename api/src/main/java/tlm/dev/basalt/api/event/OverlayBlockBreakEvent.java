package tlm.dev.basalt.api.event;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.Overlay;

/**
 * Called when a viewer breaks a block of an overlay, after Basalt checked the break (reach, game mode, breaking time)
 * and before the block leaves the overlay. Not cancelled, the overlay block is removed and the viewers see the real
 * block again. Cancelled, the block stays and the client gets it back. The real world never changes and nothing drops.
 */
@ApiStatus.Experimental
public final class OverlayBlockBreakEvent extends OverlayEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final BlockData block;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param overlay the overlay
     * @param player the viewer who breaks the block
     * @param location the position of the block
     * @param block the overlay block being broken
     */
    @ApiStatus.Internal
    public OverlayBlockBreakEvent(@NotNull Overlay overlay, @NotNull Player player, @NotNull Location location, @NotNull BlockData block) {
        super(overlay);
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
     * @return a new location of the block, in the real world of the overlay
     */
    public @NotNull Location location() {
        return location.clone();
    }

    /**
     * Returns the overlay block being broken.
     *
     * @return a copy of the block data
     */
    public @NotNull BlockData block() {
        return block.clone();
    }

    /**
     * Returns whether the break is cancelled.
     *
     * @return true if the overlay block stays
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the break or lets it happen.
     *
     * @param cancel true to keep the overlay block
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
