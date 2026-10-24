package tlm.dev.basalt.core.visibility;

import java.util.Optional;
import java.util.UUID;

public record PlayerGroup(Optional<UUID> world, boolean openToEveryone) {

    private static final PlayerGroup OUTSIDE = new PlayerGroup(Optional.empty(), true);

    public PlayerGroup {
        if (world == null) {
            throw new IllegalArgumentException("a player group names its world or none");
        }
        if (world.isEmpty() && !openToEveryone) {
            throw new IllegalArgumentException("the players outside every virtual world are open to everyone");
        }
    }

    public static PlayerGroup outside() {
        return OUTSIDE;
    }

    public static PlayerGroup of(UUID world, boolean openToEveryone) {
        return new PlayerGroup(Optional.of(world), openToEveryone);
    }

    public boolean sees(PlayerGroup other) {
        boolean sameWorld = world.isPresent() && world.equals(other.world);
        return sameWorld || (openToEveryone && other.openToEveryone);
    }
}
