package tlm.dev.basalt.core.world;

import java.util.UUID;

public interface WorldListener {

    default void emptied(UUID world) {
    }

    default void closed(UUID world) {
    }

    static WorldListener both(WorldListener first, WorldListener second) {
        return new WorldListener() {
            @Override
            public void emptied(UUID world) {
                first.emptied(world);
                second.emptied(world);
            }

            @Override
            public void closed(UUID world) {
                first.closed(world);
                second.closed(world);
            }
        };
    }
}
