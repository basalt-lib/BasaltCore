package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.OverlayEntity;

/**
 * Called when a viewer right clicks an entity of an overlay, after Basalt checked the reach and the rate of clicks.
 * The click never reaches the server and the usual Bukkit events do not fire. Not cancellable, nothing else happens.
 */
@ApiStatus.Experimental
public final class OverlayEntityUseEvent extends OverlayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final OverlayEntity entity;
    private final EquipmentSlot hand;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param player the viewer who clicks
     * @param entity the overlay entity clicked
     * @param hand the hand used
     */
    @ApiStatus.Internal
    public OverlayEntityUseEvent(@NotNull Player player, @NotNull OverlayEntity entity, @NotNull EquipmentSlot hand) {
        super(entity.overlay());
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
     * Returns the overlay entity clicked.
     *
     * @return the entity
     */
    public @NotNull OverlayEntity entity() {
        return entity;
    }

    /**
     * Returns the hand used.
     *
     * @return {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    public @NotNull EquipmentSlot hand() {
        return hand;
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
