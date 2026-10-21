package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.UUID;

public interface ChunkViews {

    void track(UUID player);

    ShowResult show(UUID player, ChunkKey chunk, DebugPattern pattern);

    HideResult hide(UUID player, ExitCause cause);

    int hideAll(ExitCause cause);

    Optional<ChunkKey> virtualChunk(UUID player);
}
