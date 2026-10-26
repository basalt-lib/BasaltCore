package tlm.dev.basalt.api.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import tlm.dev.basalt.api.VirtualWorld;

/**
 * An event about a virtual world. Every Basalt event is called on the main server thread.
 * <p>
 * Every plugin that listens to an event gets it, whoever owns the world: a handler that should only act on its own
 * worlds compares {@code world().owner()} with its plugin first.
 */
@ApiStatus.Experimental
public abstract class VirtualWorldEvent extends Event {

    private final VirtualWorld world;

    /**
     * Creates the event. Only Basalt calls it.
     *
     * @param world the virtual world the event is about
     */
    @ApiStatus.Internal
    protected VirtualWorldEvent(@NotNull VirtualWorld world) {
        this.world = world;
    }

    /**
     * Returns the virtual world the event is about.
     *
     * @return the world
     */
    public @NotNull VirtualWorld world() {
        return world;
    }
}
