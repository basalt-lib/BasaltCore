package tlm.dev.basalt.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An entity made of packets in a virtual world, seen by every viewer of the world and unknown to the server.
 * It has no behaviour: no AI, no gravity, no movement of its own and no equipment. A viewer can hit it and use it.
 * A hit takes the attack damage of the player, at least 1. At 0 health it dies: the viewers see its death,
 * and it is removed 20 ticks later.
 * <p>
 * Every method runs on the main server thread only and throws {@link IllegalStateException} on another thread.
 * Get an entity from {@link VirtualWorld#spawnEntity(EntityType, Location)}, never implement this interface.
 * Two instances for the same entity are equal.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface VirtualEntity {

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
     * @return the type given to {@link VirtualWorld#spawnEntity(EntityType, Location)}
     */
    @NotNull EntityType type();

    /**
     * Returns the virtual world this entity belongs to.
     *
     * @return the world, which may be closed
     */
    @NotNull VirtualWorld world();

    /**
     * Returns where this entity stands.
     *
     * @return a new location in the real world of the area of its virtual world
     * @throws IllegalStateException if the entity is removed
     */
    @NotNull Location location();

    /**
     * Moves this entity at once, with the yaw and pitch of the location, and shows it to every viewer.
     * A dying entity can still be moved.
     *
     * @param location the new location, in the area of the virtual world
     * @throws IllegalArgumentException if the location is in another real world or outside the area
     * @throws IllegalStateException if the entity is removed
     */
    void teleport(@NotNull Location location);

    /**
     * Returns the name shown above this entity.
     *
     * @return the name, or null when the entity has none
     * @throws IllegalStateException if the entity is removed
     */
    @Nullable Component customName();

    /**
     * Sets the name shown above this entity.
     *
     * @param name the new name, or null to show none
     * @throws IllegalArgumentException if the name is longer than 4096 characters once serialized to JSON
     * @throws IllegalStateException if the entity is removed
     */
    void setCustomName(@Nullable Component name);

    /**
     * Returns the health of this entity.
     *
     * @return the health, from 0 to {@link #maxHealth()}, 0 while the entity is dying
     * @throws IllegalStateException if the entity is removed
     */
    double health();

    /**
     * Sets the health of this entity. A health of 0 kills it, without killer.
     *
     * @param health the new health, from 0 to {@link #maxHealth()}
     * @throws IllegalArgumentException if the health is not in that range
     * @throws IllegalStateException if the entity is removed or dying
     */
    void setHealth(double health);

    /**
     * Returns the maximum health of this entity.
     *
     * @return the maximum health, 20 until changed
     * @throws IllegalStateException if the entity is removed
     */
    double maxHealth();

    /**
     * Sets the maximum health of this entity. A health above it is lowered to it.
     *
     * @param maxHealth the new maximum health, above 0 and up to 1024
     * @throws IllegalArgumentException if the maximum health is not in that range
     * @throws IllegalStateException if the entity is removed or dying
     */
    void setMaxHealth(double maxHealth);

    /**
     * Returns whether this entity is silent.
     *
     * @return true if the viewers hear no hurt and no death sound from it
     * @throws IllegalStateException if the entity is removed
     */
    boolean isSilent();

    /**
     * Sets whether this entity is silent.
     *
     * @param silent true to play no hurt and no death sound
     * @throws IllegalStateException if the entity is removed
     */
    void setSilent(boolean silent);

    /**
     * Removes this entity from its world and from every viewer at once. Does nothing if it is already removed.
     */
    void remove();

    /**
     * Returns whether this entity is removed, by {@link #remove()}, 20 ticks after its death, or with its world.
     *
     * @return true once removed
     */
    boolean isRemoved();
}
