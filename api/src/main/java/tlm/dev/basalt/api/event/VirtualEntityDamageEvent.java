package tlm.dev.basalt.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualEntity;

/**
 * Called when a viewer hits a virtual entity, after Basalt checked the hit (reach, game mode, delay between two hits)
 * and before the health goes down. The damage starts at the attack damage of the player, at least 1.
 * A handler may change the world: the hit then applies to the entity as it is afterwards, and does nothing if the
 * entity is gone. Cancelled, the entity keeps its health and the viewers see nothing.
 */
@ApiStatus.Experimental
public final class VirtualEntityDamageEvent extends VirtualWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VirtualEntity entity;
    private double damage;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param player the viewer who hits
     * @param entity the entity being hit
     * @param damage the damage of the hit
     */
    @ApiStatus.Internal
    public VirtualEntityDamageEvent(@NotNull Player player, @NotNull VirtualEntity entity, double damage) {
        super(entity.world());
        this.player = player;
        this.entity = entity;
        this.damage = damage;
    }

    /**
     * Returns the viewer who hits.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    /**
     * Returns the entity being hit.
     *
     * @return the entity
     */
    public @NotNull VirtualEntity entity() {
        return entity;
    }

    /**
     * Returns the damage of the hit.
     *
     * @return the health the entity loses
     */
    public double damage() {
        return damage;
    }

    /**
     * Sets the damage of the hit. A damage of 0 shows the hit without taking health.
     *
     * @param damage the health the entity loses, 0 or more
     * @throws IllegalArgumentException if the damage is negative or not finite
     */
    public void setDamage(double damage) {
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("a damage is finite and 0 or more: " + damage);
        }
        this.damage = damage;
    }

    /**
     * Returns whether the hit is cancelled.
     *
     * @return true if the entity keeps its health
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the hit or lets it happen.
     *
     * @param cancel true to keep the health of the entity
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
