package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.UUID;

public interface ViewChanges {

    void entered(UUID player, ChunkKey chunk);

    void left(UUID player, ExitCause cause);

    static ViewChanges both(ViewChanges first, ViewChanges second) {
        return new ViewChanges() {
            @Override
            public void entered(UUID player, ChunkKey chunk) {
                first.entered(player, chunk);
                second.entered(player, chunk);
            }

            @Override
            public void left(UUID player, ExitCause cause) {
                first.left(player, cause);
                second.left(player, cause);
            }
        };
    }
}
