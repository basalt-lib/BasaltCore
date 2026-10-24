package tlm.dev.basalt.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Which players the viewers of a virtual world see, and who sees them.
 * Two players see each other when they view the same virtual world, or when the groups of both are {@link #EVERYONE}.
 * The players outside every virtual world form one group that is always {@link #EVERYONE}, so a server
 * without virtual worlds behaves as usual. A hidden player also leaves the player list of the other client.
 */
@ApiStatus.Experimental
public enum PlayerVisibility {

    /**
     * The viewers see only each other: they do not see the players outside or in other worlds, who do not see them.
     * This is the default.
     */
    WORLD_ONLY,

    /**
     * The viewers also see the players outside every virtual world and the viewers of other worlds set to
     * {@code EVERYONE}, and those players see them.
     */
    EVERYONE
}
