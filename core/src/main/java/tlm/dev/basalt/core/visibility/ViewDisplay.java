package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.UUID;

public interface ViewDisplay<T> {

    void attach(UUID player, PlayerView<T> view);

    void detach(UUID player);

    boolean hasChunk(UUID player, ChunkKey chunk);

    T buildVirtual(UUID player, ChunkKey chunk, DebugPattern pattern);

    void sendVirtual(UUID player, T payload);

    void resendReal(UUID player, ChunkKey chunk);

    void restartFlyingCheck(UUID player);
}
