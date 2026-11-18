package tlm.dev.basalt.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An entity made of packets in an overlay, seen by every viewer of the overlay and unknown to the server.
 * It has no gravity, no AI, no movement of its own and no sound. A viewer can use it, see
 * {@link tlm.dev.basalt.api.event.OverlayEntityUseEvent}; an attack does nothing. The client cannot click an item or
 * an item display: check their distance to the players instead.
 * <p>
 * Every method runs on the main server thread only and throws {@link IllegalStateException} on another thread.
 * Get an entity from {@link Overlay#spawnEntity(EntityType, Location)}, never implement this interface.
 * Two instances for the same entity are equal.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface OverlayEntity {

    /**
     * Returns the id the clients know this entity by. It never matches the id of a real entity.
     *
     * @return the entity id
     */
    int id();

    /**
     * Returns the unique id of this entity.
     *
     * @return the unique id, stable for the life of the entity
     */
    @NotNull UUID uuid();

    /**
     * Returns the type of this entity.
     *
     * @return the type given to {@link Overlay#spawnEntity(EntityType, Location)}
     */
    @NotNull EntityType type();

    /**
     * Returns the overlay this entity belongs to.
     *
     * @return the overlay, which may be closed
     */
    @NotNull Overlay overlay();

    /**
     * Returns where this entity stands.
     *
     * @return a new location with its yaw and pitch, in the real world of the overlay
     * @throws IllegalStateException if the entity is removed
     */
    @NotNull Location location();

    /**
     * Moves this entity, shown at once to the viewers.
     *
     * @param location the new location, with its yaw and pitch, inside the area and the height of the real world
     * @throws IllegalArgumentException if the location is in another world, outside the area or the height, or not finite
     * @throws IllegalStateException if the entity is removed
     */
    void teleport(@NotNull Location location);

    /**
     * Returns the stack this item or item display shows.
     *
     * @return a copy of the stack, or null when none is set
     * @throws IllegalStateException if the entity is removed
     */
    @Nullable ItemStack item();

    /**
     * Sets the stack this item or item display shows, sent at once to the viewers. Only an item
     * ({@link EntityType#ITEM}) or an item display ({@link EntityType#ITEM_DISPLAY}) shows a stack.
     *
     * @param item the stack, copied, or null to show none
     * @throws IllegalStateException if the entity is removed or is neither an item nor an item display
     */
    void setItem(@Nullable ItemStack item);

    /**
     * Returns the name shown above this entity.
     *
     * @return the name, or null when none is set
     * @throws IllegalStateException if the entity is removed
     */
    @Nullable Component customName();

    /**
     * Sets the name shown above this entity, sent at once to the viewers.
     *
     * @param name the name, or null to show none
     * @throws IllegalStateException if the entity is removed
     */
    void setCustomName(@Nullable Component name);

    /**
     * Removes this entity from its overlay. Nothing if already removed.
     */
    void remove();

    /**
     * Returns whether this entity was removed, by {@link #remove()} or by the closing of its overlay.
     *
     * @return true once removed
     */
    boolean isRemoved();
}
