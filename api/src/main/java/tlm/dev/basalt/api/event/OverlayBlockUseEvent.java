package tlm.dev.basalt.api.event;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.Overlay;

/**
 * Called when a viewer right clicks a block of an overlay, after Basalt checked the reach, the game mode and the rate
 * of clicks. The click never reaches the server and the usual Bukkit events do not fire: Basalt sends the viewer the
 * overlay block, its inventory and the end of the click again, and nothing is placed. Not cancellable, nothing else
 * happens. A click on a real block whose face touches an overlay block is held back too, without this event.
 */
@ApiStatus.Experimental
public final class OverlayBlockUseEvent extends OverlayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final BlockData block;
    private final BlockFace face;
    private final EquipmentSlot hand;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param overlay the overlay
     * @param player the viewer who clicks
     * @param location the position of the block
     * @param block the overlay block
     * @param face the face clicked
     * @param hand the hand used
     */
    @ApiStatus.Internal
    public OverlayBlockUseEvent(@NotNull Overlay overlay, @NotNull Player player, @NotNull Location location, @NotNull BlockData block,
                                @NotNull BlockFace face, @NotNull EquipmentSlot hand) {
        super(overlay);
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
     * Returns the position of the block.
     *
     * @return a new location of the block, in the real world of the overlay
     */
    public @NotNull Location location() {
        return location.clone();
    }

    /**
     * Returns the overlay block clicked.
     *
     * @return a copy of the block data
     */
    public @NotNull BlockData block() {
        return block.clone();
    }

    /**
     * Returns the face clicked.
     *
     * @return the face
     */
    public @NotNull BlockFace face() {
        return face;
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
