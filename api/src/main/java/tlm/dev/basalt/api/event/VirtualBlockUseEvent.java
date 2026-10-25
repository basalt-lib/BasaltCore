package tlm.dev.basalt.api.event;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * Called when a viewer right-clicks a block of a virtual world, once for each hand the client sends, after Basalt
 * checked the click and before any placement. A virtual block has no behaviour of its own: this event is where a
 * plugin gives it one. Cancelled, nothing is placed and the client gets the blocks and its inventory back.
 * Not called for spectators.
 */
@ApiStatus.Experimental
public final class VirtualBlockUseEvent extends VirtualWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final BlockData block;
    private final BlockFace face;
    private final EquipmentSlot hand;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world
     * @param player the viewer who clicks
     * @param location the position of the clicked block
     * @param block the clicked block
     * @param face the clicked face
     * @param hand the hand of the click, {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    @ApiStatus.Internal
    public VirtualBlockUseEvent(@NotNull VirtualWorld world, @NotNull Player player, @NotNull Location location,
                                @NotNull BlockData block, @NotNull BlockFace face, @NotNull EquipmentSlot hand) {
        super(world);
        this.player = player;
        this.location = location.clone();
        this.block = block.clone();
        this.face = face;
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
     * Returns the position of the clicked block.
     *
     * @return a new location of the block, in the real world of the area
     */
    public @NotNull Location location() {
        return location.clone();
    }

    /**
     * Returns the clicked block.
     *
     * @return a copy of the block data
     */
    public @NotNull BlockData block() {
        return block.clone();
    }

    /**
     * Returns the clicked face of the block.
     *
     * @return the face
     */
    public @NotNull BlockFace face() {
        return face;
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
     * @return true if nothing happens
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
