package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualEntity;

/**
 * Called when a viewer right-clicks a virtual entity, once for each hand the client sends, after Basalt checked the
 * reach and the game mode. A virtual entity has no behaviour of its own: this event is where a plugin gives it one.
 * The client also sends a click at a point of the entity, which calls no event. Cancelled, the click does nothing.
 */
@ApiStatus.Experimental
public final class VirtualEntityUseEvent extends VirtualWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VirtualEntity entity;
    private final EquipmentSlot hand;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param player the viewer who clicks
     * @param entity the clicked entity
     * @param hand the hand of the click, {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    @ApiStatus.Internal
    public VirtualEntityUseEvent(@NotNull Player player, @NotNull VirtualEntity entity, @NotNull EquipmentSlot hand) {
        super(entity.world());
        this.player = player;
        this.entity = entity;
        this.hand = hand;
    }

    /**
     * Returns the viewer who clicks.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    /**
     * Returns the clicked entity.
     *
     * @return the entity
     */
    public @NotNull VirtualEntity entity() {
        return entity;
    }

    /**
     * Returns the hand of the click.
     *
     * @return {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    public @NotNull EquipmentSlot hand() {
        return hand;
    }

    /**
     * Returns whether the click is cancelled.
     *
     * @return true if the click does nothing
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the click or lets it go on.
     *
     * @param cancel true to stop the click here
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
