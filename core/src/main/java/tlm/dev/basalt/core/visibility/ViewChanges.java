package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.UUID;

public interface ViewChanges {

    default void entered(UUID player, UUID world, ChunkRange area) {
    }

    default void left(UUID player, UUID world, ExitCause cause) {
    }

    default void sent(UUID player, UUID world, ChunkKey chunk) {
    }

    static ViewChanges both(ViewChanges first, ViewChanges second) {
        return new ViewChanges() {
            @Override
            public void entered(UUID player, UUID world, ChunkRange area) {
                first.entered(player, world, area);
                second.entered(player, world, area);
            }

            @Override
            public void left(UUID player, UUID world, ExitCause cause) {
                first.left(player, world, cause);
                second.left(player, world, cause);
            }

            @Override
            public void sent(UUID player, UUID world, ChunkKey chunk) {
                first.sent(player, world, chunk);
                second.sent(player, world, chunk);
            }
        };
    }
}
