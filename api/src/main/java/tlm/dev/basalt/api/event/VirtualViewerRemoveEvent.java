package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * Called when a player stops viewing a virtual world, once the player has the real world back. A handler may add the
 * player to another world, except when the player is leaving the server or the world is closing.
 */
@ApiStatus.Experimental
public final class VirtualViewerRemoveEvent extends VirtualWorldEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Reason reason;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world
     * @param player the former viewer
     * @param reason why the player stopped viewing the world
     */
    @ApiStatus.Internal
    public VirtualViewerRemoveEvent(@NotNull VirtualWorld world, @NotNull Player player, @NotNull Reason reason) {
        super(world);
        this.player = player;
        this.reason = reason;
    }

    /**
     * Returns the former viewer.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    /**
     * Returns why the player stopped viewing the world.
     *
     * @return the reason
     */
    public @NotNull Reason reason() {
        return reason;
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

    /**
     * Why a player stopped viewing a virtual world.
     */
    public enum Reason {

        /**
         * {@link VirtualWorld#removeViewer(Player)} was called.
         */
        REQUESTED,

        /**
         * The player left the server.
         */
        DISCONNECT,

        /**
         * The player went to another real world.
         */
        REAL_WORLD_CHANGE,

        /**
         * The world was closed, by {@link VirtualWorld#close()}, the disabling of its owner or the stop of BasaltCore.
         */
        WORLD_CLOSED
    }
}
