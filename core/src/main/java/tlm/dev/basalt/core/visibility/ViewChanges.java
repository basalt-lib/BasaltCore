package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.UUID;

public interface ViewChanges {

    void entered(UUID player, ChunkKey chunk);

    void left(UUID player, ExitCause cause);
}
