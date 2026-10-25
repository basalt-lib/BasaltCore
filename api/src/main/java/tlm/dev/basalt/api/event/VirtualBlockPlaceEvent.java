package tlm.dev.basalt.api.event;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * Called when a viewer places a block in a virtual world, after Basalt checked the placement and before the block is
 * set. A handler may change the world: the placement is checked again afterwards and refused if the target is no
 * longer free. Cancelled, nothing is placed and the client gets the blocks and its inventory back. By default the held
 * item is not consumed.
 */
@ApiStatus.Experimental
public final class VirtualBlockPlaceEvent extends VirtualWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final BlockData block;
    private final EquipmentSlot hand;
    private boolean consumeItem;
    private boolean cancelled;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world
     * @param player the viewer who places the block
     * @param location the position where the block goes
     * @param block the block being placed
     * @param hand the hand holding the item, {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    @ApiStatus.Internal
    public VirtualBlockPlaceEvent(@NotNull VirtualWorld world, @NotNull Player player, @NotNull Location location,
                                  @NotNull BlockData block, @NotNull EquipmentSlot hand) {
        super(world);
        this.player = player;
        this.location = location.clone();
        this.block = block.clone();
        this.hand = hand;
    }

    /**
     * Returns the viewer who places the block.
     *
     * @return the player
     */
    public @NotNull Player player() {
        return player;
    }

    /**
     * Returns the position where the block goes.
     *
     * @return a new location of the block, in the real world of the area
     */
    public @NotNull Location location() {
        return location.clone();
    }

    /**
     * Returns the block being placed.
     *
     * @return a copy of the block data
     */
    public @NotNull BlockData block() {
        return block.clone();
    }

    /**
     * Returns the hand holding the item.
     *
     * @return {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    public @NotNull EquipmentSlot hand() {
        return hand;
    }

    /**
     * Returns whether one held item is taken from the real inventory of the player when the block is placed.
     * Never in creative mode.
     *
     * @return true to consume the item, false by default
     */
    public boolean consumeItem() {
        return consumeItem;
    }

    /**
     * Sets whether one held item is taken from the real inventory of the player when the block is placed.
     *
     * @param consume true to consume the item
     */
    public void setConsumeItem(boolean consume) {
        consumeItem = consume;
    }

    /**
     * Returns whether the placement is cancelled.
     *
     * @return true if nothing is placed
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the placement or lets it happen.
     *
     * @param cancel true to place nothing
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
