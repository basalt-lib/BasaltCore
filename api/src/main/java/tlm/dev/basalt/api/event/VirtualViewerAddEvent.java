package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * Called when a player starts viewing a virtual world, once the player is a viewer. The chunks of the world reach the
 * client in the next ticks.
 */
@ApiStatus.Experimental
public final class VirtualViewerAddEvent extends VirtualWorldEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world
     * @param player the new viewer
     */
    @ApiStatus.Internal
    public VirtualViewerAddEvent(@NotNull VirtualWorld world, @NotNull Player player) {
        super(world);
        this.player = player;
    }

    /**
     * Returns the new viewer.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
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
