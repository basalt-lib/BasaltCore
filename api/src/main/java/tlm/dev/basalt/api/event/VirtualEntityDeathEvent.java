package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tlm.dev.basalt.api.VirtualEntity;

/**
 * Called when a virtual entity falls to 0 health, by a hit or by {@link VirtualEntity#setHealth(double)}, before the
 * viewers see its death. The entity is dying: it can still be moved, renamed or removed, its health cannot change,
 * and it is removed 20 ticks later. Nothing drops by default.
 */
@ApiStatus.Experimental
public final class VirtualEntityDeathEvent extends VirtualWorldEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final VirtualEntity entity;
    private final Player killer;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param entity the dying entity
     * @param killer the viewer whose hit killed it, or null
     */
    @ApiStatus.Internal
    public VirtualEntityDeathEvent(@NotNull VirtualEntity entity, @Nullable Player killer) {
        super(entity.world());
        this.entity = entity;
        this.killer = killer;
    }

    /**
     * Returns the dying entity.
     *
     * @return the entity
     */
    public @NotNull VirtualEntity entity() {
        return entity;
    }

    /**
     * Returns the viewer whose hit killed the entity.
     *
     * @return the player, or null when the health was set to 0 or the player has left
     */
    public @Nullable Player killer() {
        return killer;
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
