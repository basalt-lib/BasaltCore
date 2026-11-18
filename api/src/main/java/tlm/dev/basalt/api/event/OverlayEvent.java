package tlm.dev.basalt.api.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.Overlay;

/**
 * An event about an overlay. Every Basalt event is called on the main server thread.
 * <p>
 * Every plugin that listens to an event gets it, whoever owns the overlay: a handler that should only act on its own
 * overlays compares {@code overlay().owner()} with its plugin first.
 */
@ApiStatus.Experimental
public abstract class OverlayEvent extends Event {

    private final Overlay overlay;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param overlay the overlay the event is about
     */
    @ApiStatus.Internal
    protected OverlayEvent(@NotNull Overlay overlay) {
        this.overlay = overlay;
    }

    /**
     * Returns the overlay the event is about.
     *
     * @return the overlay
     */
    public @NotNull Overlay overlay() {
        return overlay;
    }
}
